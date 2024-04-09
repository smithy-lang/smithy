/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import software.amazon.smithy.build.SmithyBuildException;
import software.amazon.smithy.model.loader.ModelSyntaxException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.utils.BuilderRef;
import software.amazon.smithy.utils.IoUtils;
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
    private final List<String> sources;
    private final List<String> imports;
    private final String outputDirectory;
    private final Map<String, ProjectionConfig> projections;
    private final Map<String, ObjectNode> plugins;
    private final boolean ignoreMissingPlugins;
    private final MavenConfig maven;
    private final long lastModifiedInMillis;

    private SmithyBuildConfig(Builder builder) {
        SmithyBuilder.requiredState("version", builder.version);
        version = builder.version;
        outputDirectory = builder.outputDirectory;
        sources = builder.sources.copy();
        imports = builder.imports.copy();
        projections = builder.projections.copy();
        plugins = builder.plugins.copy();
        ignoreMissingPlugins = builder.ignoreMissingPlugins;
        maven = builder.maven;
        lastModifiedInMillis = builder.lastModifiedInMillis;

        if (outputDirectory != null && outputDirectory.isEmpty()) {
            throw new IllegalArgumentException("outputDirectory must be set to a valid directory");
        }
    }

    public static SmithyBuildConfig fromNode(Node node) {
        Path path = SmithyBuildUtils.getBasePathFromSourceLocation(node);
        // Expand variables before deserializing the node into the builder.
        ObjectNode expanded = SmithyBuildUtils.expandNode(node);
        return builder().loadNode(path, expanded).build();
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
     * <code>
     * {
     *     "version": "1.0",
     *     "sources": ["model"],
     *     "imports": ["foo.json", "baz.json"],
     *     "outputDirectory": "build/output",
     *     "maven" {
     *          "dependencies": ["software.amazon.smithy:smithy-aws-traits:1.26.1"],
     *          "repositories": [
     *              {
     *                  "url": "https://example.com/maven",
     *                  "httpCredentials": "${MAVEN_USER}:${MAVEN_PASSWORD}"
     *              }
     *          ]
     *     }
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
                .sources(sources)
                .imports(imports)
                .projections(projections)
                .plugins(plugins)
                .ignoreMissingPlugins(ignoreMissingPlugins)
                .maven(maven);
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
     * Gets the paths to all the models to import.
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
     * Gets the paths to all model sources.
     *
     * <p>Paths can point to individual model files or directories.
     * All models stored in all recursive directories will be imported.
     * Each found Smithy model will be considered a source model.
     *
     * @return Gets the list of models to import.
     */
    public List<String> getSources() {
        return sources;
    }

    /**
     * @return Gets the optional output directory to store artifacts.
     */
    public Optional<String> getOutputDirectory() {
        return Optional.ofNullable(outputDirectory);
    }

    /**
     * Gets all the configured projections.
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
     * Gets Maven dependency configuration.
     *
     * <p>Note that smithy-build does not directly resolve or use dependencies.
     * It's up to other packages like the Smithy CLI to use a dependency resolver
     * based on smithy-build.json configuration and call smithy-build with
     * the appropriate classpath.
     *
     * @return Returns Maven dependency information.
     */
    public Optional<MavenConfig> getMaven() {
        return Optional.ofNullable(maven);
    }

    /**
     * Get the last modified time of the configuration file.
     *
     * @return Returns the last modified time in milliseconds since the epoch.
     */
    public long getLastModifiedInMillis() {
        return lastModifiedInMillis;
    }

    /**
     * Builder used to create a {@link SmithyBuildConfig}.
     */
    public static final class Builder implements SmithyBuilder<SmithyBuildConfig> {
        private final BuilderRef<List<String>> imports = BuilderRef.forList();
        private final BuilderRef<List<String>> sources = BuilderRef.forList();
        private final BuilderRef<Map<String, ProjectionConfig>> projections = BuilderRef.forOrderedMap();
        private final BuilderRef<Map<String, ObjectNode>> plugins = BuilderRef.forOrderedMap();
        private String version;
        private String outputDirectory;
        private boolean ignoreMissingPlugins;
        private MavenConfig maven;
        private long lastModifiedInMillis = 0;

        Builder() {}

        @Override
        public SmithyBuildConfig build() {
            // Add built-in plugins. This is done here to ensure that they cannot be unset.
            for (String builtin : BUILTIN_PLUGINS) {
                plugins.get().put(builtin, Node.objectNode());
            }

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
            try {
                String content = IoUtils.readUtf8File(config);
                Path basePath = config.getParent();
                if (basePath == null) {
                    basePath = SmithyBuildUtils.getCurrentWorkingDirectory();
                }
                Node loadedAndExpanded = SmithyBuildUtils.loadAndExpandJson(config.toString(), content);
                return loadNode(basePath, loadedAndExpanded);
            } catch (ModelSyntaxException e) {
                throw new SmithyBuildException(e);
            }
        }

        private Builder loadNode(Path basePath, Node node) {
            node.expectObjectNode()
                    .expectStringMember("version", this::version)
                    .getStringMember("outputDirectory", this::outputDirectory)
                    .getArrayMember("sources", s -> SmithyBuildUtils.resolveImportPath(basePath, s),
                                    values -> sources.get().addAll(values))
                    .getArrayMember("imports", s -> SmithyBuildUtils.resolveImportPath(basePath, s),
                                    values -> imports.get().addAll(values))
                    .getObjectMember("projections", v -> {
                        for (Map.Entry<String, Node> entry : v.getStringMap().entrySet()) {
                            projections.get().put(entry.getKey(), ProjectionConfig
                                    .fromNode(entry.getValue(), basePath));
                        }
                    })
                    .getObjectMember("plugins", v -> {
                        for (Map.Entry<String, Node> entry : v.getStringMap().entrySet()) {
                            plugins.get().put(entry.getKey(), entry.getValue().expectObjectNode());
                        }
                    })
                    .getBooleanMember("ignoreMissingPlugins", this::ignoreMissingPlugins)
                    .getMember("maven", MavenConfig::fromNode, this::maven);
            return this;
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
            sources.get().addAll(config.getSources());
            imports.get().addAll(config.getImports());
            projections.get().putAll(config.getProjections());
            plugins.get().putAll(config.getPlugins());

            if (config.getMaven().isPresent()) {
                if (maven == null) {
                    maven = config.maven;
                } else {
                    maven = maven.merge(config.maven);
                }
            }

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
         * Replaces imports on the config.
         *
         * @param imports Imports to set.
         * @return Returns the builder.
         */
        public Builder imports(Collection<String> imports) {
            this.imports.clear();
            this.imports.get().addAll(imports);
            return this;
        }

        /**
         * Replaces sources on the config.
         *
         * @param sources Sources to set.
         * @return Returns the builder.
         */
        public Builder sources(Collection<String> sources) {
            this.sources.clear();
            this.sources.get().addAll(sources);
            return this;
        }

        /**
         * Replaces projections on the config.
         *
         * @param projections Projections to set.
         * @return Returns the builder.
         */
        public Builder projections(Map<String, ProjectionConfig> projections) {
            this.projections.clear();
            this.projections.get().putAll(projections);
            return this;
        }

        /**
         * Replaces plugins on the config.
         *
         * @param plugins Plugins to set.
         * @return Returns the builder.
         */
        public Builder plugins(Map<String, ObjectNode> plugins) {
            this.plugins.clear();
            this.plugins.get().putAll(plugins);
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

        public Builder maven(MavenConfig maven) {
            this.maven = maven;
            return this;
        }

        public Builder lastModifiedInMillis(long lastModifiedInMillis) {
            this.lastModifiedInMillis = lastModifiedInMillis;
            return this;
        }
    }
}
