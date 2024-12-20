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
 * A trait that is attached to other traits to define a Smithy protocol.
 */
public final class ProtocolDefinitionTrait extends AbstractTrait implements ToSmithyBuilder<ProtocolDefinitionTrait> {

    public static final ShapeId ID = ShapeId.from("smithy.api#protocolDefinition");

    private final List<ShapeId> traits;
    private final boolean noInlineDocumentSupport;

    public ProtocolDefinitionTrait(Builder builder) {
        super(ID, builder.getSourceLocation());
        traits = builder.traits.copy();
        noInlineDocumentSupport = builder.noInlineDocumentSupport;
    }

    /**
     * Gets the list of shape IDs that protocol implementations must know about
     * in order to successfully utilize the protocol.
     *
     * @return Returns the protocol traits.
     */
    public List<ShapeId> getTraits() {
        return traits;
    }

    /**
     * Checks if this protocol does not support inline documents.
     *
     * @return Returns true if inline documents are not supported.
     */
    public boolean getNoInlineDocumentSupport() {
        return noInlineDocumentSupport;
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

            if (noInlineDocumentSupport) {
                builder.withMember("noInlineDocumentSupport", true);
            }
        }
        return builder.build();
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .sourceLocation(getSourceLocation())
                .traits(traits)
                .noInlineDocumentSupport(noInlineDocumentSupport);
    }

    public static final class Provider extends AbstractTrait.Provider {
        public Provider() {
            super(ID);
        }

        @Override
        public ProtocolDefinitionTrait createTrait(ShapeId target, Node value) {
            Builder builder = builder().sourceLocation(value);
            value.expectObjectNode()
                    .getArrayMember("traits", ShapeId::fromNode, builder::traits)
                    .getBooleanMember("noInlineDocumentSupport", builder::noInlineDocumentSupport);
            ProtocolDefinitionTrait result = builder.build();
            result.setNodeCache(value);
            return result;
        }
    }

    public static final class Builder extends AbstractTraitBuilder<ProtocolDefinitionTrait, Builder> {
        private final BuilderRef<List<ShapeId>> traits = BuilderRef.forList();
        private boolean noInlineDocumentSupport;

        @Override
        public ProtocolDefinitionTrait build() {
            return new ProtocolDefinitionTrait(this);
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

        public Builder noInlineDocumentSupport(boolean noInlineDocumentSupport) {
            this.noInlineDocumentSupport = noInlineDocumentSupport;
            return this;
        }
    }
}
