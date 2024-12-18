/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import java.util.Optional;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Marks a shape as deprecated.
 */
public final class DeprecatedTrait extends AbstractTrait implements ToSmithyBuilder<DeprecatedTrait> {
    public static final ShapeId ID = ShapeId.from("smithy.api#deprecated");

    private final String since;
    private final String message;

    private DeprecatedTrait(DeprecatedTrait.Builder builder) {
        super(ID, builder.sourceLocation);
        this.since = builder.since;
        this.message = builder.message;
    }

    public static final class Provider extends AbstractTrait.Provider {
        public Provider() {
            super(ID);
        }

        @Override
        public Trait createTrait(ShapeId target, Node value) {
            DeprecatedTrait.Builder builder = builder().sourceLocation(value);
            value.expectObjectNode()
                    .getStringMember("since", builder::since)
                    .getStringMember("message", builder::message);
            DeprecatedTrait result = builder.build();
            result.setNodeCache(value);
            return result;
        }
    }

    /**
     * Gets the deprecated since value.
     *
     * @return returns the optional deprecated since value.
     */
    public Optional<String> getSince() {
        return Optional.ofNullable(since);
    }

    /**
     * Gets the deprecation message value.
     *
     * @return returns the optional deprecation message value.
     */
    public Optional<String> getMessage() {
        return Optional.ofNullable(message);
    }

    public String getDeprecatedDescription(ShapeType shapeType) {
        StringBuilder builder = new StringBuilder("This ");
        builder.append(shapeType == ShapeType.OPERATION ? "operation" : "shape").append(" is deprecated");
        if (since != null) {
            builder.append(" since ").append(since);
        }
        if (message != null) {
            builder.append(": ").append(message);
        } else {
            builder.append(".");
        }
        return builder.toString();
    }

    @Override
    protected Node createNode() {
        return new ObjectNode(MapUtils.of(), getSourceLocation())
                .withOptionalMember("since", getSince().map(Node::from))
                .withOptionalMember("message", getMessage().map(Node::from));
    }

    @Override
    public DeprecatedTrait.Builder toBuilder() {
        return builder().since(since).message(message).sourceLocation(getSourceLocation());
    }

    /**
     * @return Returns a builder used to create a deprecated trait.
     */
    public static DeprecatedTrait.Builder builder() {
        return new Builder();
    }

    /**
     * Builder used to create a DeprecatedTrait.
     */
    public static final class Builder extends AbstractTraitBuilder<DeprecatedTrait, Builder> {
        private String since;
        private String message;

        private Builder() {}

        public Builder since(String since) {
            this.since = since;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        @Override
        public DeprecatedTrait build() {
            return new DeprecatedTrait(this);
        }
    }
}
