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

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Logger;
import software.amazon.smithy.build.model.SmithyBuildConfig;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.ModelAssembler;
import software.amazon.smithy.model.transform.ModelTransformer;

/**
 * Runs the projections and plugins found in a {@link SmithyBuildConfig}
 * and writes the artifacts to a {@link FileManifest}.
 */
public final class SmithyBuild {
    /** The version of Smithy build. */
    public static final String VERSION = "1.0";

    private static final Logger LOGGER = Logger.getLogger(SmithyBuild.class.getName());
    private static final PathMatcher VALID_MODELS = FileSystems.getDefault().getPathMatcher("glob:*.{json,jar,smithy}");

    SmithyBuildConfig config;
    Path outputDirectory;
    Function<String, Optional<ProjectionTransformer>> transformFactory;
    Function<String, Optional<SmithyBuildPlugin>> pluginFactory;
    Function<Path, FileManifest> fileManifestFactory;
    Supplier<ModelAssembler> modelAssemblerSupplier;
    ModelTransformer modelTransformer;
    Model model;
    ClassLoader pluginClassLoader;
    Set<Path> sources = new HashSet<>();
    Predicate<String> projectionFilter = name -> true;
    Predicate<String> pluginFilter = name -> true;

    public SmithyBuild() {}

    public SmithyBuild(SmithyBuildConfig config) {
        config(config);
    }

    /**
     * Creates a {@code SmithyBuild} implementation that is configured to
     * discover various Smithy service providers using the given
     * {@code ClassLoader}.
     *
     * @param classLoader ClassLoader used to discover service providers.
     * @return Returns the created {@code SmithyBuild} object.
     */
    public static SmithyBuild create(ClassLoader classLoader) {
        ModelAssembler assembler = Model.assembler(classLoader);
        return create(classLoader, assembler::copy);
    }

    /**
     * Creates a {@code SmithyBuild} implementation that is configured to
     * discover various Smithy service providers using the given
     * {@code ClassLoader}.
     *
     * @param classLoader ClassLoader used to discover service providers.
     * @param modelAssemblerSupplier Supplier used to create {@link ModelAssembler}s in each build.
     * @return Returns the created {@code SmithyBuild} object.
     */
    public static SmithyBuild create(ClassLoader classLoader, Supplier<ModelAssembler> modelAssemblerSupplier) {
        return new SmithyBuild()
                .modelAssemblerSupplier(modelAssemblerSupplier)
                .modelTransformer(ModelTransformer.createWithServiceProviders(classLoader))
                .transformFactory(ProjectionTransformer.createServiceFactory(classLoader))
                .pluginFactory(SmithyBuildPlugin.createServiceFactory(classLoader))
                .pluginClassLoader(classLoader);
    }

    /**
     * Gets the default directory where smithy-build artifacts are written.
     *
     * @return Returns the build output path.
     */
    public static Path getDefaultOutputDirectory() {
        return DefaultPathHolder.DEFAULT_PATH;
    }

    /**
     * Builds the model and applies all projections.
     *
     * <p>This method loads all projections, projected models, and their
     * results into memory so that a {@link SmithyBuildResult} can be
     * returned. See {@link #build(Consumer, BiConsumer)} for a streaming
     * approach that uses callbacks and does not load all projections into
     * memory at once.
     *
     * <p>Errors are aggregated together into a single
     * {@link SmithyBuildException} that contains an aggregated error
     * message and each encountered exception is registered to the aggregate
     * exception through {@link Throwable#addSuppressed(Throwable)}.
     *
     * @return Returns the result of building the model.
     * @throws IllegalStateException if a {@link SmithyBuildConfig} is not set.
     * @throws SmithyBuildException if the build fails.
     * @see #build(Consumer, BiConsumer)
     */
    public SmithyBuildResult build() {
        SmithyBuildResult.Builder resultBuilder = SmithyBuildResult.builder();
        Map<String, Throwable> errors = Collections.synchronizedMap(new TreeMap<>());
        build(resultBuilder::addProjectionResult, errors::put);

        if (!errors.isEmpty()) {
            StringBuilder message = new StringBuilder();
            message.append(errors.size()).append(" Smithy build projections failed.");
            message.append(System.lineSeparator()).append(System.lineSeparator());

            for (Map.Entry<String, Throwable> e : errors.entrySet()) {
                message.append("(").append(e.getKey()).append("): ")
                        .append(e.getValue())
                        .append(System.lineSeparator());
            }

            SmithyBuildException buildException = new SmithyBuildException(message.toString());
            errors.values().forEach(buildException::addSuppressed);
            throw buildException;
        }

        return resultBuilder.build();
    }

