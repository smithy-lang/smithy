/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.apigateway.traits;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodeMapper;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AbstractTrait;
import software.amazon.smithy.model.traits.AbstractTraitBuilder;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Defines a map of API Gateway {@code x-amazon-apigateway-authorizer}
 * values that correspond to Smithy authorization definitions.
 *
 * <p>The key in each key-value pair of the {@code aws.apigateway#authorizers}
 * trait is an arbitrary name that's used to associate authorizer definitions
 * to operations. The {@code scheme} property of an authorizer must correspond
 * to the name of an authorization scheme of the service the trait is bound to.
 * When used to generate and OpenAPI model, the {@code aws.apigateway#authorizers}
 * trait is used to add the {@code x-amazon-apigateway-authorizer} OpenAPI
 * extension to the generated security scheme.
 *
 * @see <a href="https://docs.aws.amazon.com/apigateway/latest/developerguide/api-gateway-swagger-extensions-authorizer.html">API Gateway Authorizers</a>
 */
public final class AuthorizersTrait extends AbstractTrait implements ToSmithyBuilder<AuthorizersTrait> {
    public static final ShapeId ID = ShapeId.from("aws.apigateway#authorizers");

    private final Map<String, AuthorizerDefinition> authorizers;

    private AuthorizersTrait(Builder builder) {
        super(ID, builder.getSourceLocation());
        authorizers = MapUtils.copyOf(builder.authorizers);
    }

    public static final class Provider extends AbstractTrait.Provider {
        public Provider() {
            super(ID);
        }

        @Override
        public Trait createTrait(ShapeId target, Node value) {
            NodeMapper mapper = new NodeMapper();
            Builder builder = builder().sourceLocation(value);
            value.expectObjectNode().getMembers().forEach((key, node) -> {
                AuthorizerDefinition authorizer = mapper.deserialize(node, AuthorizerDefinition.class);
                builder.putAuthorizer(key.getValue(), authorizer);
            });
            AuthorizersTrait result = builder.build();
            result.setNodeCache(value);
            return result;
        }
    }

    /**
     * Creates a builder for the trait.
     *
     * @return Returns the created builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Gets a specific authorizer by name.
     *
     * @param name Name of the authorizer to get.
     * @return Returns the optionally found authorizer.
     */
    public Optional<AuthorizerDefinition> getAuthorizer(String name) {
        return Optional.ofNullable(authorizers.get(name));
    }

    /**
     * Gets an immutable map of authorizer names to their definitions.
     *
     * @return Returns the authorizers.
     */
    public Map<String, AuthorizerDefinition> getAuthorizers() {
        return authorizers;
    }

    @Override
    public Builder toBuilder() {
        return builder().sourceLocation(getSourceLocation()).authorizers(authorizers);
    }

    @Override
    protected Node createNode() {
        return authorizers.entrySet()
                .stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .collect(ObjectNode.collectStringKeys(Map.Entry::getKey, Map.Entry::getValue))
                .toBuilder()
                .sourceLocation(getSourceLocation())
                .build();
    }

    /**
     * Builds an {@link AuthorizersTrait}.
     */
    public static final class Builder extends AbstractTraitBuilder<AuthorizersTrait, Builder> {
        private final Map<String, AuthorizerDefinition> authorizers = new HashMap<>();

        @Override
        public AuthorizersTrait build() {
            return new AuthorizersTrait(this);
        }

        /**
         * Adds an authorizer.
         *
         * @param name Name of the authorizer to add.
         * @param authorizer Authorizer definition.
         * @return Returns the builder.
         */
        public Builder putAuthorizer(String name, AuthorizerDefinition authorizer) {
            authorizers.put(name, Objects.requireNonNull(authorizer));
            return this;
        }

        /**
         * Replaces all of the authorizers with the given map.
         *
         * @param authorizers Map of authorizer names to their definitions.
         * @return Returns the builder.
         */
        public Builder authorizers(Map<String, AuthorizerDefinition> authorizers) {
            clearAuthorizers();
            authorizers.forEach(this::putAuthorizer);
            return this;
        }

        /**
         * Removes an authorizer by name.
         *
         * @param name Name of the authorizer to remove.
         * @return Returns the builder.
         */
        public Builder removeAuthorizer(String name) {
            authorizers.remove(name);
            return this;
        }

        /**
         * Clears all of the authorizers in the builder.
         *
         * @return Returns the builder.
         */
        public Builder clearAuthorizers() {
            authorizers.clear();
            return this;
        }
    }
}
