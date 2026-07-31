/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.selector;

import java.util.Set;
import software.amazon.smithy.model.shapes.Shape;

/**
 * Pushes the shapes stored in a specific variable to the next selector.
 */
final class VariableGetSelector implements InternalSelector {
    private final int slot;

    VariableGetSelector(int slot) {
        this.slot = slot;
    }

    @Override
    public Response push(Context context, Shape shape, Receiver next) {
        // Do not fail on invalid variable access.
        for (Shape v : getShapes(context)) {
            if (next.apply(context, v) == Response.STOP) {
                // Propagate the signal to stop upstream.
                return Response.STOP;
            }
        }

        return Response.CONTINUE;
    }

    private Set<Shape> getShapes(Context context) {
        return context.getVariable(slot);
    }

    @Override
    public ContainsShape containsShapeOptimization(Context context, Shape shape) {
        return getShapes(context).contains(shape) ? ContainsShape.YES : ContainsShape.NO;
    }

    @Override
    public ContainsShape emitsAnyOptimization(Context context, Shape input) {
        return getShapes(context).isEmpty() ? ContainsShape.NO : ContainsShape.YES;
    }
}
