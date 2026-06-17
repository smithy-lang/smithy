/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.selector;

import java.util.Collection;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Specialized selector for the extremely common {@code [trait|X]} existence check.
 */
final class TraitExistenceSelector implements InternalSelector {

    private final ShapeId traitId;

    TraitExistenceSelector(ShapeId traitId) {
        this.traitId = traitId;
    }

    @Override
    public Response push(Context context, Shape shape, Receiver next) {
        return shape.hasTrait(traitId)
                ? next.apply(context, shape)
                : Response.CONTINUE;
    }

    @Override
    public Collection<? extends Shape> getStartingShapes(Model model) {
        return model.getShapesWithTrait(traitId);
    }

    @Override
    public ContainsShape containsShapeOptimization(Context context, Shape shape) {
        return shape.hasTrait(traitId) ? ContainsShape.YES : ContainsShape.NO;
    }
}
