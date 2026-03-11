/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.protocol.traits;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AbstractTrait;
import software.amazon.smithy.utils.ToSmithyBuilder;

public final class Rpcv2JsonTrait extends Rpcv2ProtocolTrait implements ToSmithyBuilder<Rpcv2JsonTrait> {

    public static final ShapeId ID = ShapeId.from("smithy.protocols#rpcv2Json");

    private Rpcv2JsonTrait(Builder builder) {
        super(ID, builder);
    }

    /**
     * Creates a new {@code Builder}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates the trait from a Node.
     *
     * @param node Node object that must be a valid {@code ObjectNode}.
     * @return Returns the created trait.
     */
    public static Rpcv2JsonTrait fromNode(Node node) {
        return builder().fromNode(node).build();
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .http(getHttp())
                .eventStreamHttp(getEventStreamHttp());
    }

    /**
     * Builder for creating a {@code Rpcv2JsonTrait}.
     */
    public static final class Builder extends Rpcv2ProtocolTrait.Builder<Rpcv2JsonTrait, Builder> {

        @Override
        public Rpcv2JsonTrait build() {
            return new Rpcv2JsonTrait(this);
        }
    }

    /**
     * Implements the {@code AbstractTrait.Provider}.
     */
    public static final class Provider extends AbstractTrait.Provider {

        public Provider() {
            super(ID);
        }

        @Override
        public Rpcv2JsonTrait createTrait(ShapeId target, Node value) {
            Rpcv2JsonTrait result = fromNode(value);
            result.setNodeCache(value);
            return result;
        }
    }
}
