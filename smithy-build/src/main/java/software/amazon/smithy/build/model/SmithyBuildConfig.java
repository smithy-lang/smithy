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

package software.amazon.smithy.build.model;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.SetUtils;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Filter configuration that contains a list of named projections that
 * are used to apply filters to a model.
 */
public final class SmithyBuildConfig implements ToSmithyBuilder<SmithyBuildConfig> {
    private static final Set<String> BUILTIN_PLUGINS = SetUtils.of("build-info", "model", "sources");

    private final String version;
    private final List<String> imports;
    private final List<String> mavenRepositories;
    private final List<String> mavenDependencies;
    private final String outputDirectory;
    private final Map<String, ProjectionConfig> projections;
    private final Map<String, ObjectNode> plugins;
    private final boolean ignoreMissingPlugins;

    private SmithyBuildConfig(Builder builder) {
        SmithyBuilder.requiredState("version", builder.version);
        version = builder.version;
        outputDirectory = builder.outputDirectory;
        imports = ListUtils.copyOf(builder.imports);
        mavenRepositories = ListUtils.copyOf(builder.mavenRepositories);
        mavenDependencies = ListUtils.copyOf(builder.mavenDependencies);
        projections = MapUtils.copyOf(builder.projections);
        plugins = new HashMap<>(builder.plugins);
        ignoreMissingPlugins = builder.ignoreMissingPlugins;
        for (String builtin : BUILTIN_PLUGINS) {
            plugins.put(builtin, Node.objectNode());
        }
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
     *                 {"name": "transform-name", "args": ["argument1", "argument2", "..."]},
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
        return builder().load(file).build();
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .version(version)
                .outputDirectory(outputDirectory)
                .imports(imports)
                .projections(projections)
                .plugins(plugins)
                .ignoreMissingPlugins(ignoreMissingPlugins);
    }

    /**
     * Gets the version of Smithy-Build.
     *
     * @return Returns the version.
     */
    public String getVersion() {
        return version;
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
   * Gets the maven repositories to all of the models to import.
   *
   * @return Gets the list of maven repositories to fetch maven imports from.
   */
  public List<String> getMavenRepositories() {
    return mavenRepositories;
  }

  /**
   * Gets the maven imports to all of the models to import.
   *
   * <p>
   * Maven imports should follow the following syntax :
   * `organization:artifact:version`
   *
   * @return The list of maven imports which should be fetched and imported into
   *         the model.
   */
  public List<String> getMavenDependencies() {
    return mavenDependencies;
  }


    /**
     * @return Gets the optional output directory to store artifacts.
     */
    public Optional<String> getOutputDirectory() {
        return Optional.ofNullable(outputDirectory);
    }

    /**
     * Gets all of the configured projections.
     *
     * @return Gets the available projections as a map of name to config.
     */
    public Map<String, ProjectionConfig> getProjections() {
        return Collections.unmodifiableMap(projections);
    }

    /**
     * Gets the globally configured plugins that are applied to every
     * projection.
     *
     * @return Gets plugin settings.
     */
    public Map<String, ObjectNode> getPlugins() {
        return Collections.unmodifiableMap(plugins);
    }

    /**
     * If a plugin can't be found, Smithy will by default fail the build.
     * This setting can be set to true to allow the build to progress even
     * if there is a missing plugin.
     *
     * @return Returns true if missing build plugins are allowed.
     */
    public boolean isIgnoreMissingPlugins() {
        return ignoreMissingPlugins;
    }

    /**
     * Builder used to create a {@link SmithyBuildConfig}.
     */
    public static final class Builder implements SmithyBuilder<SmithyBuildConfig> {
        private final List<String> imports = new ArrayList<>();
        private final List<String> mavenRepositories = new ArrayList<>();
        private final List<String> mavenDependencies = new ArrayList<>();
        private final Map<String, ProjectionConfig> projections = new LinkedHashMap<>();
        private final Map<String, ObjectNode> plugins = new LinkedHashMap<>();
        private String version;
        private String outputDirectory;
        private boolean ignoreMissingPlugins;

        Builder() {}

        @Override
        public SmithyBuildConfig build() {
            return new SmithyBuildConfig(this);
        }

        /**
         * Sets the builder config file version.
         *
         * @param version Version to set.
         * @return Returns the builder.
         */
        public Builder version(String version) {
            this.version = version;
            return this;
        }

        /**
         * Loads and merges the config file into the builder.
         *
         * @param config Config file to load, parse, and merge.
         * @return Returns the updated builder.
         */
        public Builder load(Path config) {
            return merge(ConfigLoader.load(config));
        }

        /**
         * Updates this configuration with the configuration of another file.
         *
         * @param config Config to update with.
         * @return Returns the builder.
         */
        public Builder merge(SmithyBuildConfig config) {
            config.getOutputDirectory().ifPresent(this::outputDirectory);
            version(config.getVersion());
            imports.addAll(config.getImports());
            mavenRepositories.addAll(config.getMavenRepositories());
            mavenDependencies.addAll(config.getMavenDependencies());
            projections.putAll(config.getProjections());
            plugins.putAll(config.getPlugins());

            // If either one wants to ignore missing plugins, then ignore them.
            if (config.isIgnoreMissingPlugins()) {
                ignoreMissingPlugins(config.ignoreMissingPlugins);
            }

            return this;
        }

        /**
         * Set a directory where the build artifacts are written.
         *
         * @param outputDirectory Directory where artifacts are written.
         * @return Returns the builder.
         */
        public Builder outputDirectory(String outputDirectory) {
            this.outputDirectory = outputDirectory;
            return this;
        }

        /**
         * Sets imports on the config.
         *
         * @param imports Imports to set.
         * @return Returns the builder.
         */
        public Builder imports(Collection<String> imports) {
            this.imports.clear();
            this.imports.addAll(imports);
            return this;
        }

        /**
         * Sets maven repositories on the config.
         *
         * @param mavenRepositories Maven repositories to set.
         * @return Returns the builder.
         */
        public Builder mavenRepositories(Collection<String> mavenRepositories) {
          this.mavenRepositories.clear();
          this.mavenRepositories.addAll(mavenRepositories);
          return this;
        }

        /**
         * Sets maven repositories on the config.
         *
         * @param mavenDependencies Maven imports to set.
         * @return Returns the builder.
         */
        public Builder mavenDependencies(Collection<String> mavenDependencies) {
          this.mavenRepositories.clear();
          this.mavenRepositories.addAll(mavenDependencies);
          return this;
        }

        /**
         * Sets projections on the config.
         *
         * @param projections Projections to set.
         * @return Returns the builder.
         */
        public Builder projections(Map<String, ProjectionConfig> projections) {
            this.projections.clear();
            this.projections.putAll(projections);
            return this;
        }

        /**
         * Sets plugins on the config.
         *
         * @param plugins Plugins to set.
         * @return Returns the builder.
         */
        public Builder plugins(Map<String, ObjectNode> plugins) {
            this.plugins.clear();
            this.plugins.putAll(plugins);
            return this;
        }

        /**
         * Logs instead of failing when a plugin can't be found by name.
         *
         * @param ignoreMissingPlugins Set to true to ignore missing plugins on the classpath.
         * @return Returns the builder.
         */
        public Builder ignoreMissingPlugins(boolean ignoreMissingPlugins) {
            this.ignoreMissingPlugins = ignoreMissingPlugins;
            return this;
        }
    }
}
