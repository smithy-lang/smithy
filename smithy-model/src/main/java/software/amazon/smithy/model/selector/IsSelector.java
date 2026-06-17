/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.selector;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import software.amazon.smithy.model.Model;
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

    @Override
    public Collection<? extends Shape> getStartingShapes(Model model) {
        // If all children provide narrower starting shapes, use their union instead of all model shapes.
        Collection<? extends Shape> allShapes = model.toSet();
        Set<Shape> union = null;
        for (InternalSelector selector : selectors) {
            Collection<? extends Shape> starting = selector.getStartingShapes(model);
            if (starting == allShapes) {
                return allShapes;
            } else if (union == null) {
                union = new HashSet<>(starting);
            } else {
                union.addAll(starting);
            }
        }
        return union != null ? union : allShapes;
    }
}
