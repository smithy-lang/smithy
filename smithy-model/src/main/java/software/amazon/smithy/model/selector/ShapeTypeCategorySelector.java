/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.selector;

import java.util.Collection;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;

final class ShapeTypeCategorySelector implements InternalSelector {
    private final Class<? extends Shape> shapeCategory;

    ShapeTypeCategorySelector(Class<? extends Shape> shapeCategory) {
        this.shapeCategory = shapeCategory;
    }

    @Override
    public Response push(Context ctx, Shape shape, Receiver next) {
        if (shapeCategory.isInstance(shape)) {
            return next.apply(ctx, shape);
        }

        return Response.CONTINUE;
    }

    @Override
    public Collection<? extends Shape> getStartingShapes(Model model) {
        return model.toSet(shapeCategory);
    }

    @Override
    public ContainsShape containsShapeOptimization(Context context, Shape shape) {
        return getStartingShapes(context.getModel()).contains(shape) ? ContainsShape.YES : ContainsShape.NO;
    }
}
