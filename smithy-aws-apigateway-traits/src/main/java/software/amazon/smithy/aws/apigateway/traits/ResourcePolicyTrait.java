/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.apigateway.traits;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AbstractTrait;
import software.amazon.smithy.model.traits.Trait;

/**
 * Defines a resource policy for an API Gateway REST API. A resource policy
 * is a JSON policy document attached to an API that controls whether a
 * specified principal (typically an IAM role or group) can invoke the API.
 */
public final class ResourcePolicyTrait extends AbstractTrait {
    public static final ShapeId ID = ShapeId.from("aws.apigateway#resourcePolicy");

    private final Node value;

    public ResourcePolicyTrait(Node value) {
        super(ID, value);
        this.value = value;
    }

    /**
     * Gets the resource policy document value.
     *
     * @return Returns the resource policy as a Node.
     */
    public Node getValue() {
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
            return new ResourcePolicyTrait(value);
        }
    }
}
