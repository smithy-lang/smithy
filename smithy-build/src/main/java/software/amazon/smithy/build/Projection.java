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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import software.amazon.smithy.model.SmithyBuilder;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.ToNode;

/**
 * Projection stored in a {@link SmithyBuildConfig}.
 */
public final class Projection implements ToNode {
    private final boolean isAbstract;
    private final String name;
    private final List<String> imports;
    private final List<TransformConfiguration> transforms;
    private final Map<String, ObjectNode> plugins;

    private Projection(Builder builder) {
        if (builder.name == null) {
            throw new SmithyBuildException("No name was provided for projection");
        }

        if (!SmithyBuildConfig.PATTERN.matcher(builder.name).find()) {
            throw new SmithyBuildException("Invalid projection name: `" + builder.name + "`. Projection names must "
                                           + "match the following pattern: " + SmithyBuildConfig.PATTERN.pattern());
        }

        this.name = builder.name;
        this.imports = List.copyOf(builder.imports);
        this.transforms = List.copyOf(builder.transforms);
        this.isAbstract = builder.isAbstract;
        this.plugins = Map.copyOf(builder.plugins);

        if (isAbstract && (!plugins.isEmpty() || !imports.isEmpty())) {
            throw new SmithyBuildException(String.format(
                    "Invalid abstract projection: `%s` Abstract projections must not define plugins or imports",
                    builder.name));
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return The name of the projection.
     */
    public String getName() {
        return name;
    }

    /**
     * @return Gets the immutable transforms in the projection.
     */
    public List<TransformConfiguration> getTransforms() {
        return transforms;
    }

    /**
     * @return Gets the immutable plugins of the projection.
     */
    public Map<String, ObjectNode> getPlugins() {
        return plugins;
    }

    /**
     * @return Returns true if the projection is abstract.
     */
    public boolean isAbstract() {
        return isAbstract;
    }

    /**
     * Gets the imports configured for the projection.
     *
     * @return Returns the projection-specific imports.
     */
    public List<String> getImports() {
        return imports;
    }

    @Override
    public Node toNode() {
        var result = Node.objectNodeBuilder();
        if (isAbstract) {
            result.withMember("abstract", Node.from(true));
        }

        if (!imports.isEmpty()) {
            result.withMember("imports", imports.stream().map(Node::from).collect(ArrayNode.collect()));
        }

        return result
                .withMember("name", Node.from(getName()))
                .withMember("transforms", createTransformerNode(getTransforms()))
                .withMember("plugins", getPlugins().entrySet().stream()
                        .collect(ObjectNode.collectStringKeys(Map.Entry::getKey, Map.Entry::getValue)))
                .build();
    }

    private static Node createTransformerNode(List<TransformConfiguration> values) {
        return values.stream().map(TransformConfiguration::toNode).collect(ArrayNode.collect());
    }

    /**
     * Builds a {@link Projection}.
     */
    public static final class Builder implements SmithyBuilder<Projection> {
        private String name;
        private boolean isAbstract;
        private final List<String> imports = new ArrayList<>();
        private final List<TransformConfiguration> transforms = new ArrayList<>();
        private final Map<String, ObjectNode> plugins = new HashMap<>();

        private Builder() {}

        /**
         * Builds the projection.
         *
         * @return Returns the created projection.
         */
        public Projection build() {
            return new Projection(this);
        }

        /**
         * Sets the <strong>required</strong> projection name.
         *
         * @param name Name of the projection.
         * @return Returns the builder.
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the {@code abstract} property of the projection.
         *
         * <p>Abstract projections do not directly create any artifacts.
         *
         * @param isAbstract Set to true to mark as abstract.
         * @return Returns the builder.
         */
        public Builder isAbstract(boolean isAbstract) {
            this.isAbstract = isAbstract;
            return this;
        }

        /**
         * Adds a single path to import.
         *
         * @param importPath Path to a model file or directory to import.
         * @return Returns the builder.
         */
        public Builder addImport(String importPath) {
            this.imports.add(importPath);
            return this;
        }

        /**
         * Adds a transform to the projection.
         *
         * @param transform Transform to add.
         * @return Returns the builder.
         */
        public Builder addTransform(TransformConfiguration transform) {
            transforms.add(transform);
            return this;
        }

        /**
         * Adds a plugin to the projection.
         *
         * @param name Name of the plugin.
         * @param settings Settings of the plugin.
         * @return Returns the builder.
         */
        public Builder addPlugin(String name, ObjectNode settings) {
            plugins.put(Objects.requireNonNull(name), Objects.requireNonNull(settings));
            return this;
        }
    }
}
