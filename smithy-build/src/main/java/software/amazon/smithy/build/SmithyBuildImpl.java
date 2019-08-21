/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.build;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.build.model.ProjectionConfig;
import software.amazon.smithy.build.model.SmithyBuildConfig;
import software.amazon.smithy.build.model.TransformConfig;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.ModelAssembler;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.transform.ModelTransformer;
import software.amazon.smithy.model.validation.ValidatedResult;
import software.amazon.smithy.utils.SmithyBuilder;

final class SmithyBuildImpl {
    private static final Logger LOGGER = Logger.getLogger(SmithyBuild.class.getName());
    private static final String APPLY_PROJECTIONS = "apply";
    private static final Pattern PATTERN = Pattern.compile("^[A-Za-z0-9\\-_.]+$");

    private final SmithyBuildConfig config;
    private final Function<Path, FileManifest> fileManifestFactory;
    private final Supplier<ModelAssembler> modelAssemblerSupplier;
    private final Path outputDirectory;
    private final Map<String, BiFunction<ModelTransformer, Model, Model>> transformers = new HashMap<>();
    private final ModelTransformer modelTransformer;
    private final Function<String, Optional<ProjectionTransformer>> transformFactory;
    private final Function<String, Optional<SmithyBuildPlugin>> pluginFactory;
    private final Model model;
    private final Path importBasePath;
    private final ClassLoader pluginClassLoader;
    private final Set<Path> sources;
    private final Predicate<String> projectionFilter;
    private final Predicate<String> pluginFilter;

    SmithyBuildImpl(SmithyBuild builder) {
        config = prepareConfig(SmithyBuilder.requiredState("config", builder.config));
        sources = builder.sources;
        fileManifestFactory = builder.fileManifestFactory != null
                ? builder.fileManifestFactory
                : FileManifest::create;
        modelAssemblerSupplier = builder.modelAssemblerSupplier != null
                ? builder.modelAssemblerSupplier
                : Model::assembler;
        modelTransformer = builder.modelTransformer != null
                ? builder.modelTransformer
                : ModelTransformer.create();
        transformFactory = builder.transformFactory != null
                ? builder.transformFactory
                : ProjectionTransformer.createServiceFactory(getClass().getClassLoader());
        pluginFactory = builder.pluginFactory != null
                ? builder.pluginFactory
                : SmithyBuildPlugin.createServiceFactory(getClass().getClassLoader());
        model = builder.model != null
                ? builder.model
                : Model.builder().build();

        if (builder.outputDirectory != null) {
            outputDirectory = builder.outputDirectory;
        } else if (config.getOutputDirectory().isPresent()) {
            outputDirectory = Paths.get(config.getOutputDirectory().get());
        } else {
            // Default the output directory to the current working directory + "./build/smithy"
            outputDirectory = Paths.get(".").toAbsolutePath().normalize().resolve("build").resolve("smithy");
        }

        // Use the base path of the configuration or get the current working directory.
        importBasePath = builder.importBasePath != null
                ? builder.importBasePath
                : Paths.get(".").toAbsolutePath().normalize();

        // Create the transformers for each projection.
        config.getProjections().forEach((k, p) -> transformers.put(k, createTransformer(k, p, new LinkedHashSet<>())));

        pluginClassLoader = builder.pluginClassLoader;
        projectionFilter = builder.projectionFilter;
        pluginFilter = builder.pluginFilter;
    }

    private static SmithyBuildConfig prepareConfig(SmithyBuildConfig config) {
        // If we don't have a source projection specified, supply one.
        if (!config.getProjections().containsKey("source")) {
            Map<String, ProjectionConfig> projections = new HashMap<>(config.getProjections());
            projections.put("source", ProjectionConfig.builder().build());
            config = config.toBuilder().projections(projections).build();
        }

        // The `source` projection cannot include mappers or filters.
        ProjectionConfig sourceProjection = config.getProjections().get("source");
        if (!sourceProjection.getTransforms().isEmpty()) {
            throw new SmithyBuildException("The source projection cannot contain any transforms");
        }

        config.getPlugins().keySet().forEach(p -> validatePluginName("[top-level]", p));

        for (Map.Entry<String, ProjectionConfig> entry : config.getProjections().entrySet()) {
            String projectionName = entry.getKey();
            if (!PATTERN.matcher(projectionName).matches()) {
                throw new SmithyBuildException(String.format("Invalid Smithy build projection name `%s`. "
                        + "Projection names must match the following regex: %s", projectionName, PATTERN));
            }
            entry.getValue().getPlugins().keySet().forEach(p -> validatePluginName(entry.getKey(), p));
            entry.getValue().getTransforms().forEach(t -> validateTransformName(entry.getKey(), t.getName()));
        }

        return config;
    }

