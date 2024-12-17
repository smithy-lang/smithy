/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.traits.clientendpointdiscovery;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AbstractTrait;
import software.amazon.smithy.model.traits.AbstractTraitBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Indicates that the target operation should use the SDK's endpoint discovery
 * logic.
 */
public final class ClientDiscoveredEndpointTrait extends AbstractTrait
        implements ToSmithyBuilder<ClientDiscoveredEndpointTrait> {
    public static final ShapeId ID = ShapeId.from("aws.api#clientDiscoveredEndpoint");

    private static final String REQUIRED = "required";

    private final boolean required;

    private ClientDiscoveredEndpointTrait(Builder builder) {
        super(ID, builder.getSourceLocation());
        this.required = builder.required;
    }

    /**
     * @return Returns a builder used to create {@link ClientDiscoveredEndpointTrait}
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return Returns whether or not the service requires endpoint discovery.
     */
    public boolean isRequired() {
        return required;
    }

    @Override
    protected Node createNode() {
        return Node.objectNodeBuilder()
                .sourceLocation(getSourceLocation())
                .withMember(REQUIRED, Node.from(isRequired()))
                .build();
    }

    @Override
    public Builder toBuilder() {
        return builder().sourceLocation(getSourceLocation()).required(required);
    }

    /** Builder for {@link ClientDiscoveredEndpointTrait}. */
    public static final class Builder extends AbstractTraitBuilder<ClientDiscoveredEndpointTrait, Builder> {
        private boolean required;

        private Builder() {}

        @Override
        public ClientDiscoveredEndpointTrait build() {
            return new ClientDiscoveredEndpointTrait(this);
        }

        public Builder required(boolean required) {
            this.required = required;
            return this;
        }
    }

    public static final class Provider extends AbstractTrait.Provider {
        public Provider() {
            super(ID);
        }

        @Override
        public ClientDiscoveredEndpointTrait createTrait(ShapeId target, Node value) {
            ObjectNode objectNode = value.expectObjectNode();
            ClientDiscoveredEndpointTrait result = builder()
                    .sourceLocation(value)
                    .required(objectNode.getBooleanMemberOrDefault(REQUIRED, true))
                    .build();
            result.setNodeCache(objectNode);
            return result;
        }
    }
}
