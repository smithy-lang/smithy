/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.model.traits;

import java.util.ArrayList;
import java.util.List;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * A trait that is attached to other traits to define a Smithy protocol.
 */
public final class ProtocolDefinitionTrait extends AbstractTrait implements ToSmithyBuilder<ProtocolDefinitionTrait> {

    public static final ShapeId ID = ShapeId.from("smithy.api#protocolDefinition");
    private final List<ShapeId> traits;

    public ProtocolDefinitionTrait(Builder builder) {
        super(ID, builder.getSourceLocation());
        traits = ListUtils.copyOf(builder.traits);
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

    public static Builder builder() {
        return new Builder();
    }

    @Override
    protected Node createNode() {
        if (traits.isEmpty()) {
            return Node.objectNode();
        }

        ArrayNode ids = traits.stream()
                .map(ShapeId::toString)
                .map(Node::from)
                .collect(ArrayNode.collect());

        return Node.objectNode().withMember("traits", ids);
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
        public ProtocolDefinitionTrait createTrait(ShapeId target, Node value) {
            Builder builder = builder().sourceLocation(value);
            ObjectNode objectNode = value.expectObjectNode();
            objectNode.getArrayMember("traits").ifPresent(traits -> {
                for (String string : Node.loadArrayOfString("traits", traits)) {
                    builder.addTrait(ShapeId.from(string));
                }
            });
            return builder.build();
        }
    }

    public static final class Builder extends AbstractTraitBuilder<ProtocolDefinitionTrait, Builder> {
        private final List<ShapeId> traits = new ArrayList<>();

        @Override
        public ProtocolDefinitionTrait build() {
            return new ProtocolDefinitionTrait(this);
        }

        public Builder traits(List<ShapeId> traits) {
            this.traits.clear();
            this.traits.addAll(traits);
            return this;
        }

        public Builder addTrait(ShapeId trait) {
            traits.add(trait);
            return this;
        }
    }
}
