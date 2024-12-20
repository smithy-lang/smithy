/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Provides a default value for a shape or member.
 */
public final class DefaultTrait extends AbstractTrait {

    public static final ShapeId ID = ShapeId.from("smithy.api#default");

    public DefaultTrait(Node value) {
        super(ID, value);
    }

    @Override
    protected Node createNode() {
        throw new UnsupportedOperationException("NodeCache is always set");
    }

    public static final class Provider extends AbstractTrait.Provider {
        public Provider() {
            super(ID);
        }

        @Override
        public Trait createTrait(ShapeId target, Node value) {
            return new DefaultTrait(value);
        }
    }
}
