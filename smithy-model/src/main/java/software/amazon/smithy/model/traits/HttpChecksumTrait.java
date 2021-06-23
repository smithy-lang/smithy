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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.ExpectationNotMetException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Indicates that an operation supports checksum validation.
 * Contains request and response members that define the checksum behavior
 * for operations HTTP Request and HTTP Response respectively.
 */
public final class HttpChecksumTrait extends AbstractTrait implements ToSmithyBuilder<HttpChecksumTrait> {
    public static final ShapeId ID = ShapeId.from("smithy.api#httpChecksum");

    private static final String REQUEST_PROPERTY = "request";
    private static final String RESPONSE_PROPERTY = "response";

    private final List<HttpChecksumProperty> requestProperties;
    private final List<HttpChecksumProperty> responseProperties;

    private HttpChecksumTrait(HttpChecksumTrait.Builder builder) {
        super(ID, builder.sourceLocation);
        this.requestProperties = ListUtils.copyOf(builder.requestProperties);
        this.responseProperties = ListUtils.copyOf(builder.responseProperties);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public SmithyBuilder<HttpChecksumTrait> toBuilder() {
        return new Builder()
                .sourceLocation(getSourceLocation())
                .requestProperties(getRequestProperties())
                .responseProperties(getResponseProperties());
    }

    /**
     * Gets request property defined within the HttpChecksum trait.
     *
     * @return Returns checksum properties for request.
     */
    public List<HttpChecksumProperty> getRequestProperties() {
        return requestProperties;
    }

    /**
     * Gets supported algorithms for request property defined within the
     * HttpChecksum trait.
     *
     * @return Returns all supported algorithms for request.
     */
    public Set<String> getRequestAlgorithms() {
        Set<String> set = new LinkedHashSet<>();
        requestProperties.forEach(p -> set.add(p.getAlgorithm()));
        return set;
    }

    /**
     * Gets checksum properties associated with the provided algorithm on request.
     * Returns an empty list if algorithm is not supported on request.
     *
     * @param algorithm the algorithm on request property.
     * @return Returns an ordered list of HttpChecksumProperties.
     */
    public List<HttpChecksumProperty> getRequestPropertiesForAlgorithm(String algorithm) {
        List<HttpChecksumProperty> list = new ArrayList<>();
        for (HttpChecksumProperty property : requestProperties) {
            if (property.getAlgorithm().equalsIgnoreCase(algorithm)) {
                list.add(property);
            }
        }

        return list;
    }

    /**
     * Gets response property defined within the HttpChecksum trait.
     *
     * @return Returns checksum properties for response.
     */
    public List<HttpChecksumProperty> getResponseProperties() {
        return responseProperties;
    }

    /**
     * Gets supported algorithms for response property defined within the
     * HttpChecksum trait.
     *
     * @return Returns all supported algorithms for response.
     */
    public Set<String> getResponseAlgorithms() {
        Set<String> set = new LinkedHashSet<>();
        responseProperties.forEach(p -> set.add(p.getAlgorithm()));
        return set;
    }

    /**
     * Gets checksum properties associated with the provided algorithm on response.
     * Returns an empty list if algorithm is not supported on response.
     *
     * @param algorithm the supported algorithm on response.
     * @return Returns an ordered list of HttpChecksumProperties.
     */
    public List<HttpChecksumProperty> getResponsePropertiesForAlgorithm(String algorithm) {
        List<HttpChecksumProperty> list = new ArrayList<>();
        for (HttpChecksumProperty property : responseProperties) {
            if (property.getAlgorithm().equalsIgnoreCase(algorithm)) {
                list.add(property);
            }
        }

        return list;
    }

    @Override
    protected Node createNode() {
        ObjectNode.Builder builder = ObjectNode.objectNodeBuilder();
        builder.sourceLocation(getSourceLocation());

        if (!requestProperties.isEmpty()) {
            builder.withMember(REQUEST_PROPERTY, getRequestProperties().stream().map(HttpChecksumProperty::toNode)
                    .collect(ArrayNode.collect()));
        }

        if (!responseProperties.isEmpty()) {
            builder.withMember(RESPONSE_PROPERTY, getResponseProperties().stream().map(HttpChecksumProperty::toNode)
                    .collect(ArrayNode.collect()));
        }
        return builder.build();
    }

    public static final class Builder extends AbstractTraitBuilder<HttpChecksumTrait, Builder> {
        private Set<HttpChecksumProperty> requestProperties;
        private Set<HttpChecksumProperty> responseProperties;

        private Builder() {
            requestProperties = new LinkedHashSet<>();
            responseProperties = new LinkedHashSet<>();
        }

        @Override
        public HttpChecksumTrait build() {
            return new HttpChecksumTrait(this);
        }

        public Builder requestProperties(List<HttpChecksumProperty> properties) {
            clearRequestProperties();
            properties.forEach(this::addRequestProperty);
            return this;
        }

        public Builder addRequestProperty(HttpChecksumProperty property) {
            if (requestProperties.contains(property)) {
                throw new ExpectationNotMetException(
                        String.format("Found duplicate request property entry for algorithm %s at location %s within"
                                + " the HttpChecksum trait.", property.getAlgorithm(), property.getLocation()),
                        this.sourceLocation);
            }

            this.requestProperties.add(property);
            return this;
        }

        public Builder clearRequestProperties() {
            this.requestProperties.clear();
            return this;
        }

        public Builder responseProperties(List<HttpChecksumProperty> properties) {
            clearResponseProperties();
            properties.forEach(this::addResponseProperty);
            return this;
        }

        public Builder addResponseProperty(HttpChecksumProperty property) {
            if (responseProperties.contains(property)) {
                throw new ExpectationNotMetException(
                        String.format("Found duplicate response property entry for algorithm %s at location %s within"
                                + " the HttpChecksum trait.", property.getAlgorithm(), property.getLocation()),
                        this.sourceLocation);
            }

            this.responseProperties.add(property);
            return this;
        }

        public Builder clearResponseProperties() {
            this.responseProperties.clear();
            return this;
        }
    }

    public static final class Provider extends AbstractTrait.Provider {
        public Provider() {
            super(ID);
        }

        @Override
        public Trait createTrait(ShapeId target, Node value) {
            ObjectNode objectNode = value.expectObjectNode();
            Builder builder = builder().sourceLocation(value);

            Optional<ArrayNode> requestNodes = objectNode.getArrayMember(REQUEST_PROPERTY);
            if (requestNodes.isPresent()) {
                List<HttpChecksumProperty> properties = requestNodes.get()
                        .getElementsAs(HttpChecksumProperty::fromNode);
                for (HttpChecksumProperty property : properties) {
                    builder.addRequestProperty(property);
                }
            }

            Optional<ArrayNode> responseNodes = objectNode.getArrayMember(RESPONSE_PROPERTY);
            if (responseNodes.isPresent()) {
                List<HttpChecksumProperty> properties = responseNodes.get()
                        .getElementsAs(HttpChecksumProperty::fromNode);
                for (HttpChecksumProperty property : properties) {
                    builder.addResponseProperty(property);
                }
            }
            return builder.build();
        }
    }
}
