/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.model.traits;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.smithy.model.node.ExpectationNotMetException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.SetUtils;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Indicates that an operation supports checksum validation.
 * Contains request and response members that define the checksum behavior
 * for operations HTTP Request and HTTP Response respectively.
 */
public final class HttpChecksumTrait extends AbstractTrait implements ToSmithyBuilder<HttpChecksumTrait> {
    public static final ShapeId ID = ShapeId.from("smithy.api#httpChecksum");

    private static final String REQUEST_PROPERTY = "Request";
    private static final String RESPONSE_PROPERTY = "Response";

    private final HttpChecksumProperties requestProperty;
    private final HttpChecksumProperties responseProperty;

    private HttpChecksumTrait(HttpChecksumTrait.Builder builder) {
        super(ID, builder.sourceLocation);
        this.requestProperty = builder.requestProperty;
        this.responseProperty = builder.responseProperty;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public SmithyBuilder<HttpChecksumTrait> toBuilder() {
        return new Builder()
                .sourceLocation(getSourceLocation())
                .requestProperty(requestProperty)
                .responseProperty(responseProperty);
    }

    /**
     * Gets request property defined within the HttpChecksum trait.
     *
     * @return checksum properties for request.
     */
    public HttpChecksumProperties getRequestProperty() {
        return requestProperty;
    }

    /**
     * Gets response property defined within the HttpChecksum trait.
     *
     * @return checksum properties for response.
     */
    public HttpChecksumProperties getResponseProperty() {
        return responseProperty;
    }

    @Override
    protected Node createNode() {
        ObjectNode.Builder builder = ObjectNode.objectNodeBuilder();
        builder.sourceLocation(getSourceLocation());

        if (requestProperty != null) {
            builder.withMember(REQUEST_PROPERTY, requestProperty.toNode());
        }
        if (responseProperty != null) {
            builder.withMember(RESPONSE_PROPERTY, responseProperty.toNode());
        }
        return builder.build();
    }


    public static final class Builder extends AbstractTraitBuilder<HttpChecksumTrait, Builder> {
        private HttpChecksumProperties requestProperty;
        private HttpChecksumProperties responseProperty;

        private Builder() {
        }

        @Override
        public HttpChecksumTrait build() {
            return new HttpChecksumTrait(this);
        }

        public Builder requestProperty(HttpChecksumProperties requestProperty) {
            this.requestProperty = requestProperty;
            return this;
        }

        public Builder responseProperty(HttpChecksumProperties responseProperty) {
            this.responseProperty = responseProperty;
            return this;
        }
    }

    public static final class Provider implements TraitService {
        @Override
        public ShapeId getShapeId() {
            return ID;
        }

        public Trait createTrait(ShapeId target, Node value) {
            ObjectNode node = value.expectObjectNode();
            Builder builder = builder().sourceLocation(value);

            Optional<ObjectNode> requestNode = node.getObjectMember(REQUEST_PROPERTY);
            if (requestNode.isPresent()) {
                builder.requestProperty = HttpChecksumProperties.fromNode(requestNode.get());
            }

            Optional<ObjectNode> responseNode = node.getObjectMember(RESPONSE_PROPERTY);
            if (responseNode.isPresent()) {
                builder.responseProperty = HttpChecksumProperties.fromNode(responseNode.get());
            }

            return builder.build();
        }
    }


    /**
     * Defines checksum properties for data checksum validation.
     * These properties are used by the members defined for HTTPChecksum trait.
     */
    public static final class HttpChecksumProperties implements ToNode, ToSmithyBuilder<HttpChecksumProperties> {

        private static final String PREFIX = "prefix";
        private static final String ALGORITHMS = "algorithms";
        private static final String LOCATION = "location";
        private static final String HEADER = "header";
        private static final Set<String> KEYS = SetUtils.of(
                PREFIX, ALGORITHMS, LOCATION);

        private final String prefix;
        private final Set<String> algorithms;
        private final String location;

        private HttpChecksumProperties(HttpChecksumProperties.Builder builder) {
            this.prefix = builder.prefix;
            this.algorithms = builder.algorithms;
            this.location = builder.location;
        }

        public static HttpChecksumProperties.Builder builder() {
            return new Builder();
        }

        /**
         * Create a {@code HttpChecksumProperties} from {@link Node}.
         *
         * @param node {@code Node} to create the {@code HttpChecksumProperties}
         * @return Returns the created {@code HttpChecksumProperties}.
         * @throws ExpectationNotMetException if the given {@code node} is invalid.
         */
        public static HttpChecksumProperties fromNode(Node node) {
            ObjectNode value = node.expectObjectNode().warnIfAdditionalProperties(KEYS);
            HttpChecksumProperties.Builder builder = builder();
            value.getStringMember(PREFIX).map(StringNode::getValue).ifPresent(builder::prefix);
            value.getMember(ALGORITHMS).ifPresent(
                    algorithms -> builder.algorithms(Node.loadArrayOfString(ALGORITHMS, algorithms).stream().collect(
                            Collectors.toSet())));

            Optional<StringNode> locationNode = value.getStringMember(LOCATION);
            if (!locationNode.isPresent()) {
                // set default value for location as HEADER.
                builder.location(HEADER);
            } else {
                String location = locationNode.get().getValue();
                builder.location(location);
            }

            return builder.build();
        }

        /**
         * @return location member as string. By default, location is `"header"`.
         */
        public String getLocation() {
            return location;
        }

        /**
         * @return prefix member as string.
         */
        public String getPrefix() {
            return prefix;
        }

        /**
         * @return set of string denoting the supported checksum algorithms.
         */
        public Set<String> getAlgorithms() {
            return algorithms;
        }

        /**
         * Take this object and create a builder that contains all of the
         * current property values of this object.
         *
         * @return a builder for type HttpChecksumProperties
         */
        @Override
        public SmithyBuilder<HttpChecksumProperties> toBuilder() {
            return builder()
                    .prefix(getPrefix())
                    .algorithms(getAlgorithms())
                    .location(getLocation());
        }

        /**
         * Converts a value to a {@link Node}.
         *
         * @return Returns the creates Node.
         */
        @Override
        public Node toNode() {
            ObjectNode.Builder builder = Node.objectNodeBuilder()
                    .withMember(PREFIX, getPrefix())
                    .withMember(ALGORITHMS, Node.fromStrings(algorithms))
                    .withMember(LOCATION, getLocation());

            return builder.build();
        }

        public static final class Builder implements SmithyBuilder<HttpChecksumProperties> {
            private String prefix;
            private String location;
            private Set<String> algorithms = new HashSet<>();

            private Builder() {
            }

            @Override
            public HttpChecksumProperties build() {
                return new HttpChecksumProperties(this);
            }

            public Builder prefix(String prefix) {
                this.prefix = prefix;
                return this;
            }

            public Builder location(String location) {
                this.location = location;
                return this;
            }

            public Builder algorithms(Set<String> algorithms) {
                this.algorithms.clear();
                algorithms.forEach(this::addAlgorithm);
                return this;
            }

            public Builder addAlgorithm(String algorithm) {
                this.algorithms.add(algorithm);
                return this;
            }
        }
    }
}
