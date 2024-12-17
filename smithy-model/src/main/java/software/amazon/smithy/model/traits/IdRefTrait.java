/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import java.util.Optional;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.selector.Selector;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Indicates that a string value must contain a valid shape ID.
 *
 * <p>{@code failWhenMissing} can be set to true to fail when the
 * shape ID contained in a string shape cannot be found in the result
 * set of the {@code selector} property. {@code failWhenMissing} defaults to
 * {@code false} when not set.
 *
 * <p>The {@code selector} property is used to query the model for all shapes
 * that match a shape selector. The value contained within the string shape
 * targeted by this trait MUST match one of the shapes returns by the shape
 * selector if {@code failWhenMissing} is set to true. {@code selector}
 * defaults to "*" when not set.
 */
public final class IdRefTrait extends AbstractTrait implements ToSmithyBuilder<IdRefTrait> {
    public static final ShapeId ID = ShapeId.from("smithy.api#idRef");

    private final Selector selector;
    private final boolean failWhenMissing;
    private final String errorMessage;

    private IdRefTrait(Builder builder) {
        super(ID, builder.sourceLocation);
        selector = builder.selector;
        failWhenMissing = builder.failWhenMissing;
        errorMessage = builder.errorMessage;
    }

    public Selector getSelector() {
        return selector == null ? Selector.IDENTITY : selector;
    }

    public boolean failWhenMissing() {
        return failWhenMissing;
    }

    public Optional<String> getErrorMessage() {
        return Optional.ofNullable(errorMessage);
    }

    @Override
    public Builder toBuilder() {
        return builder().sourceLocation(getSourceLocation())
                .failWhenMissing(failWhenMissing)
                .selector(selector)
                .errorMessage(errorMessage);
    }

    @Override
    protected Node createNode() {
        ObjectNode.Builder builder = Node.objectNodeBuilder()
                .sourceLocation(getSourceLocation())
                .withOptionalMember(
                        "selector",
                        Optional.ofNullable(selector).map(Selector::toString).map(Node::from))
                .withOptionalMember("errorMessage", getErrorMessage().map(Node::from));
        if (failWhenMissing) {
            builder = builder.withMember("failWhenMissing", Node.from(true));
        }
        return builder.build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder extends AbstractTraitBuilder<IdRefTrait, Builder> {
        private Selector selector;
        private boolean failWhenMissing;
        private String errorMessage;

        private Builder() {}

        public Builder failWhenMissing(boolean failWhenMissing) {
            this.failWhenMissing = failWhenMissing;
            return this;
        }

        public Builder selector(Selector selector) {
            this.selector = selector;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        @Override
        public IdRefTrait build() {
            return new IdRefTrait(this);
        }
    }

    public static final class Provider implements TraitService {
        @Override
        public ShapeId getShapeId() {
            return ID;
        }

        @Override
        public IdRefTrait createTrait(ShapeId target, Node value) {
            Builder builder = builder().sourceLocation(value);
            value.expectObjectNode()
                    .getMember("selector", Selector::fromNode, builder::selector)
                    .getBooleanMember("failWhenMissing", builder::failWhenMissing)
                    .getStringMember("errorMessage", builder::errorMessage);
            IdRefTrait result = builder.build();
            result.setNodeCache(value);
            return result;
        }
    }
}