    /**
     * Builds the model and applies all projections, passing each
     * {@link ProjectionResult} to the provided callback as they are
     * completed and each encountered exception to the provided
     * {@code exceptionCallback} as they are encountered.
     *
     * <p>This method differs from {@link #build()} in that it does not
     * require every projection and projection result to be loaded into
     * memory.
     *
     * <p>The result each projection is placed in the outputDirectory.
     * A {@code [projection]-build-info.json} file is created in the output
     * directory. A directory is created for each projection using the
     * projection name, and a file named model.json is place in each directory.
     *
     * @param resultCallback A thread-safe callback that receives projection
     *   results as they complete.
     * @param exceptionCallback A thread-safe callback that receives the name
     *   of each failed projection and the exception that occurred.
     * @throws IllegalStateException if a {@link SmithyBuildConfig} is not set.
     */
    public void build(Consumer<ProjectionResult> resultCallback, BiConsumer<String, Throwable> exceptionCallback) {
        new SmithyBuildImpl(this).applyAllProjections(resultCallback, exceptionCallback);
    }

    /**
     * Sets the <strong>required</strong> configuration object used to
     * build the model.
     *
     * @param config Configuration to set.
     * @return Returns the builder.
     */
    public SmithyBuild config(SmithyBuildConfig config) {
        this.config = config;
        for (String source : config.getSources()) {
            addSource(Paths.get(source));
        }
        return this;
    }

    // Add a source path using absolute paths to better de-conflict source files. ModelAssembler also
    // de-conflicts imports with absolute paths, but this ensures the same file doesn't appear twice in
    // the build plugin output (though it does not use realpath to de-conflict based on symlinks).
    //
    // Ignores and logs when an unsupported model file is encountered.
    private void addSource(Path path) {
        if (Files.isRegularFile(path) && !VALID_MODELS.matches(path.getFileName())) {
            LOGGER.warning("Omitting unsupported Smithy model file from model sources: " + path);
        } else {
            sources.add(path.toAbsolutePath());
        }
    }

    /**
     * Sets the <strong>required</strong> configuration object used to
     * build the model.
     *
     * @param configPath Path to the configuration to set.
     * @return Returns the builder.
     */
    public SmithyBuild config(Path configPath) {
        return config(SmithyBuildConfig.load(configPath));
    }

    /**
     * Sets an optional model to use with the build. The provided model is
     * used alongside any "imports" found in the configuration object.
     *
     * @param model Model to build.
     * @return Returns the builder.
     */
    public SmithyBuild model(Model model) {
        this.model = model;
        return this;
    }

    @Deprecated
    public SmithyBuild importBasePath(Path importBasePath) {
        return this;
    }

    /**
     * Set a directory where the build artifacts are written.
     *
     * <p>Calling this method will supersede any {@code outputDirectory}
     * setting returned by {@link SmithyBuildConfig#getOutputDirectory()}.
     *
     * <p>If no output directory is specified here or in the config, then
     * a default output directory of the current working directory resolved
     * with {@code ./build/smithy} is used.
     *
     * @param outputDirectory Directory where artifacts are written.
     * @return Returns the builder.
     *
     * @see SmithyBuildConfig#getOutputDirectory
     */
    public SmithyBuild outputDirectory(Path outputDirectory) {
        this.outputDirectory = outputDirectory;
        return this;
    }

    /**
     * Set a directory where the build artifacts are written.
     *
     * <p>Calling this method will supersede any {@code outputDirectory}
     * setting returned by {@link SmithyBuildConfig#getOutputDirectory()}.
     *
     * <p>If no output directory is specified here or in the config, then
     * a default output directory of the current working directory +
     * {@code ./build/smithy} is used.
     *
     * @param outputDirectory Directory where artifacts are written.
     * @return Returns the builder.
     *
     * @see SmithyBuildConfig#getOutputDirectory
     */
    public SmithyBuild outputDirectory(String outputDirectory) {
        return outputDirectory(Paths.get(outputDirectory));
    }

    /**
     * Sets a factory function that's used to create {@link FileManifest}
     * objects when writing {@link SmithyBuildPlugin} artifacts.
     *
     * <p>A default implementation of {@link FileManifest#create} will be
     * used if a custom factory is not provided.
     *
     * @param fileManifestFactory Factory that accepts a base path and
     *  returns a {@link FileManifest}.
     * @return Returns the builder.
     */
    public SmithyBuild fileManifestFactory(Function<Path, FileManifest> fileManifestFactory) {
        this.fileManifestFactory = fileManifestFactory;
        return this;
    }

