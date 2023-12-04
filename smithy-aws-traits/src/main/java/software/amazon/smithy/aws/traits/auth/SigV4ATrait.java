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
 * Adds AWS Signature Version 4 Asymmetric authentication to a service or operation.
 */
public final class SigV4ATrait extends AbstractTrait implements ToSmithyBuilder<SigV4ATrait> {
    public static final ShapeId ID = ShapeId.from("aws.auth#sigv4a");
    private static final String NAME = "name";

    private final String name;

    private SigV4ATrait(Builder builder) {
        super(ID, builder.getSourceLocation());
        this.name = SmithyBuilder.requiredState(NAME, builder.name);
    }

    /**
     * Gets the service signing name.
     *
     * @return the service signing name
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
            .sourceLocation(getSourceLocation())
            .name(getName());
    }

    @Override
    protected Node createNode() {
        return Node.objectNodeBuilder()
            .sourceLocation(getSourceLocation())
            .withMember(NAME, getName())
            .build();
    }

    public static final class Builder extends AbstractTraitBuilder<SigV4ATrait, Builder> {
        private String name;

        private Builder() {}

        @Override
        public SigV4ATrait build() {
            return new SigV4ATrait(this);
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }
    }

    public static final class Provider extends AbstractTrait.Provider {
        public Provider() {
            super(ID);
        }

        @Override
        public Trait createTrait(ShapeId target, Node value) {
            ObjectNode objectNode = value.expectObjectNode();
            Builder builder = builder().sourceLocation(value);
            builder.name(objectNode.expectStringMember(NAME).getValue());
            SigV4ATrait result = builder.build();
            result.setNodeCache(objectNode);
            return result;
        }
    }
}
