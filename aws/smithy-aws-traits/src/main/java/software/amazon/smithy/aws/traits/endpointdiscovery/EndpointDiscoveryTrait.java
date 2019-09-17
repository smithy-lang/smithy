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

package software.amazon.smithy.aws.traits.endpointdiscovery;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AbstractTrait;
import software.amazon.smithy.model.traits.AbstractTraitBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Configures endpoint discovery for the targeted service.
 */
public final class EndpointDiscoveryTrait extends AbstractTrait implements ToSmithyBuilder<EndpointDiscoveryTrait> {
    public static final ShapeId ID = ShapeId.from("aws.api#endpointDiscovery");

    private static final String OPERATION = "operation";
    private static final String ERROR = "error";
    private static final List<String> PROPERTIES = Collections.unmodifiableList(Arrays.asList(OPERATION, ERROR));

    private final ShapeId operation;
    private final ShapeId error;

    public EndpointDiscoveryTrait(Builder builder) {
        super(ID, builder.getSourceLocation());
        this.operation = builder.operation;
        this.error = builder.error;
    }

    /**
     * @return Returns a builder used to create {@link EndpointDiscoveryTrait}
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
     * which is marked with an {@link DiscoveredEndpointTrait}.</p>
     *
     * @return The ShapeId of the invalid endpoint error.
     */
    public ShapeId getError() {
        return error;
    }

    @Override
    protected Node createNode() {
        return Node.objectNode()
                .withMember(OPERATION, Node.from(getOperation().toString()))
                .withMember(ERROR, Node.from(getError().toString()));
    }

    @Override
    public Builder toBuilder() {
        return new Builder()
                .operation(getOperation())
                .error(getError());
    }

    /** Builder for {@link EndpointDiscoveryTrait}. */
    public static final class Builder extends AbstractTraitBuilder<EndpointDiscoveryTrait, Builder> {
        private ShapeId operation;
        private ShapeId error;

        private Builder() {}

        @Override
        public EndpointDiscoveryTrait build() {
            return new EndpointDiscoveryTrait(this);
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
         * which is marked with an {@link DiscoveredEndpointTrait}.</p>
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
        public EndpointDiscoveryTrait createTrait(ShapeId target, Node value) {
            ObjectNode objectNode = value.expectObjectNode();
            objectNode.warnIfAdditionalProperties(PROPERTIES);

            return builder()
                    .operation(objectNode.expectStringMember(OPERATION).expectShapeId())
                    .error(objectNode.expectStringMember(ERROR).expectShapeId())
                    .build();
        }
    }
}
