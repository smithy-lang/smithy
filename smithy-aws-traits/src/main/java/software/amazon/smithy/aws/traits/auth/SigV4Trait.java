/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.traits.auth;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AbstractTrait;
import software.amazon.smithy.model.traits.AbstractTraitBuilder;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Adds AWS signature version 4 authentication to a service or operation.
 */
public final class SigV4Trait extends AbstractTrait implements ToSmithyBuilder<SigV4Trait> {

    public static final ShapeId ID = ShapeId.from("aws.auth#sigv4");
    private static final String NAME = "name";

    private final String name;

    private SigV4Trait(Builder builder) {
        super(ID, builder.getSourceLocation());
        this.name = SmithyBuilder.requiredState(NAME, builder.name);
    }

    /**
     * @return Gets the service signing name.
     */
    public String getName() {
        return name;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .name(getName())
                .sourceLocation(getSourceLocation());
    }

    @Override
    protected Node createNode() {
        return Node.objectNodeBuilder().sourceLocation(getSourceLocation()).withMember(NAME, getName()).build();
    }

    public static final class Builder extends AbstractTraitBuilder<SigV4Trait, Builder> {
        private String name;

        private Builder() {}

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        @Override
        public SigV4Trait build() {
            return new SigV4Trait(this);
        }
    }

    public static final class Provider extends AbstractTrait.Provider {
        public Provider() {
            super(ID);
        }

        @Override
        public Trait createTrait(ShapeId target, Node value) {
            Builder builder = builder().sourceLocation(value);
            ObjectNode objectNode = value.expectObjectNode();
            builder.name(objectNode.expectStringMember(NAME).getValue());
            SigV4Trait result = builder.build();
            result.setNodeCache(objectNode);
            return result;
        }
    }
}
