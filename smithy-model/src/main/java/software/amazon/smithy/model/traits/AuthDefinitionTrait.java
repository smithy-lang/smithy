/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import java.util.List;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.BuilderRef;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * This trait is attached to another trait to define an auth scheme.
 */
public final class AuthDefinitionTrait extends AbstractTrait implements ToSmithyBuilder<AuthDefinitionTrait> {

    public static final ShapeId ID = ShapeId.from("smithy.api#authDefinition");
    private final List<ShapeId> traits;

    public AuthDefinitionTrait(Builder builder) {
        super(ID, builder.getSourceLocation());
        traits = builder.traits.copy();
    }

    /**
     * Gets the list of shape IDs that auth implementations must know about
     * in order to successfully utilize the auth scheme.
     *
     * @return Returns the auth traits.
     */
    public List<ShapeId> getTraits() {
        return traits;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    protected Node createNode() {
        ObjectNode.Builder builder = Node.objectNodeBuilder();
        builder.sourceLocation(getSourceLocation());
        if (!traits.isEmpty()) {
            ArrayNode ids = traits.stream()
                    .map(ShapeId::toString)
                    .map(Node::from)
                    .collect(ArrayNode.collect());
            builder.withMember("traits", ids);
        }
        return builder.build();
    }

    @Override
    public Builder toBuilder() {
        return builder().sourceLocation(getSourceLocation()).traits(traits);
    }

    public static final class Provider extends AbstractTrait.Provider {
        public Provider() {
            super(ID);
        }

        @Override
        public AuthDefinitionTrait createTrait(ShapeId target, Node value) {
            Builder builder = builder().sourceLocation(value);
            value.expectObjectNode().getArrayMember("traits", ShapeId::fromNode, builder::traits);
            AuthDefinitionTrait result = builder.build();
            result.setNodeCache(value);
            return result;
        }
    }

    public static final class Builder extends AbstractTraitBuilder<AuthDefinitionTrait, Builder> {
        private final BuilderRef<List<ShapeId>> traits = BuilderRef.forList();

        @Override
        public AuthDefinitionTrait build() {
            return new AuthDefinitionTrait(this);
        }

        public Builder traits(List<ShapeId> traits) {
            this.traits.clear();
            this.traits.get().addAll(traits);
            return this;
        }

        public Builder addTrait(ShapeId trait) {
            traits.get().add(trait);
            return this;
        }
    }
}
