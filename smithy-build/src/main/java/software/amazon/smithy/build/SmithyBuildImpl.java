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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import software.amazon.smithy.build.model.ProjectionConfig;
import software.amazon.smithy.build.model.SmithyBuildConfig;
import software.amazon.smithy.build.model.TransformConfig;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.ModelAssembler;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.transform.ModelTransformer;
import software.amazon.smithy.model.validation.ValidatedResult;
import software.amazon.smithy.utils.Pair;
import software.amazon.smithy.utils.SmithyBuilder;

final class SmithyBuildImpl {
    private static final Logger LOGGER = Logger.getLogger(SmithyBuild.class.getName());

    // Must start with one or more alphanumerics, followed by zero or more alphanumerics, "-", "_", or ".".
    private static final String PATTERN_PART = "[A-Za-z0-9]+[A-Za-z0-9\\-_.]*";

    // The pattern for projections and plugins must only be a valid PATTERN_PART.
    private static final Pattern PATTERN = Pattern.compile("^" + PATTERN_PART + "$");

    // Must start with letter/number. Allows for optional artifact name: plugin-name::artifact-name
    private static final Pattern PLUGIN_PATTERN = Pattern
            .compile("^" + PATTERN_PART + "(::" + PATTERN_PART + ")?$");

    private final SmithyBuildConfig config;
    private final Function<Path, FileManifest> fileManifestFactory;
    private final Supplier<ModelAssembler> modelAssemblerSupplier;
    private final Path outputDirectory;
    private final Map<String, List<Pair<ObjectNode, ProjectionTransformer>>> transformers = new HashMap<>();
    private final ModelTransformer modelTransformer;
    private final Function<String, Optional<ProjectionTransformer>> transformFactory;
    private final Function<String, Optional<SmithyBuildPlugin>> pluginFactory;
    private final Model model;
    private final ClassLoader pluginClassLoader;
    private final Set<Path> sources;
    private final Predicate<String> projectionFilter;
    private final Predicate<String> pluginFilter;

    private static final class ResolvedPlugin {
        final PluginId id;
        final SmithyBuildPlugin plugin;
        final ObjectNode config;

