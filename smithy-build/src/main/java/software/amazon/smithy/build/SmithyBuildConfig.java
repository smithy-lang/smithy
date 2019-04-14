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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Filter configuration that contains a list of named projections that
 * are used to apply filters to a model.
 */
public final class SmithyBuildConfig implements ToSmithyBuilder<SmithyBuildConfig> {
    static final Pattern PATTERN = Pattern.compile("^[A-Za-z0-9\\-_.]+$");
    private final List<String> imports;
    private final Path outputDirectory;
    private final Map<String, Projection> projections;
    private final Map<String, ObjectNode> plugins;
    private final Path importBasePath;

    private SmithyBuildConfig(Builder builder) {
        outputDirectory = builder.outputDirectory;
        imports = ListUtils.copyOf(builder.imports);
        projections = MapUtils.copyOf(builder.projections);
        plugins = MapUtils.copyOf(builder.plugins);
        importBasePath = builder.importBasePath;
    }

    /**
     * @return Creates a builder used to build a {@link SmithyBuildConfig}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Loads a SmithyBuildConfig from a JSON file on disk.
     *
     * <p>This method will set the import base path to match the directory
     * that contains the given file.
     *
     * <p>The file is expected to contain the following structure:
     *
     * <code>
     * {
     *     "version": "1.0",
     *     "imports": ["foo.json", "baz.json"],
     *     "outputDirectory": "build/output",
     *     "projections": {
     *         "projection-name": {
     *             "transforms": [
     *                 {"name": "transform-name", "config": ["argument1", "argument2", "..."]},
     *                 {"name": "other-transform"}
     *             },
     *             "plugins": {
     *                 "plugin-name": {
     *                     "plugin-config": "value"
     *                 },
     *                 "...": {}
     *             }
     *         }
     *     },
     *     "plugins": {
     *         "plugin-name": {
     *             "plugin-config": "value"
     *         },
     *         "...": {}
     *     }
     * }
     * </code>
     *
     * @param file File to load and parse.
     * @return Returns the loaded FileConfig.
     * @throws RuntimeException if the file cannot be loaded.
     */
    public static SmithyBuildConfig load(Path file) {
        return new ConfigLoader().load(file);
    }

    @Override
    public Builder toBuilder() {
        Builder builder = builder()
                .outputDirectory(outputDirectory)
                .importBasePath(importBasePath);
        projections.values().forEach(builder::addProjection);
        imports.forEach(builder::addImport);
        plugins.forEach(builder::addPlugin);
        return builder;
    }

    /**
     * Gets the paths to all of the models to import.
     *
     * <p>Paths can point to individual model files or directories.
     * All models stored in all recursive directories will be imported.
     *
     * @return Gets the list of models to import.
     */
    public List<String> getImports() {
        return imports;
    }

    /**
     * @return Gets the optional output directory to store artifacts.
     */
    public Optional<Path> getOutputDirectory() {
        return Optional.ofNullable(outputDirectory);
    }

    /**
     * Gets all of the configured projections.
     *
     * @return Gets the available projections.
     */
    public Collection<Projection> getProjections() {
        return Collections.unmodifiableCollection(projections.values());
    }

    /**
     * Gets a specific projection by name.
     *
     * @param projectionName Name of the projection to retrieve.
     * @return Returns the optionally found projection.
     */
    public Optional<Projection> getProjection(String projectionName) {
        return Optional.ofNullable(projections.get(projectionName));
    }

    /**
     * Gets the globally configured plugins that are applied to every
     * projection.
     *
     * @return Gets plugin settings.
     */
    public Map<String, ObjectNode> getPlugins() {
        return plugins;
    }

    /**
     * Gets a specific plugin's settings by name.
     *
     * @param pluginName Name of the plugin to retrieve.
     * @return Returns the optionally found plugin settings.
     */
    public Optional<ObjectNode> getPlugin(String pluginName) {
        return Optional.ofNullable(plugins.get(pluginName));
    }

