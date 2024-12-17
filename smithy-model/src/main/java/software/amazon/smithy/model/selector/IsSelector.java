/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.selector;

import java.util.List;
import software.amazon.smithy.model.shapes.Shape;

/**
 * Maps input over each function and returns the concatenated result.
 */
final class IsSelector implements InternalSelector {
    private final List<InternalSelector> selectors;

    private IsSelector(List<InternalSelector> predicates) {
        this.selectors = predicates;
    }

    static InternalSelector of(List<InternalSelector> predicates) {
        return predicates.size() == 1 ? predicates.get(0) : new IsSelector(predicates);
    }

    @Override
    public Response push(Context context, Shape shape, Receiver next) {
        for (InternalSelector selector : selectors) {
            if (selector.push(context, shape, next) == Response.STOP) {
                return Response.STOP;
            }
        }

        return Response.CONTINUE;
    }
}
