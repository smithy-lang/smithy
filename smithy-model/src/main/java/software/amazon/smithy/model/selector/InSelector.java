/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.selector;

import software.amazon.smithy.model.shapes.Shape;

/**
 * Checks if the given value is in the result of a selector.
 */
final class InSelector implements InternalSelector {

    private final InternalSelector selector;

    InSelector(InternalSelector selector) {
        this.selector = selector;
    }

    @Override
    public Response push(Context context, Shape shape, Receiver next) {
        // Some internal selectors provide optimizations for quickly checking if they contain a shape.
        switch (selector.containsShapeOptimization(context, shape)) {
            case YES:
                return next.apply(context, shape);
            case NO:
                return Response.CONTINUE;
            case MAYBE:
            default:
                // Unable to use the optimization, so emit each shape until a match is found.
                FilteredHolder holder = new FilteredHolder(shape);
                selector.push(context, shape, holder);

                if (holder.matched) {
                    return next.apply(context, shape);
                }

                return Response.CONTINUE;
        }
    }

    private static final class FilteredHolder implements InternalSelector.Receiver {
        private final Shape shapeToMatch;
        private boolean matched;

        FilteredHolder(Shape shapeToMatch) {
            this.shapeToMatch = shapeToMatch;
        }

        @Override
        public Response apply(Context context, Shape shape) {
            if (shape.equals(shapeToMatch)) {
                matched = true;
                return Response.STOP;
            }

            return Response.CONTINUE;
        }
    }
}
