/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import java.util.Optional;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * An HTTP-specific authentication scheme that sends an arbitrary
 * API key in a header or query string parameter.
 */
public final class HttpApiKeyAuthTrait extends AbstractTrait implements ToSmithyBuilder<HttpApiKeyAuthTrait> {

    public static final ShapeId ID = ShapeId.from("smithy.api#httpApiKeyAuth");

    private final String scheme;
    private final String name;
    private final Location in;

    private HttpApiKeyAuthTrait(Builder builder) {
        super(ID, builder.getSourceLocation());
        name = SmithyBuilder.requiredState("name", builder.name);
        in = SmithyBuilder.requiredState("in", builder.in);
        scheme = builder.scheme;
    }

    public Optional<String> getScheme() {
        return Optional.ofNullable(scheme);
    }

    public String getName() {
        return name;
    }

    public Location getIn() {
        return in;
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .sourceLocation(getSourceLocation())
                .scheme(getScheme().orElse(null))
                .name(getName())
                .in(getIn());
    }

    @Override
    protected Node createNode() {
        ObjectNode.Builder builder = Node.objectNodeBuilder()
                .sourceLocation(getSourceLocation())
                .withMember("name", getName())
                .withMember("in", getIn().toString())
                .withOptionalMember("scheme", getScheme().map(Node::from));
        return builder.build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public enum Location {
        HEADER("header"),
        QUERY("query");

        private final String serialized;

        Location(String serialized) {
            this.serialized = serialized;
        }

        static Location from(String value) {
            for (Location location : values()) {
                if (location.serialized.equals(value)) {
                    return location;
                }
            }
            throw new IllegalArgumentException("Invalid location type: " + value);
        }

        @Override
        public String toString() {
            return serialized;
        }
    }

    public static final class Provider extends AbstractTrait.Provider {
        public Provider() {
            super(ID);
        }

        @Override
        public Trait createTrait(ShapeId target, Node value) {
            Builder builder = builder().sourceLocation(value.getSourceLocation());
            value.expectObjectNode()
                    .getStringMember("scheme", builder::scheme)
                    .expectStringMember("name", builder::name)
                    .getStringMember("in", s -> builder.in(Location.from(s)));
            HttpApiKeyAuthTrait result = builder.build();
            result.setNodeCache(value);
            return result;
        }
    }

    public static final class Builder extends AbstractTraitBuilder<HttpApiKeyAuthTrait, Builder> {
        private String scheme;
        private String name;
        private Location in;

        private Builder() {}

        @Override
        public HttpApiKeyAuthTrait build() {
            return new HttpApiKeyAuthTrait(this);
        }

        public Builder scheme(String scheme) {
            this.scheme = scheme;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder in(Location in) {
            this.in = in;
            return this;
        }
    }
}
