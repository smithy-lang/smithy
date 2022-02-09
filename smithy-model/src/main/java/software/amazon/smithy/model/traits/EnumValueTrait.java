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
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NumberNode;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

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

    public Optional<String> getStringValue() {
        return Optional.ofNullable(string);
    }

    public Optional<Integer> getIntValue() {
        return Optional.ofNullable(integer);
    }

    public static final class Provider extends AbstractTrait.Provider {
        public Provider() {
            super(ID);
        }

        @Override
        public Trait createTrait(ShapeId target, Node value) {
            Builder builder = builder().sourceLocation(value);
            ObjectNode objectNode = value.expectObjectNode();
            objectNode.getMember("string")
                    .map(v -> v.expectStringNode().getValue())
                    .ifPresent(builder::stringValue);
            objectNode.getMember("int")
                    .map(v -> v.expectNumberNode().getValue().intValue())
                    .ifPresent(builder::intValue);
            return builder.build();
        }
    }

    @Override
    protected Node createNode() {
        return ObjectNode.builder()
                .sourceLocation(getSourceLocation())
                .withOptionalMember("string", getStringValue().map(StringNode::from))
                .withOptionalMember("int", getIntValue().map(NumberNode::from))
                .build();
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