    /**
     * Returns the base path of the configuration file.
     *
     * <p>This base path is used to resolve relative model imports. This
     * value is set automatically each time a JSON file is loaded. It can
     * also be set manually using {@link Builder#importBasePath}.
     *
     * @return Gets the optional base path of the config if known.
     */
    public Optional<Path> getImportBasePath() {
        return Optional.ofNullable(importBasePath);
    }

    /**
     * Builder used to create a {@link SmithyBuildConfig}.
     */
    public static final class Builder implements SmithyBuilder<SmithyBuildConfig> {
        private static final Map<String, ObjectNode> BUILTIN_PLUGINS = new HashMap<>();

        static {
            BUILTIN_PLUGINS.put("build-info", Node.objectNode());
            BUILTIN_PLUGINS.put("model", Node.objectNode());
        }

        private final List<String> imports = new ArrayList<>();
        private final Map<String, Projection> projections = new LinkedHashMap<>();
        private final Map<String, ObjectNode> plugins = new HashMap<>(BUILTIN_PLUGINS);
        private Path importBasePath;
        private Path outputDirectory;

        Builder() {}

        @Override
        public SmithyBuildConfig build() {
            return new SmithyBuildConfig(this);
        }

        /**
         * Loads the contents of a JSON file into the builder.
         *
         * <p>This method will change the import base path to match the path
         * that contains the given file. This can be overridden by setting the
         * base path using {@link #importBasePath} after calling {@code load}.
         *
         * @param path File to load and parse.
         * @return Returns the loaded FileConfig.
         * @throws RuntimeException if the file cannot be loaded.
         * @see SmithyBuildConfig#load for a description of the format.
         */
        public Builder load(Path path) {
            SmithyBuildConfig config = SmithyBuildConfig.load(path);
            config.getOutputDirectory().ifPresent(this::outputDirectory);
            config.getImports().forEach(this::addImport);
            config.getProjections().forEach(this::addProjection);
            config.getPlugins().forEach(this::addPlugin);
            importBasePath(config.getImportBasePath().orElse(null));
            return this;
        }

        /**
         * Add a model to import.
         *
         * @param importPath Model file or directory.
         * @return Returns the builder.
         */
        public Builder addImport(String importPath) {
            imports.add(importPath);
            return this;
        }

        /**
         * Add a model to import.
         *
         * @param importPath Model file or directory.
         * @return Returns the builder.
         */
        public Builder addImport(Path importPath) {
            return addImport(importPath.toString());
        }

        /**
         * Set a directory where the build artifacts are written.
         *
         * @param outputDirectory Directory where artifacts are written.
         * @return Returns the builder.
         */
        public Builder outputDirectory(Path outputDirectory) {
            this.outputDirectory = outputDirectory;
            return this;
        }

        /**
         * Set a directory where the build artifacts are written.
         *
         * @param outputDirectory Directory where artifacts are written.
         * @return Returns the builder.
         */
        public Builder outputDirectory(String outputDirectory) {
            return outputDirectory(Paths.get(outputDirectory));
        }

        /**
         * Registers a projection by name.
         *
         * @param projection Projection that contains filters.
         * @return Returns the builder.
         */
        public Builder addProjection(Projection projection) {
            this.projections.put(projection.getName(), projection);
            return this;
        }

        /**
         * Registers a plugin with the builder.
         *
         * @param name Plugin name.
         * @param settings Plugin settings.
         * @return Returns the builder.
         */
        public Builder addPlugin(String name, ObjectNode settings) {
            if (!PATTERN.matcher(name).find()) {
                throw new SmithyBuildException(String.format(
                        "Invalid plugin name: `%s`. Plugin names must match the following pattern: %s",
                        name, PATTERN.pattern()));
            }

            this.plugins.put(name, settings);
            return this;
        }

        /**
         * Sets the base path to use when resolving relative import paths.
         *
         * <p>Note that this setting is changed each time that {@link #load}
         * is invoked.
         *
         * @param importBasePath Import base path to set.
         * @return Returns the builder.
         */
        public Builder importBasePath(Path importBasePath) {
            this.importBasePath = importBasePath;
            return this;
        }
    }
}
