/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.apigateway.traits;

import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AbstractTrait;
import software.amazon.smithy.model.traits.AbstractTraitBuilder;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.utils.BuilderRef;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Defines custom gateway responses for an API Gateway REST API. Gateway
 * responses customize error responses for authentication failures,
 * integration errors, and other API Gateway-generated errors.
 *
 * @see <a href="https://docs.aws.amazon.com/apigateway/latest/developerguide/api-gateway-swagger-extensions-gateway-responses.html">x-amazon-apigateway-gateway-responses</a>
 */
public final class GatewayResponsesTrait extends AbstractTrait implements ToSmithyBuilder<GatewayResponsesTrait> {
    public static final ShapeId ID = ShapeId.from("aws.apigateway#gatewayResponses");

    private final Map<String, GatewayResponse> responses;

    private GatewayResponsesTrait(Builder builder) {
        super(ID, builder.getSourceLocation());
        responses = builder.responses.copy();
    }

    /**
     * Creates a builder for the trait.
     *
     * @return Returns the created builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Provider extends AbstractTrait.Provider {
        public Provider() {
            super(ID);
        }

        @Override
        public Trait createTrait(ShapeId target, Node value) {
            Builder builder = builder().sourceLocation(value);
            for (Map.Entry<StringNode, Node> entry : value.expectObjectNode().getMembers().entrySet()) {
                GatewayResponse response = GatewayResponse.fromNode(entry.getValue());
                builder.putResponse(entry.getKey().getValue(), response);
            }
            GatewayResponsesTrait result = builder.build();
            result.setNodeCache(value);
            return result;
        }
    }

    /**
     * Gets all gateway responses.
     *
     * @return Returns the immutable map of response type keys to definitions.
     */
    public Map<String, GatewayResponse> getResponses() {
        return responses;
    }

    /**
     * Gets a specific gateway response by type key.
     *
     * @param type Response type key (e.g., {@code DEFAULT_4XX}).
     * @return Returns the optionally found gateway response.
     */
    public Optional<GatewayResponse> getResponse(String type) {
        return Optional.ofNullable(responses.get(type));
    }

    /**
     * Gets the gateway responses as an ObjectNode.
     *
     * @return Returns the gateway responses map as a Node.
     */
    public ObjectNode getValue() {
        return createNode().expectObjectNode();
    }

    @Override
    public Builder toBuilder() {
        Builder builder = builder().sourceLocation(getSourceLocation());
        responses.forEach(builder::putResponse);
        return builder;
    }

    @Override
    protected Node createNode() {
        return responses.entrySet()
                .stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .collect(ObjectNode.collectStringKeys(Map.Entry::getKey, e -> e.getValue().toNode()));
    }

    /**
     * Builds a {@link GatewayResponsesTrait}.
     */
    public static final class Builder extends AbstractTraitBuilder<GatewayResponsesTrait, Builder> {
        private final BuilderRef<Map<String, GatewayResponse>> responses = BuilderRef.forOrderedMap();

        @Override
        public GatewayResponsesTrait build() {
            return new GatewayResponsesTrait(this);
        }

        /**
         * Adds a gateway response.
         *
         * @param type Response type key (e.g., {@code DEFAULT_4XX}).
         * @param response Gateway response definition.
         * @return Returns the builder.
         */
        public Builder putResponse(String type, GatewayResponse response) {
            responses.get().put(type, Objects.requireNonNull(response));
            return this;
        }

        /**
         * Removes a gateway response by type key.
         *
         * @param type Response type key to remove.
         * @return Returns the builder.
         */
        public Builder removeResponse(String type) {
            responses.get().remove(type);
            return this;
        }
    }
}
