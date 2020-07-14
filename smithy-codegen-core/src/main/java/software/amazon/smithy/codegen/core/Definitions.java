/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.codegen.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;
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
 * Class that defines the acceptable values that can be used in ShapeLink objects.
 * <p>
 * The types {@link Map} defines the set of types that can be used in a {@link ShapeLink} object.
 * Each key is the name of the type, and each value is a description of the type.
 * For programming languages, these types represent language-specific components.
 * For example, in Java, types should map to the possible values of {@link java.lang.annotation.ElementType}.
 * </p>
 * <p>
 * The tags {@link Map} defines the set of tags that can be used in a {@link ShapeLink} object.
 * Each key is the name of the tag, and each value is the description of the tag.
 * Tags are used to provide semantics to links. Tools that evaluate trace models
 * use these tags to inform their analysis. For example, a tag for an AWS SDK code
 * generator could be "requestBuilder" to indicate that a class is used as a builder for a request.
 * </p>
 */
public final class Definitions implements ToNode, ToSmithyBuilder<Definitions> {
    public static final String TYPE_TEXT = "types";
    public static final String TAGS_TEXT = "tags";

    private Map<String, String> tags;
    private Map<String, String> types;

    private Definitions(Builder builder) {
        tags = SmithyBuilder.requiredState(TAGS_TEXT, MapUtils.copyOf(builder.tags));
        types = SmithyBuilder.requiredState(TYPE_TEXT, MapUtils.copyOf(builder.types));
    }

    /**
     * Converts an ObjectNode that represents the definitions section of the
     * trace file into a types maps and tags map.
     *
     * @param value ObjectNode that contains the JSON data inside the definitions tag of
     *              the trace file
     */
    public static Definitions createFromNode(Node value) {
        return new NodeMapper().deserialize(value, Definitions.class);
    }

    /**
     * Parses a definitions file and converts it into a definitions object. This is useful
     * in the scenario when the user is provided the definitions specification and must create
     * the trace file from that definitions specification.
     *
     * @param filename a definitions file URI - see example
     * @return Definitions corresponding to parsed URI
     * @throws FileNotFoundException if the definitions file path is not found
     */
    public static Definitions createFromFile(URI filename) throws FileNotFoundException {
        InputStream stream = new FileInputStream(new File(filename));
        return createFromNode(Node.parse(stream).expectObjectNode());
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
     *
     * @return this Definition's Tags Map
     */
    public Map<String, String> getTags() {
        return tags;
    }

    /**
     * Gets this Definition's Types Map.
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
    public SmithyBuilder<Definitions> toBuilder() {
        return builder()
                .tags(tags)
                .types(types);
    }

    public static final class Builder implements SmithyBuilder<Definitions> {
        private Map<String, String> tags;
        private Map<String, String> types;

        /**
         * @return Definitions object from this builder.
         */
        public Definitions build() {
            return new Definitions(this);
        }

        public Builder tags(Map<String, String> tags) {
            this.tags = tags;
            return this;
        }

        public Builder types(Map<String, String> types) {
            this.types = types;
            return this;
        }

        /**
         * Adds the tag's key, value pair to the tags map.
         *
         * @param key   Key of tag.
         * @param value Value of tag.
         * @return This builder.
         */
        public Builder addTag(String key, String value) {
            if (this.tags == null) {
                this.tags = new HashMap<>();
            }
            this.tags.put(key, value);
            return this;
        }

        /**
         * Adds the type's key, value pair to the tags map.
         *
         * @param key   Key of type.
         * @param value Value of type.
         * @return This builder.
         */
        public Builder addType(String key, String value) {
            if (this.types == null) {
                this.types = new HashMap<>();
            }
            this.types.put(key, value);
            return this;
        }

    }

}
