/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import java.util.Optional;
import software.amazon.smithy.model.node.ExpectationNotMetException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NumberNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Sets the value for an enum member.
 */
public final class EnumValueTrait extends AbstractTrait implements ToSmithyBuilder<EnumValueTrait> {
    public static final ShapeId ID = ShapeId.from("smithy.api#enumValue");

    private final Node value;

    private EnumValueTrait(Builder builder) {
        super(ID, builder.sourceLocation);
        if (builder.value == null) {
            throw new IllegalStateException("No integer or string value set on EnumValueTrait");
        }
        value = builder.value;
    }

    /**
     * Gets the string value if a string value was set.
     *
     * @return Optionally returns the string value.
     */
    public Optional<String> getStringValue() {
        return value.asStringNode().map(StringNode::getValue);
    }

    /**
     * Gets the string value.
     *
     * @return Returns the string value.
     * @throws ExpectationNotMetException if the string value was not set.
     */
    public String expectStringValue() {
        return getStringValue().orElseThrow(() -> new ExpectationNotMetException(
                "Expected string value was not set.",
                this));
    }

    /**
     * Gets the int value if an int value was set.
     *
     * @return Returns the set int value.
     */
    public Optional<Integer> getIntValue() {
        return value.asNumberNode().map(NumberNode::getValue).map(Number::intValue);
    }

    /**
     * Gets the int value.
     *
     * @return Returns the int value.
     * @throws ExpectationNotMetException if the int value was not set.
     */
    public int expectIntValue() {
        return getIntValue().orElseThrow(() -> new ExpectationNotMetException(
                "Expected integer value was not set.",
                this));
    }

    public static final class Provider extends AbstractTrait.Provider {
        public Provider() {
            super(ID);
        }

        @Override
        public Trait createTrait(ShapeId target, Node value) {
            Builder builder = builder().sourceLocation(value);
            builder.value = value;
            return builder.build();
        }
    }

    @Override
    protected Node createNode() {
        return value;
    }

    @Override
    public Builder toBuilder() {
        Builder builder = builder().sourceLocation(getSourceLocation());
        builder.value = value;
        return builder;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder extends AbstractTraitBuilder<EnumValueTrait, Builder> {
        private Node value;

        @Override
        public EnumValueTrait build() {
            return new EnumValueTrait(this);
        }

        public Builder stringValue(String string) {
            this.value = Node.from(string);
            return this;
        }

        public Builder intValue(int integer) {
            this.value = Node.from(integer);
            return this;
        }
    }
}
