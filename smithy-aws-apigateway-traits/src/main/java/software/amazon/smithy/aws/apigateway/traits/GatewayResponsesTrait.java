/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.apigateway.traits;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AbstractTrait;
import software.amazon.smithy.model.traits.Trait;

/**
 * Defines custom gateway responses for an API Gateway REST API. Gateway
 * responses customize error responses for authentication failures,
 * integration errors, and other API Gateway-generated errors.
 */
public final class GatewayResponsesTrait extends AbstractTrait {
    public static final ShapeId ID = ShapeId.from("aws.apigateway#gatewayResponses");

    private final ObjectNode value;

    public GatewayResponsesTrait(ObjectNode value) {
        super(ID, value);
        this.value = value;
    }

    /**
     * Gets the gateway responses as an ObjectNode.
     *
     * @return Returns the gateway responses map as a Node.
     */
    public ObjectNode getValue() {
        return value;
    }

    @Override
    protected Node createNode() {
        return value;
    }

    public static final class Provider extends AbstractTrait.Provider {
        public Provider() {
            super(ID);
        }

        @Override
        public Trait createTrait(ShapeId target, Node value) {
            return new GatewayResponsesTrait(value.expectObjectNode());
        }
    }
}