        ResolvedPlugin(PluginId id, SmithyBuildPlugin plugin, ObjectNode config) {
            this.id = id;
            this.plugin = plugin;
            this.config = config;
        }
    }

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
            outputDirectory = SmithyBuild.getDefaultOutputDirectory();
        }

        // Create the transformers for each projection.
        config.getProjections().forEach((projectionName, projectionConfig) -> {
            transformers.put(projectionName, createTransformers(projectionName, projectionConfig));
        });

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
        if (!PLUGIN_PATTERN.matcher(plugin).matches()) {
            throw new SmithyBuildException(String.format(
                    "Invalid plugin name `%s` found in the `%s` projection. "
                    + " Plugin names must match the following regex: %s", plugin, projection, PLUGIN_PATTERN));
        }
    }

    void applyAllProjections(
            Consumer<ProjectionResult> projectionResultConsumer,
            BiConsumer<String, Throwable> projectionExceptionConsumer
    ) {
        ValidatedResult<Model> resolvedModel = createBaseModel();

        // Some plugins need things like file locks and can't be run in parallel with other plugins.
        // When a serial plugin is encountered for a projection, run the projection immediately.
        // Parallel projections are collected into a list and run in parallel after serial projections.
        // Note: we may later decide to run all parallel plugins in parallel across projections.
        List<Runnable> parallelProjections = new ArrayList<>();

        for (Map.Entry<String, ProjectionConfig> entry : config.getProjections().entrySet()) {
            String projectionName = entry.getKey();
            ProjectionConfig config = entry.getValue();

            if (config.isAbstract() || !projectionFilter.test(projectionName)) {
                continue;
            }

            List<ResolvedPlugin> resolvedPlugins = resolvePlugins(projectionName, config);

            if (areAnyResolvedPluginsSerial(resolvedPlugins)) {
                executeSerialProjection(resolvedModel, projectionName, config, resolvedPlugins,
                                        projectionResultConsumer, projectionExceptionConsumer);
            } else {
                parallelProjections.add(() -> {
                    executeSerialProjection(resolvedModel, projectionName, config, resolvedPlugins,
                                            projectionResultConsumer, projectionExceptionConsumer);
                });
            }
        }

        if (parallelProjections.size() == 1) {
            parallelProjections.get(0).run();
        } else if (!parallelProjections.isEmpty()) {
            parallelProjections.parallelStream().forEach(Runnable::run);
        }
    }

    private List<ResolvedPlugin> resolvePlugins(String projectionName, ProjectionConfig config) {
        // Ensure that no two plugins use the same artifact name.
        Set<String> seenArtifactNames = new HashSet<>();
        List<ResolvedPlugin> resolvedPlugins = new ArrayList<>();

        for (Map.Entry<String, ObjectNode> pluginEntry : getCombinedPlugins(config).entrySet()) {
            PluginId id = PluginId.from(pluginEntry.getKey());
            if (!seenArtifactNames.add(id.getArtifactName())) {
                throw new SmithyBuildException(String.format(
                        "Multiple plugins use the same artifact name '%s' in the '%s' projection",
                        id.getArtifactName(), projectionName));
            }
            createPlugin(projectionName, id).ifPresent(plugin -> {
                resolvedPlugins.add(new ResolvedPlugin(id, plugin, pluginEntry.getValue()));
            });
        }

        return resolvedPlugins;
    }

    private Map<String, ObjectNode> getCombinedPlugins(ProjectionConfig projection) {
        Map<String, ObjectNode> result = new TreeMap<>(config.getPlugins());
        result.putAll(projection.getPlugins());
        return result;
    }

    private Optional<SmithyBuildPlugin> createPlugin(String projectionName, PluginId id) {
        SmithyBuildPlugin plugin = pluginFactory.apply(id.getPluginName()).orElse(null);

        if (plugin != null) {
            return Optional.of(plugin);
        }

        String message = "Unable to find a plugin for `" + id + "` in the `" + projectionName + "` "
                         + "projection. Is this the correct spelling? Are you missing a dependency? Is your "
                         + "classpath configured correctly?";

        if (config.isIgnoreMissingPlugins()) {
            LOGGER.severe(message);
            return Optional.empty();
        }

        throw new SmithyBuildException(message);
    }

    private boolean areAnyResolvedPluginsSerial(List<ResolvedPlugin> resolvedPlugins) {
        for (ResolvedPlugin plugin : resolvedPlugins) {
            if (plugin.plugin.isSerial()) {
                return true;
            }
        }
        return false;
    }

    private void executeSerialProjection(
            ValidatedResult<Model> baseModel,
            String name,
            ProjectionConfig config,
            List<ResolvedPlugin> resolvedPlugins,
            Consumer<ProjectionResult> projectionResultConsumer,
            BiConsumer<String, Throwable> projectionExceptionConsumer
    ) {
        // Errors that occur while invoking the result callback must not
        // cause the exception callback to be invoked.
        ProjectionResult result = null;

        try {
            result = applyProjection(name, config, baseModel, resolvedPlugins);
        } catch (Throwable e) {
            projectionExceptionConsumer.accept(name, e);
        }

        if (result != null) {
            projectionResultConsumer.accept(result);
        }
    }

    private ValidatedResult<Model> createBaseModel() {
        if (!config.getImports().isEmpty()) {
            LOGGER.fine(() -> "Merging the following imports into the loaded model: " + config.getImports());
        }

        ModelAssembler assembler = modelAssemblerSupplier.get().addModel(model);
        config.getImports().forEach(assembler::addImport);
        return assembler.assemble();
    }

    private ProjectionResult applyProjection(
            String projectionName,
            ProjectionConfig projection,
            ValidatedResult<Model> baseModel,
            List<ResolvedPlugin> resolvedPlugins
    ) throws Throwable {
        Model resolvedModel = baseModel.unwrap();
        LOGGER.fine(() -> String.format("Creating the `%s` projection", projectionName));

        // Resolve imports, and overwrite baseModel.
        if (!projection.getImports().isEmpty()) {
            LOGGER.fine(() -> String.format(
                    "Merging the following `%s` projection imports into the loaded model: %s",
                    projectionName, projection.getImports()));
            ModelAssembler assembler = modelAssemblerSupplier.get().addModel(resolvedModel);
            projection.getImports().forEach(assembler::addImport);
            baseModel = assembler.assemble();

            // Fail if the model can't be merged with the imports.
            if (baseModel.isBroken() || !baseModel.getResult().isPresent()) {
                LOGGER.severe(String.format(
                        "The model could not be merged with the following imports: [%s]",
                        projection.getImports()));
                return ProjectionResult.builder()
                        // Create an empty model so that ProjectionResult can be created when
                        // the Model can't be assembled.
                        .model(Model.builder().build())
                        .projectionName(projectionName)
                        .events(baseModel.getValidationEvents())
                        .build();
            }

            resolvedModel = baseModel.unwrap();
        }

        // Create the base directory where all projection artifacts are stored.
        Path baseProjectionDir = outputDirectory.resolve(projectionName);

        Model projectedModel = resolvedModel;
        ValidatedResult<Model> modelResult = baseModel;

        // Don't do another round of validation and transforms if there are no transforms.
        // This is the case on the source projection, for example.
        if (!projection.getTransforms().isEmpty()) {
            LOGGER.fine(() -> String.format("Applying transforms to projection %s: %s", projectionName,
                    projection.getTransforms().stream().map(TransformConfig::getName).collect(Collectors.toList())));
            projectedModel = applyProjectionTransforms(
                    baseModel, resolvedModel, projectionName, Collections.emptySet());
            modelResult = modelAssemblerSupplier.get().addModel(projectedModel).assemble();
        } else {
            LOGGER.fine(() -> String.format("No transforms to apply for projection %s", projectionName));
        }

        // Keep track of the first error created by plugins to fail the build after all plugins have run.
        Throwable firstPluginError = null;
        ProjectionResult.Builder resultBuilder = ProjectionResult.builder()
                .projectionName(projectionName)
                .model(projectedModel)
                .events(modelResult.getValidationEvents());

        for (ResolvedPlugin resolvedPlugin : resolvedPlugins) {
            if (pluginFilter.test(resolvedPlugin.id.getArtifactName())) {
                try {
                    applyPlugin(projectionName, projection, baseProjectionDir, resolvedPlugin,
                            projectedModel, resolvedModel, modelResult, resultBuilder);
                } catch (Throwable e) {
                    if (firstPluginError == null) {
                        firstPluginError = e;
                    } else {
                        // Only log subsequent errors, since the first one is thrown.
                        LOGGER.severe(String.format("Plugin `%s` failed: %s", resolvedPlugin.id, e));
                    }
                }
            }
        }

        if (firstPluginError != null) {
            throw firstPluginError;
        }

        return resultBuilder.build();
    }

    private Model applyProjectionTransforms(
            ValidatedResult<Model> baseModel,
            Model currentModel,
            String projectionName,
            Set<String> visited
    ) {
        Model originalModel = baseModel.unwrap();

        for (Pair<ObjectNode, ProjectionTransformer> transformerBinding : transformers.get(projectionName)) {
            TransformContext context = TransformContext.builder()
                    .model(currentModel)
                    .originalModel(originalModel)
                    .originalModelValidationEvents(baseModel.getValidationEvents())
                    .transformer(modelTransformer)
                    .projectionName(projectionName)
                    .sources(sources)
                    .settings(transformerBinding.left)
                    .build();
            currentModel = transformerBinding.right.transform(context);
            Collection<String> queuedProjections = transformerBinding.right.getAdditionalProjections(context);
            currentModel = applyQueuedProjections(queuedProjections, context, currentModel, visited);
        }

        return currentModel;
    }

    private void applyPlugin(
            String projectionName,
            ProjectionConfig projection,
            Path baseProjectionDir,
            ResolvedPlugin resolvedPlugin,
            Model projectedModel,
            Model resolvedModel,
            ValidatedResult<Model> modelResult,
            ProjectionResult.Builder resultBuilder
    ) {
        PluginId id = resolvedPlugin.id;

        // Create the manifest where plugin artifacts are stored.
        Path pluginBaseDir = baseProjectionDir.resolve(id.getArtifactName());
        FileManifest manifest = fileManifestFactory.apply(pluginBaseDir);

        if (resolvedPlugin.plugin.requiresValidModel() && modelResult.isBroken()) {
            LOGGER.fine(() -> String.format("Skipping `%s` plugin for `%s` projection because the model is broken",
                                            id, projectionName));
        } else {
            LOGGER.info(() -> String.format("Applying `%s` plugin to `%s` projection", id, projectionName));
            resolvedPlugin.plugin
                    .execute(PluginContext.builder()
                    .model(projectedModel)
                    .originalModel(resolvedModel)
                    .projection(projectionName, projection)
                    .events(modelResult.getValidationEvents())
                    .settings(resolvedPlugin.config)
                    .fileManifest(manifest)
                    .pluginClassLoader(pluginClassLoader)
                    .sources(sources)
                    .artifactName(id.hasArtifactName() ? id.getArtifactName() : null)
                    .build());
            resultBuilder.addPluginManifest(id.getArtifactName(), manifest);
        }
    }

    // Creates pairs where the left value is the configuration arguments of the
    // transformer, and the right value is the instantiated transformer.
    private List<Pair<ObjectNode, ProjectionTransformer>> createTransformers(
            String projectionName,
            ProjectionConfig config
    ) {
        List<Pair<ObjectNode, ProjectionTransformer>> resolved = new ArrayList<>(config.getTransforms().size());

        for (TransformConfig transformConfig : config.getTransforms()) {
            String name = transformConfig.getName();
            ProjectionTransformer transformer = transformFactory.apply(name)
                    .orElseThrow(() -> new UnknownTransformException(String.format(
                            "Unable to find a transform named `%s` in the `%s` projection. Is this the correct "
                            + "spelling? Are you missing a dependency? Is your classpath configured correctly?",
                            name, projectionName)));
            resolved.add(Pair.of(transformConfig.getArgs(), transformer));
        }

        return resolved;
    }

    private Model applyQueuedProjections(Collection<String> queuedProjections,
                                         TransformContext context,
                                         Model currentModel,
                                         Set<String> visited) {
        LOGGER.fine(() -> String.format("Applying queued projections: %s", queuedProjections));
        for (String projectionTarget : queuedProjections) {
            Set<String> updatedVisited = new LinkedHashSet<>(visited);
            if (context.getProjectionName().equals(projectionTarget)) {
                throw new SmithyBuildException("Cannot recursively apply the same projection: " + projectionTarget);
            } else if (!transformers.containsKey(projectionTarget)) {
                throw new UnknownProjectionException(String.format(
                        "Unable to find projection named `%s` referenced by the `%s` projection",
                        projectionTarget, context.getProjectionName()));
            } else if (visited.contains(projectionTarget)) {
                updatedVisited.add(projectionTarget);
                throw new SmithyBuildException(String.format(
                        "Cycle found in apply transforms: %s -> ...", String.join(" -> ", updatedVisited)));
            }

            updatedVisited.add(projectionTarget);
            currentModel = applyProjectionTransforms(
                    new ValidatedResult<>(context.getOriginalModel().orElse(currentModel),
                                          context.getOriginalModelValidationEvents()),
                    currentModel,
                    projectionTarget,
                    updatedVisited);
        }

        return currentModel;
    }
}
