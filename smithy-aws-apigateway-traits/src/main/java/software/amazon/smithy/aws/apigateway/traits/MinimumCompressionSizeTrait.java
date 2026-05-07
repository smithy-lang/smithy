/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.apigateway.traits;

import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NumberNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AbstractTrait;
import software.amazon.smithy.model.traits.Trait;

/**
 * Defines the minimum payload size in bytes at which compression is applied on an API Gateway REST API.
 */
public final class MinimumCompressionSizeTrait extends AbstractTrait {
    public static final ShapeId ID = ShapeId.from("aws.apigateway#minimumCompressionSize");

    private final int value;

    public MinimumCompressionSizeTrait(int value, FromSourceLocation sourceLocation) {
        super(ID, sourceLocation);
        this.value = value;
    }

    public MinimumCompressionSizeTrait(int value) {
        this(value, SourceLocation.NONE);
    }

    /**
     * Gets the minimum compression size value.
     *
     * @return Returns the minimum compression size in bytes.
     */
    public int getValue() {
        return value;
    }

    @Override
    protected Node createNode() {
        return new NumberNode(value, getSourceLocation());
    }

    public static final class Provider extends AbstractTrait.Provider {
        public Provider() {
            super(ID);
        }

        @Override
        public Trait createTrait(ShapeId target, Node value) {
            return new MinimumCompressionSizeTrait(
                    value.expectNumberNode().getValue().intValue(),
                    value.getSourceLocation());
        }
    }
}
