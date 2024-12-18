/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.selector;

import software.amazon.smithy.model.shapes.Shape;

/**
 * Filters out shapes that yield shapes when applied to a selector.
 */
final class NotSelector implements InternalSelector {

    private final InternalSelector selector;

    NotSelector(InternalSelector selector) {
        this.selector = selector;
    }

    @Override
    public Response push(Context context, Shape shape, Receiver next) {
        if (!context.receivedShapes(shape, selector)) {
            return next.apply(context, shape);
        } else {
            return Response.CONTINUE;
        }
    }
}
