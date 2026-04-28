/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.selector;

import java.util.Collection;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeType;

/**
 * Matches shapes whose {@link ShapeType.Category} equals a given category.
 *
 * <p>This complements {@link ShapeTypeCategorySelector}, which keys off of a shared
 * Java base class. A class-based check is not possible for the AGGREGATE and SERVICE
 * categories because their shape types do not share a common Java superclass.
 */
final class ShapeTypeCategoryEnumSelector implements InternalSelector {
    private final ShapeType.Category category;

    ShapeTypeCategoryEnumSelector(ShapeType.Category category) {
        this.category = category;
    }

    @Override
    public Response push(Context ctx, Shape shape, Receiver next) {
        if (shape.getType().getCategory() == category) {
            return next.apply(ctx, shape);
        }
        return Response.CONTINUE;
    }

    @Override
    public Collection<? extends Shape> getStartingShapes(Model model) {
        return model.toSet(category);
    }

    @Override
    public ContainsShape containsShapeOptimization(Context context, Shape shape) {
        return shape.getType().getCategory() == category ? ContainsShape.YES : ContainsShape.NO;
    }
}
