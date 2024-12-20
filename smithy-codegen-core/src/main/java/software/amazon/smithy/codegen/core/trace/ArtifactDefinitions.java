/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core.trace;

import java.util.HashMap;
import java.util.Map;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodeMapper;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Class that defines the acceptable values that can be used in {@link ShapeLink} objects.
 */
public final class ArtifactDefinitions implements ToNode, ToSmithyBuilder<ArtifactDefinitions> {
    public static final String TYPE_TEXT = "types";
    public static final String TAGS_TEXT = "tags";

    private Map<String, String> tags;
    private Map<String, String> types;

    private ArtifactDefinitions(Builder builder) {
        if (builder.tags.isEmpty()) {
            throw new IllegalStateException("ArtifactDefinition's Tags field must not be empty.");
        }
        if (builder.types.isEmpty()) {
            throw new IllegalStateException("ArtifactDefinition's Types field must not be empty.");
        }
        tags = MapUtils.copyOf(builder.tags);
        types = MapUtils.copyOf(builder.types);
    }

    /**
     * Converts an ObjectNode that represents the definitions section of the
     * trace file into a types maps and tags map.
     *
     * @param value ObjectNode that contains the JSON data inside the definitions tag of
     *              the trace file
     * @return an ArtifactDefinitions object created from the ObjectNode.
     */
    public static ArtifactDefinitions fromNode(Node value) {
        NodeMapper mapper = new NodeMapper();
        mapper.disableFromNodeForClass(ArtifactDefinitions.class);
        return mapper.deserialize(value, ArtifactDefinitions.class);
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Converts the types and tags Maps into a single ObjectNode.
     *
     * @return an ObjectNode that contains two ObjectNode children; one contains the tag map structure the
     * other contains the type map structure.
     */
    @Override
    public ObjectNode toNode() {
        return ObjectNode.objectNodeBuilder()
                .withMember(TAGS_TEXT, ObjectNode.fromStringMap(tags))
                .withMember(TYPE_TEXT, ObjectNode.fromStringMap(types))
                .build();
    }

    /**
     * Gets this Definition's Tags Map.
     * The tags {@link Map} defines the set of tags that can be used in a {@link ShapeLink} object.
     * Each key is the name of the tag, and each value is the description of the tag.
     * Tags are used to provide semantics to links. Tools that evaluate trace models
     * use these tags to inform their analysis. For example, a tag for an AWS SDK code
     * generator could be "requestBuilder" to indicate that a class is used as a builder for a request.
     *
     * @return this Definition's Tags Map
     */
    public Map<String, String> getTags() {
        return tags;
    }

    /**
     * Gets this Definition's Types Map.
     * The types {@link Map} defines the set of types that can be used in a {@link ShapeLink} object.
     * Each key is the name of the type, and each value is a description of the type.
     * For programming languages, these types represent language-specific components.
     * For example, in Java, types should map to the possible values of {@link java.lang.annotation.ElementType}.
     *
     * @return this Definition's Type's Map
     */
    public Map<String, String> getTypes() {
        return types;
    }

    /**
     * Take this object and create a builder that contains all of the
     * current property values of this object.
     *
     * @return a builder for type T
     */
    @Override
    public Builder toBuilder() {
        return builder()
                .tags(tags)
                .types(types);
    }

    public static final class Builder implements SmithyBuilder<ArtifactDefinitions> {
        private final Map<String, String> tags = new HashMap<>();
        private final Map<String, String> types = new HashMap<>();

        /**
         * @return Definitions object from this builder.
         */
        public ArtifactDefinitions build() {
            return new ArtifactDefinitions(this);
        }

        public Builder tags(Map<String, String> tags) {
            this.tags.clear();
            this.tags.putAll(tags);
            return this;
        }

        public Builder types(Map<String, String> types) {
            this.types.clear();
            this.types.putAll(types);
            return this;
        }

        /**
         * Adds the tag's key, value pair to the tags map.
         *
         * @param name        Name of tag.
         * @param description Description of tag.
         * @return This builder.
         */
        public Builder addTag(String name, String description) {
            this.tags.put(name, description);
            return this;
        }

        /**
         * Adds the type's key, value pair to the tags map.
         *
         * @param name        Key of type.
         * @param description Value of type.
         * @return This builder.
         */
        public Builder addType(String name, String description) {
            this.types.put(name, description);
            return this;
        }

    }

}