    /**
     * Called to create {@link ModelAssembler} to load the original
     * model and to load each projected model.
     *
     * <p>If not provided, the runner will use a default ModelAssembler
     * implementation that discovers traits, validators, and other
     * service providers using the class path and module path of
     * {@code software.smithy.model}.
     *
     * <p>Warning: this supplier can be invoked multiple times to build
     * a single projection, cache things like service provider factories
     * when necessary. The same instance of a ModelAssembler MUST NOT
     * be returned from successive calls to the supplier because assemblers
     * are created and mutated in different threads.
     *
     * @param modelAssemblerSupplier ModelValidator supplier to utilize.
     * @return Returns the builder.
     */
    public SmithyBuild modelAssemblerSupplier(Supplier<ModelAssembler> modelAssemblerSupplier) {
        this.modelAssemblerSupplier = modelAssemblerSupplier;
        return this;
    }

    /**
     * Sets a custom {@link ModelTransformer} to use when building
     * projections.
     *
     * <p>The runner will use a default ModelTransformer if one is not
     * provided.
     *
     * @param modelTransformer Transformer to set.
     * @return Returns the builder.
     */
    public SmithyBuild modelTransformer(ModelTransformer modelTransformer) {
        this.modelTransformer = modelTransformer;
        return this;
    }

    /**
     * Sets a factory function used to create transforms by name.
     *
     * <p>A default implementation that utilizes Java SPI to discover
     * implementations of {@link ProjectionTransformer} will be
     * used if a custom factory is not provided.
     *
     * @param transformFactory Factory that accepts a transform name and
     *  returns the optionally found transform.
     * @return Returns the builder.
     *
     * @see ProjectionTransformer#createServiceFactory
     */
    public SmithyBuild transformFactory(Function<String, Optional<ProjectionTransformer>> transformFactory) {
        this.transformFactory = transformFactory;
        return this;
    }

    /**
     * Sets a factory function used to create plugins by name.
     *
     * <p>A default implementation that utilizes Java SPI to discover
     * implementations of {@link SmithyBuildPlugin} will be used if a
     * custom factory is not provided.
     *
     * @param pluginFactory Plugin factory that accepts a plugin name and
     *  returns the optionally found plugin.
     * @return Returns the builder.
     *
     * @see SmithyBuildPlugin#createServiceFactory
     */
    public SmithyBuild pluginFactory(Function<String, Optional<SmithyBuildPlugin>> pluginFactory) {
        this.pluginFactory = pluginFactory;
        return this;
    }

    /**
     * Sets a ClassLoader that should be used by SmithyBuild plugins when
     * discovering services.
     *
     * @param pluginClassLoader ClassLoader plugins discover services with.
     * @return Returns the builder.
     */
    public SmithyBuild pluginClassLoader(ClassLoader pluginClassLoader) {
        this.pluginClassLoader = pluginClassLoader;
        return this;
    }

    /**
     * Registers the given paths as sources of the model being built.
     *
     * <p>There are typically two kinds of models that are added to a build:
     * source models and discovered models. Discovered models are someone
     * else's models. Source models are the models owned by the package
     * being built.
     *
     * <p>Source models are copied into the automatically executed "manifest"
     * plugin. If no transformations were applied to the sources, then the
     * source models are copied literally into the manifest directory output.
     * Otherwise, a modified version of the source models are copied.
     *
     * <p>When a directory is provided, all of the files in the directory are
     * treated as sources, and they are relativized to remove the directory.
     * When a file is provided, the directory that contains that file is used
     * as a source. All of the relativized files resolved in sources must be
     * unique across the entire set of files. The sources directories are
     * essentially flattened into a single directory.
     *
     * <p>Unsupported model files are ignored and not treated as sources.
     * This can happen when adding model files from a directory that contains
     * a mix of model files and non-model files. Filtering models here prevents
     * unsupported files from appearing in places like JAR manifest files where
     * they are not allowed.
     *
     * @param pathToSources Path to source directories to mark.
     * @return Returns the builder.
     */
    public SmithyBuild registerSources(Path... pathToSources) {
        for (Path path : pathToSources) {
            addSource(path);
        }
        return this;
    }

    /**
     * Sets a predicate that accepts the name of a projection and returns
     * true if the projection should be built.
     *
     * @param projectionFilter Predicate that accepts a projection name.
     * @return Returns the builder.
     */
    public SmithyBuild projectionFilter(Predicate<String> projectionFilter) {
        this.projectionFilter = Objects.requireNonNull(projectionFilter);
        return this;
    }

    /**
     * Sets a predicate that accepts the name of a plugin and returns
     * true if the plugin should be built.
     *
     * @param pluginFilter Predicate that accepts a projection name.
     * @return Returns the builder.
     */
    public SmithyBuild pluginFilter(Predicate<String> pluginFilter) {
        this.pluginFilter = Objects.requireNonNull(pluginFilter);
        return this;
    }

    // Lazy initialization holder class idiom.
    private static final class DefaultPathHolder {
        private static final Path DEFAULT_PATH = resolveDefaultPath();

        private static Path resolveDefaultPath() {
            return Paths.get(".").toAbsolutePath().normalize().resolve("build").resolve("smithy");
        }
    }
}
