/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.selector;

import java.util.List;
import software.amazon.smithy.model.shapes.Shape;

/**
 * Filters out shapes that do not match any predicates.
 *
 * <p>The result of this selector is always a subset of the input
 * (i.e., it does not map over the input).
 */
final class TestSelector implements InternalSelector {
    private final List<InternalSelector> selectors;

    TestSelector(List<InternalSelector> selectors) {
        this.selectors = selectors;
    }

    @Override
    public Response push(Context context, Shape shape, Receiver next) {
        for (InternalSelector predicate : selectors) {
            if (context.receivedShapes(shape, predicate)) {
                // The instant something matches, stop testing selectors.
                return next.apply(context, shape);
            }
        }

        // Continue to receive shapes because other shapes could match.
        return Response.CONTINUE;
    }
}