    private static void validateTransformName(String projection, String transformName) {
        if (!PATTERN.matcher(transformName).matches()) {
            throw new SmithyBuildException(String.format("Invalid transform name `%s` found in the `%s` projection. "
                    + " Transform names must match the following regex: %s", transformName, projection, PATTERN));
        }
    }

    private static void validatePluginName(String projection, String plugin) {
        if (!PATTERN.matcher(plugin).matches()) {
            throw new SmithyBuildException(String.format(
                    "Invalid plugin name `%s` found in the `%s` projection. "
                    + " Plugin names must match the following regex: %s", plugin, projection, PATTERN));
        }
    }

    SmithyBuildResult applyAllProjections() {
        Model resolvedModel = createBaseModel();
        SmithyBuildResult.Builder builder = SmithyBuildResult.builder();

        // The projections are being split up here because we need to be able to break out non-parallelizeable plugins.
        // Right now the only parallelization that occurs is at the projection level though, which is why the split is
        // here instead of somewhere else.
        // TODO: Run all parallelizeable plugins across all projections in parallel, followed by all serial plugins
        Map<String, ProjectionConfig> serialProjections = new TreeMap<>();
        Map<String, ProjectionConfig> parallelProjections = new TreeMap<>();
        config.getProjections().entrySet().stream()
                .filter(e -> !e.getValue().isAbstract())
                .filter(e -> projectionFilter.test(e.getKey()))
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .forEach(e -> {
                    // Check to see if any of the plugins in the projection require the projection be run serially
                    boolean isSerial = resolvePlugins(e.getValue()).keySet().stream().anyMatch(pluginName -> {
                        Optional<SmithyBuildPlugin> plugin = pluginFactory.apply(pluginName);
                        return plugin.isPresent() && plugin.get().isSerial();
                    });
                    // Only run a projection in parallel if all its plugins are parallelizeable.
                    if (isSerial) {
                        serialProjections.put(e.getKey(), e.getValue());
                    } else {
                        parallelProjections.put(e.getKey(), e.getValue());
                    }
                });

        serialProjections.entrySet().stream()
                .map(e -> applyProjection(e.getKey(), e.getValue(), resolvedModel))
                .collect(Collectors.toList())
                .forEach(builder::addProjectionResult);

        parallelProjections.entrySet().stream()
                .parallel()
                .map(e -> applyProjection(e.getKey(), e.getValue(), resolvedModel))
                .collect(Collectors.toList())
                .forEach(builder::addProjectionResult);

        return builder.build();
    }

    private Model createBaseModel() {
        Model resolvedModel = model;

        if (!config.getImports().isEmpty()) {
            LOGGER.fine(() -> "Merging the following imports into the loaded model: " + config.getImports());
            ModelAssembler assembler = modelAssemblerSupplier.get().addModel(model);
            config.getImports().forEach(path -> assembler.addImport(importBasePath.resolve(path)));
            resolvedModel = assembler.assemble().unwrap();
        }

        return resolvedModel;
    }

    private ProjectionResult applyProjection(String projectionName, ProjectionConfig projection, Model resolvedModel) {
        LOGGER.fine(() -> String.format("Creating the `%s` projection", projectionName));

        // Resolve imports.
        if (!projection.getImports().isEmpty()) {
            LOGGER.fine(() -> String.format(
                    "Merging the following `%s` projection imports into the loaded model: %s",
                    projectionName, projection.getImports()));
            ModelAssembler assembler = modelAssemblerSupplier.get().addModel(resolvedModel);
            projection.getImports().forEach(path -> assembler.addImport(importBasePath.resolve(path)));
            ValidatedResult<Model> resolvedResult = assembler.assemble();

            // Fail if the model can't be merged with the imports.
            if (!resolvedResult.getResult().isPresent()) {
                LOGGER.severe(String.format(
                        "The model could not be merged with the following imports: [%s]",
                        projection.getImports()));
                return ProjectionResult.builder()
                        .projectionName(projectionName)
                        .events(resolvedResult.getValidationEvents())
                        .build();
            }

            resolvedModel = resolvedResult.unwrap();
        }

        // Create the base directory where all projection artifacts are stored.
        Path baseProjectionDir = outputDirectory.resolve(projectionName);

        // Project the model and collect the results.
        Model projectedModel = transformers
                .get(projectionName)
                .apply(modelTransformer, resolvedModel);

        ValidatedResult<Model> modelResult = modelAssemblerSupplier.get().addModel(projectedModel).assemble();

        ProjectionResult.Builder resultBuilder = ProjectionResult.builder()
                .projectionName(projectionName)
                .model(projectedModel)
                .events(modelResult.getValidationEvents());

        for (Map.Entry<String, ObjectNode> entry : resolvePlugins(projection).entrySet()) {
            if (pluginFilter.test(entry.getKey())) {
                applyPlugin(projectionName, projection, baseProjectionDir, entry.getKey(), entry.getValue(),
                            projectedModel, resolvedModel, modelResult, resultBuilder);
            }
        }

        return resultBuilder.build();
    }

