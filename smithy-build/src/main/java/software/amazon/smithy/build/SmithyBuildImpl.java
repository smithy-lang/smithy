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
import java.util.stream.Stream;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SmithyBuilder;
import software.amazon.smithy.model.ValidatedResult;
import software.amazon.smithy.model.loader.ModelAssembler;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.transform.ModelTransformer;

final class SmithyBuildImpl {
    private static final Logger LOGGER = Logger.getLogger(SmithyBuild.class.getName());
    private static final String APPLY_PROJECTIONS = "apply";

    private final SmithyBuildConfig config;
    private final Function<Path, FileManifest> fileManifestFactory;
    private final Supplier<ModelAssembler> modelAssemblerSupplier;
    private final Path outputDirectory;
    private final Map<String, BiFunction<ModelTransformer, Model, Model>> transformers = new HashMap<>();
    private final ModelTransformer modelTransformer;
    private final Function<String, Optional<ProjectionTransformer>> transformFactory;
    private final Function<String, Optional<SmithyBuildPlugin>> pluginFactory;
    private final Model model;
    private final BiFunction<String, SmithyBuildConfig, Path> importBasePathResolver;
    private final ClassLoader pluginClassLoader;
    private final ModuleLayer pluginModuleLayer;

    SmithyBuildImpl(SmithyBuild builder) {
        SmithyBuildConfig baseConfig = SmithyBuilder.requiredState("config", builder.config);

        // If we don't have a source projection specified, supply one.
        if (baseConfig.getProjections().stream().noneMatch(projection -> projection.getName().equals("source"))) {
            config = baseConfig.toBuilder().addProjection(Projection.builder().name("source").build()).build();
        } else {
            config = baseConfig;
        }

        // The `source` projection cannot include mappers or filters.
        Projection sourceProjection = config.getProjection("source").get();
        if (!sourceProjection.getTransforms().isEmpty()) {
            throw new SmithyBuildException("The source projection cannot contain any transforms");
        }

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
            outputDirectory = config.getOutputDirectory().get();
        } else {
            // Default the output directory to the current working directory + "./build/smithy"
            outputDirectory = Paths.get(".").toAbsolutePath().normalize().resolve("build").resolve("smithy");
        }

        if (builder.importBasePathResolver != null) {
            importBasePathResolver = builder.importBasePathResolver;
        } else {
            // Use the base path of the configuration or get the current working directory.
            Path basePath = config.getImportBasePath().orElseGet(() -> Paths.get(".").toAbsolutePath().normalize());
            importBasePathResolver = (path, cfg) -> basePath.resolve(path);
        }

        // Create the transformers for each projection.
        config.getProjections().forEach(p -> {
            BiFunction<ModelTransformer, Model, Model> modelTransformer = createTransformer(p, new LinkedHashSet<>());
            transformers.put(p.getName(), modelTransformer);
        });

