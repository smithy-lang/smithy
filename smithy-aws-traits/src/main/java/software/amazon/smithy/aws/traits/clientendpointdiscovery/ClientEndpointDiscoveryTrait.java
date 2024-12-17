/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.traits.clientendpointdiscovery;

import java.util.Optional;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AbstractTrait;
import software.amazon.smithy.model.traits.AbstractTraitBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Configures endpoint discovery for the targeted service.
 */
public final class ClientEndpointDiscoveryTrait extends AbstractTrait
        implements ToSmithyBuilder<ClientEndpointDiscoveryTrait> {
    public static final ShapeId ID = ShapeId.from("aws.api#clientEndpointDiscovery");

    private static final String OPERATION = "operation";
    private static final String ERROR = "error";

    private final ShapeId operation;
    private final ShapeId error;

    public ClientEndpointDiscoveryTrait(Builder builder) {
        super(ID, builder.getSourceLocation());
        this.operation = builder.operation;
        this.error = builder.error;
    }

    /**
     * @return Returns a builder used to create {@link ClientEndpointDiscoveryTrait}
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * The operation used to discover endpoints for the service.
     *
     * <p>The operation MUST be bound to the service.</p>
     *
     * @return The ShapeId of the operation used to discover endpoints.
     */
    public ShapeId getOperation() {
        return operation;
    }

    /**
     * The error shape which indicates to a client that an endpoint they are
     * using is no longer valid.
     *
     * <p>This error MUST be bound to every operation bound to the service
     * which is marked with an {@link ClientDiscoveredEndpointTrait}.</p>
     *
     * @return The ShapeId of the invalid endpoint error.
     */
    public Optional<ShapeId> getOptionalError() {
        return Optional.ofNullable(error);
    }

    /**
     * Deprecated in favor of {@link ClientEndpointDiscoveryTrait#getOptionalError}.
     */
    @Deprecated
    public ShapeId getError() {
        return error;
    }

    @Override
    protected Node createNode() {
        return Node.objectNodeBuilder()
                .sourceLocation(getSourceLocation())
                .withMember(OPERATION, Node.from(getOperation().toString()))
                .withOptionalMember(ERROR, getOptionalError().map(error -> Node.from(error.toString())))
                .build();
    }

    @Override
    public Builder toBuilder() {
        return new Builder()
                .sourceLocation(getSourceLocation())
                .operation(operation)
                .error(error);
    }

    /** Builder for {@link ClientEndpointDiscoveryTrait}. */
    public static final class Builder extends AbstractTraitBuilder<ClientEndpointDiscoveryTrait, Builder> {
        private ShapeId operation;
        private ShapeId error;

        private Builder() {}

        @Override
        public ClientEndpointDiscoveryTrait build() {
            return new ClientEndpointDiscoveryTrait(this);
        }

        /**
         * Set the operation used to discover endpoints for the service.
         *
         * <p>The operation MUST be bound to the service.</p>
         *
         * @param operation The ShapeId of the operation used to discover endpoints.
         * @return Returns the builder.
         */
        public Builder operation(ShapeId operation) {
            this.operation = operation;
            return this;
        }

        /**
         * Set the error shape which indicates to a client that an endpoint
         * they are using is no longer valid.
         *
         * <p>This error MUST be bound to every operation bound to the service
         * which is marked with an {@link ClientDiscoveredEndpointTrait}.</p>
         *
         * @param error The ShapeId of the invalid endpoint error.
         * @return Returns the builder.
         */
        public Builder error(ShapeId error) {
            this.error = error;
            return this;
        }
    }

    public static final class Provider extends AbstractTrait.Provider {
        public Provider() {
            super(ID);
        }

        @Override
        public ClientEndpointDiscoveryTrait createTrait(ShapeId target, Node value) {
            ObjectNode objectNode = value.expectObjectNode();
            Builder builder = builder()
                    .sourceLocation(value)
                    .operation(objectNode.expectStringMember(OPERATION).expectShapeId());
            objectNode.getStringMember(ERROR).ifPresent(error -> builder.error(error.expectShapeId()));
            ClientEndpointDiscoveryTrait result = builder.build();
            result.setNodeCache(objectNode);
            return result;
        }
    }
}
