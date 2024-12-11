/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * A general-purpose trait used to represent traits that are defined in the
 * model and have no concrete implementation.
 */
public final class DynamicTrait extends AbstractTrait {

    private final Node value;

    public DynamicTrait(ShapeId id, Node value) {
        super(id, value);
        this.value = value;
    }

    @Override
    protected Node createNode() {
        return value;
    }
}