    private void applyPlugin(
            String projectionName,
            ProjectionConfig projection,
            Path baseProjectionDir,
            String pluginName,
            ObjectNode pluginSettings,
            Model projectedModel,
            Model resolvedModel,
            ValidatedResult<Model> modelResult,
            ProjectionResult.Builder resultBuilder
    ) {
        // Create the manifest where plugin artifacts are stored.
        Path pluginBaseDir = baseProjectionDir.resolve(pluginName);
        FileManifest manifest = fileManifestFactory.apply(pluginBaseDir);

        // Find the desired plugin in the SPI found plugins.
        SmithyBuildPlugin resolved = pluginFactory.apply(pluginName).orElse(null);

        if (resolved == null) {
            LOGGER.info(() -> String.format(
                    "Unable to find a plugin for `%s` in the `%s` projection",
                    pluginName, projectionName));
        } else if (resolved.requiresValidModel() && modelResult.isBroken()) {
            LOGGER.fine(() -> String.format(
                    "Skipping `%s` plugin for `%s` projection because the model is broken",
                    pluginName, projectionName));
        } else {
            LOGGER.info(() -> String.format(
                    "Applying `%s` plugin to `%s` projection",
                    pluginName, projectionName));
            resolved.execute(PluginContext.builder()
                    .model(projectedModel)
                    .originalModel(resolvedModel)
                    .projection(projectionName, projection)
                    .events(modelResult.getValidationEvents())
                    .settings(pluginSettings)
                    .fileManifest(manifest)
                    .pluginClassLoader(pluginClassLoader)
                    .sources(sources)
                    .build());
            resultBuilder.addPluginManifest(pluginName, manifest);
        }
    }

    private Map<String, ObjectNode> resolvePlugins(ProjectionConfig projection) {
        Map<String, ObjectNode> result = new TreeMap<>(config.getPlugins());
        result.putAll(projection.getPlugins());
        return result;
    }

    private BiFunction<ModelTransformer, Model, Model> createTransformer(
            String projectionName,
            ProjectionConfig projection,
            Set<String> visited
    ) {
        if (visited.contains(projectionName)) {
            visited.add(projectionName);
            throw new SmithyBuildException(String.format("Cycle found in %s transforms: %s -> ...",
                    APPLY_PROJECTIONS, String.join(" -> ", visited)));
        }

        visited.add(projectionName);

        // Create a composed transformer of each created transformer.
        return projection.getTransforms().stream()
                .flatMap(transform -> getTransform(projectionName, transform, visited))
                .reduce((a, b) -> (transformer, model) -> b.apply(transformer, a.apply(transformer, model)))
                .orElse(((transformer, model) -> model));
    }

    private Stream<BiFunction<ModelTransformer, Model, Model>> getTransform(
            String projection,
            TransformConfig config,
            Set<String> visited
    ) {
        String name = config.getName();

        if (name.equals(APPLY_PROJECTIONS)) {
            return config.getArgs().stream().map(arg -> {
                // Copy the set of visited projections to a new set;
                // visiting the same projection isn't a problem, it's
                // cycles that's problematic.
                ProjectionConfig targetProjection = findProjection(projection, arg);
                return createTransformer(arg, targetProjection, new LinkedHashSet<>(visited));
            });
        }

        ProjectionTransformer transformer = transformFactory.apply(name)
                .orElseThrow(() -> new UnknownTransformException("Unable to find a transform for `" + name + "`."));
        return Stream.of(transformer.createTransformer(config.getArgs()));
    }

    private ProjectionConfig findProjection(String projection, String name) {
        if (!config.getProjections().containsKey(name)) {
            throw new UnknownProjectionException(String.format(
                    "Unable to find projection named `%s` referenced by `%s`", name, projection));
        }

        return config.getProjections().get(name);
    }
}
