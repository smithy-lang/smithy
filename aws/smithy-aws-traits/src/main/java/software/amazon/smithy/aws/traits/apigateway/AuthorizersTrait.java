package software.amazon.smithy.aws.traits.apigateway;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import software.amazon.smithy.model.ToSmithyBuilder;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AbstractTrait;
import software.amazon.smithy.model.traits.AbstractTraitBuilder;
import software.amazon.smithy.model.traits.Trait;

/**
 * Defines a map of API Gateway {@code x-amazon-apigateway-authorizer}
 * values that correspond to Smithy authorization definitions.
 *
 * <p>The key in each key-value pair of the {@code aws.apigateway#authorizers}
 * trait must correspond to the name of an authorization scheme of the
 * service the trait is bound to. When used to generate and OpenAPI model,
 * the {@code aws.apigateway#authorizers} trait is used to add the
 * {@code x-amazon-apigateway-authorizer} OpenAPI extension to the
 * generated security scheme.
 *
 * @see <a href="https://docs.aws.amazon.com/apigateway/latest/developerguide/api-gateway-swagger-extensions-authorizer.html">API Gateway Authorizers</a>
 */
public final class AuthorizersTrait extends AbstractTrait implements ToSmithyBuilder<AuthorizersTrait> {
    public static final String TRAIT = "aws.apigateway#authorizers";

    private final Map<String, Authorizer> authorizers;

    private AuthorizersTrait(Builder builder) {
        super(TRAIT, builder.getSourceLocation());
        authorizers = Map.copyOf(builder.authorizers);
    }

    public static final class Provider extends AbstractTrait.Provider {
        public Provider() {
            super(TRAIT);
        }

        @Override
        public Trait createTrait(ShapeId target, Node value) {
            Builder builder = builder().sourceLocation(value);
            value.expectObjectNode().getMembers().forEach((key, node) -> {
                var authorizer = Authorizer.fromNode(node.expectObjectNode());
                builder.putAuthorizer(key.getValue(), authorizer);
            });
            return builder.build();
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
    public Optional<Authorizer> getAuthorizer(String name) {
        return Optional.ofNullable(authorizers.get(name));
    }

    /**
     * Gets an immuatable map of authorizer names to their definitions.
     *
     * @return Returns the authorizers.
     */
    public Map<String, Authorizer> getAllAuthorizers() {
        return authorizers;
    }

    @Override
    public Builder toBuilder() {
        return builder().authorizers(authorizers);
    }

    @Override
    protected Node createNode() {
        return authorizers.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .collect(ObjectNode.collectStringKeys(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Builds an {@link AuthorizersTrait}.
     */
    public static final class Builder extends AbstractTraitBuilder<AuthorizersTrait, Builder> {
        private final Map<String, Authorizer> authorizers = new HashMap<>();

        @Override
        public AuthorizersTrait build() {
            return new AuthorizersTrait(this);
        }

        /**
         * Adds an authorizer.
         *
         * @param name Name of the authorizer that must correspond to
         *  an authentication scheme name.
         * @param authorizer Authorizer definition.
         * @return Returns the builder.
         */
        public Builder putAuthorizer(String name, Authorizer authorizer) {
            authorizers.put(name, Objects.requireNonNull(authorizer));
            return this;
        }

        /**
         * Replaces all of the authorizers with the given map.
         *
         * @param authorizers Map of authorizer names to their definitions.
         * @return Returns the builder.
         */
        public Builder authorizers(Map<String, Authorizer> authorizers) {
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