        pluginModuleLayer = builder.pluginModuleLayer;
        pluginClassLoader = builder.pluginClassLoader;
    }

    SmithyBuildResult applyAllProjections() {
        Model resolvedModel = createBaseModel();
        SmithyBuildResult.Builder builder = SmithyBuildResult.builder();
        config.getProjections().stream()
                .filter(Predicate.not(Projection::isAbstract))
                .sorted(Comparator.comparing(Projection::getName))
                .parallel()
                .map(projection -> applyProjection(projection, resolvedModel))
                .forEach(builder::addProjectionResult);
        return builder.build();
    }

    private Model createBaseModel() {
        Model resolvedModel = model;

        if (!config.getImports().isEmpty()) {
            LOGGER.fine(() -> "Merging the following imports into the loaded model: " + config.getImports());
            ModelAssembler assembler = modelAssemblerSupplier.get().addModel(model);
            config.getImports().forEach(path -> assembler.addImport(importBasePathResolver.apply(path, config)));
            resolvedModel = assembler.assemble().unwrap();
        }

        return resolvedModel;
    }

    private ProjectionResult applyProjection(Projection projection, Model resolvedModel) {
        LOGGER.fine(() -> String.format("Creating the `%s` projection", projection.getName()));

        // Resolve imports.
        if (!projection.getImports().isEmpty()) {
            LOGGER.fine(() -> String.format(
                    "Merging the following `%s` projection imports into the loaded model: %s",
                    projection.getName(), projection.getImports()));
            var assembler = modelAssemblerSupplier.get().addModel(resolvedModel);
            projection.getImports().forEach(path -> assembler.addImport(importBasePathResolver.apply(path, config)));
            var resolvedResult = assembler.assemble();

            // Fail if the model can't be merged with the imports.
            if (resolvedResult.getResult().isEmpty()) {
                LOGGER.severe(String.format(
                        "The model could not be merged with the following imports: [%s[",
                        projection.getImports()));
                return ProjectionResult.builder()
                        .projectionName(projection.getName())
                        .events(resolvedResult.getValidationEvents())
                        .build();
            }

            resolvedModel = resolvedResult.unwrap();
        }

        // Create the base directory where all projection artifacts are stored.
        Path baseProjectionDir = outputDirectory.resolve(projection.getName());

        // Project the model and collect the results.
        Model projectedModel = transformers
                .get(projection.getName())
                .apply(modelTransformer, resolvedModel);

        ValidatedResult<Model> modelResult = modelAssemblerSupplier.get().addModel(projectedModel).assemble();

        ProjectionResult.Builder resultBuilder = ProjectionResult.builder()
                .projectionName(projection.getName())
                .model(projectedModel)
                .events(modelResult.getValidationEvents());

        for (Map.Entry<String, ObjectNode> entry : resolvePlugins(projection).entrySet()) {
            applyPlugin(projection, baseProjectionDir, entry.getKey(), entry.getValue(),
                        projectedModel, resolvedModel, modelResult, resultBuilder);
        }

        return resultBuilder.build();
    }

    private void applyPlugin(
            Projection projection,
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
            LOGGER.fine(() -> String.format(
                    "Unable to find a plugin for `%s` in the `%s` projection",
                    pluginName, projection.getName()));
        } else if (resolved.requiresValidModel() && modelResult.isBroken()) {
            LOGGER.fine(() -> String.format(
                    "Skipping `%s` plugin for `%s` projection because the model is broken",
                    pluginName, projection.getName()));
        } else {
            LOGGER.fine(() -> String.format(
                    "Applying `%s` plugin to `%s` projection",
                    pluginName, projection.getName()));
            resolved.execute(PluginContext.builder()
                    .model(projectedModel)
                    .originalModel(resolvedModel)
                    .projection(projection)
                    .events(modelResult.getValidationEvents())
                    .settings(pluginSettings)
                    .fileManifest(manifest)
                    .pluginModuleLayer(pluginModuleLayer)
                    .pluginClassLoader(pluginClassLoader)
                    .build());
            resultBuilder.addPluginManifest(pluginName, manifest);
        }
    }

    private Map<String, ObjectNode> resolvePlugins(Projection projection) {
        Map<String, ObjectNode> result = new TreeMap<>(config.getPlugins());
        result.putAll(projection.getPlugins());
        return result;
    }

    private BiFunction<ModelTransformer, Model, Model> createTransformer(
            Projection projection,
            Set<String> visited
    ) {
        if (visited.contains(projection.getName())) {
            visited.add(projection.getName());
            throw new SmithyBuildException(String.format(
                    "Cycle found in %s transforms: %s -> ...",
                    APPLY_PROJECTIONS,
                    String.join(" -> ", visited)));
        }

        visited.add(projection.getName());

        // Create a composed transformer of each created transformer.
        return projection.getTransforms().stream()
                .flatMap(transform -> getTransform(projection.getName(), transform, visited))
                .reduce((a, b) -> (transformer, model) -> b.apply(transformer, a.apply(transformer, model)))
                .orElse(((transformer, model) -> model));
    }

    private Stream<BiFunction<ModelTransformer, Model, Model>> getTransform(
            String projection,
            TransformConfiguration config,
            Set<String> visited
    ) {
        String name = config.getName();

        if (name.equals(APPLY_PROJECTIONS)) {
            return config.getArgs().stream()
                    .map(arg -> findProjection(projection, arg))
                    // Copy the set of visited projections to a new set;
                    // visiting the same projection isn't a problem, it's
                    // cycles that's problematic.
                    .map(targetProjection -> createTransformer(targetProjection, new LinkedHashSet<>(visited)));
        }

        ProjectionTransformer transformer = transformFactory.apply(name)
                .orElseThrow(() -> new UnknownTransformException("Unable to find a transform for `" + name + "`."));
        return Stream.of(transformer.createTransformer(config.getArgs()));
    }

    private Projection findProjection(String projection, String name) {
        return config.getProjection(name)
                .orElseThrow(() -> new UnknownProjectionException(String.format(
                        "Unable to find projection named `%s` referenced by `%s`", name, projection)));
    }
}
