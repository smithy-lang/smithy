/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
