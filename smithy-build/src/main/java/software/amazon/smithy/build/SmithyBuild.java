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
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.ModelAssembler;
import software.amazon.smithy.model.transform.ModelTransformer;

/**
 * Runs the projections and plugins found in a {@link SmithyBuildConfig}
 * and writes the artifacts to a {@link FileManifest}.
 */
public final class SmithyBuild {
    SmithyBuildConfig config;
    Path outputDirectory;
    BiFunction<String, SmithyBuildConfig, Path> importBasePathResolver;
    Function<String, Optional<ProjectionTransformer>> transformFactory;
    Function<String, Optional<SmithyBuildPlugin>> pluginFactory;
    Function<Path, FileManifest> fileManifestFactory;
    Supplier<ModelAssembler> modelAssemblerSupplier;
    ModelTransformer modelTransformer;
    Model model;
    ClassLoader pluginClassLoader;
    Set<Path> sources = new HashSet<>();

    public SmithyBuild() {}

    public SmithyBuild(SmithyBuildConfig config) {
        this.config = config;
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
     * Builds the model and applies all projections.
     *
     * <p>The result each projection is placed in the outputDirectory.
     * A {@code [projection]-build-info.json} file is created in the output
     * directory. A directory is created for each projection using the
     * projection name, and a file named model.json is place in each directory.
     *
     * @return Returns the result of building the model.
     * @throws IllegalStateException if a {@link SmithyBuildConfig} is not set.
     */
    public SmithyBuildResult build() {
        return new SmithyBuildImpl(this).applyAllProjections();
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
        return this;
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

    /**
     * Sets the <em>import base path resolver</em> function to use to find
     * imports.
     *
     * <p>This function is invoked each time an import defined in the config
     * is loaded. This function is provided the path String to the file to
     * import followed by the smithy build configuration object. The function
     * is expected to return the resolved path to the file to import.
     *
     * <p>If no custom import path resolver function is provided, a default
     * function will be used to that resolves import paths based on the
     * location of the smithy-build.json configuration file on disk (if known)
     * using {@link SmithyBuildConfig#getImportBasePath}. For example:
     *
     * <pre>
     * {@code
     * Path basePath = Paths.get("/foo/baz");
     * BuilderRunner runner = new BuilderRunner();
     * runner.importBasePathResolver((path, config) -> basePath.resolve(path));
     * }
     * </pre>
     *
     * @param importBasePathResolver Import path resolved to use.
     * @return Returns the builder.
     */
    public SmithyBuild importBasePathResolver(BiFunction<String, SmithyBuildConfig, Path> importBasePathResolver) {
        this.importBasePathResolver = importBasePathResolver;
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
     * @param pathToSources Path to source directories to mark.
     * @return Returns the builder.
     */
    public SmithyBuild registerSources(Path... pathToSources) {
        Collections.addAll(sources, pathToSources);
        return this;
    }
}
