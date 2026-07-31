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

    @Override
    public ContainsShape emitsAnyOptimization(Context context, Shape input) {
        // `:test` emits the input iff any child emits. A definite YES or NO is side-effect-free by contract, so
        // fall back to a full push only when a child cannot answer without running.
        if (selectors.size() == 1) {
            return selectors.get(0).emitsAnyOptimization(context, input);
        }

        for (InternalSelector predicate : selectors) {
            switch (predicate.emitsAnyOptimization(context, input)) {
                case YES:
                    return ContainsShape.YES;
                case MAYBE:
                    return ContainsShape.MAYBE;
                case NO:
                default:
                    break;
            }
        }

        return ContainsShape.NO;
    }

    @Override
    public boolean isOutputSubsetOfInput() {
        return true;
    }
}
