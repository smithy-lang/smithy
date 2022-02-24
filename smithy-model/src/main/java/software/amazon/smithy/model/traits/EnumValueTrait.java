/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *   http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.model.traits;

import java.util.Optional;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.node.ExpectationNotMetException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NumberNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Sets the value for an enum member.
 */
public final class EnumValueTrait extends AbstractTrait implements ToSmithyBuilder<EnumValueTrait> {
    public static final ShapeId ID = ShapeId.from("smithy.api#enumValue");

    private final String string;
    private final Integer integer;

    private EnumValueTrait(Builder builder) {
        super(ID, builder.sourceLocation);
        string = builder.string;
        integer = builder.integer;
        if (string == null && integer == null) {
            throw new SourceException(
                    "Either a string value or an integer value must be set for the enumValue trait.",
                    getSourceLocation()
            );
        }
    }

    /**
     * Gets the string value if a string value was set.
     *
     * @return Optionally returns the string value.
     */
    public Optional<String> getStringValue() {
        return Optional.ofNullable(string);
    }

    /**
     * Gets the string value.
     *
     * @return Returns the string value.
     * @throws ExpectationNotMetException if the string value was not set.
     */
    public String expectStringValue() {
        return getStringValue().orElseThrow(() -> new ExpectationNotMetException(
                "Expected string value was not set.", this
        ));
    }

    /**
     * Gets the int value if an int value was set.
     *
     * @return Returns the set int value.
     */
    public Optional<Integer> getIntValue() {
        return Optional.ofNullable(integer);
    }

    /**
     * Gets the int value.
     *
     * @return Returns the int value.
     * @throws ExpectationNotMetException if the int value was not set.
     */
    public int expectIntValue() {
        return getIntValue().orElseThrow(() -> new ExpectationNotMetException(
                "Expected integer value was not set.", this
        ));
    }

    public static final class Provider extends AbstractTrait.Provider {
        public Provider() {
            super(ID);
        }

        @Override
        public Trait createTrait(ShapeId target, Node value) {
            Builder builder = builder().sourceLocation(value);
            value.asStringNode().ifPresent(node -> builder.stringValue(node.getValue()));
            value.asNumberNode().ifPresent(node -> {
                if (node.isNaturalNumber()) {
                    builder.intValue(node.getValue().intValue());
                }
            });
            EnumValueTrait result = builder.build();
            result.setNodeCache(value);
            return result;
        }
    }

    @Override
    protected Node createNode() {
        if (getIntValue().isPresent()) {
            return new NumberNode(integer, getSourceLocation());
        } else {
            return new StringNode(string, getSourceLocation());

        }
    }

    @Override
    public SmithyBuilder<EnumValueTrait> toBuilder() {
        Builder builder = builder().sourceLocation(getSourceLocation());
        if (getIntValue().isPresent()) {
            builder.intValue(getIntValue().get());
        } else if (getStringValue().isPresent()) {
            builder.stringValue(getStringValue().get());
        }
        return builder;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder extends AbstractTraitBuilder<EnumValueTrait, Builder> {
        private String string;
        private Integer integer;

        @Override
        public EnumValueTrait build() {
            return new EnumValueTrait(this);
        }

        public Builder stringValue(String string) {
            this.string = string;
            this.integer = null;
            return this;
        }

        public Builder intValue(int integer) {
            this.integer = integer;
            this.string = null;
            return this;
        }
    }
}
