/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.selector;

import java.util.Collection;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeType;

final class ShapeTypeSelector implements InternalSelector {

    final ShapeType shapeType;

    ShapeTypeSelector(ShapeType shapeType) {
        this.shapeType = shapeType;
    }

    @Override
    public Response push(Context ctx, Shape shape, Receiver next) {
        if (shape.getType().isShapeType(shapeType)) {
            return next.apply(ctx, shape);
        }

        return Response.CONTINUE;
    }

    @Override
    public Collection<? extends Shape> getStartingShapes(Model model) {
        return model.toSet(shapeType.getShapeClass());
    }

    @Override
    public ContainsShape containsShapeOptimization(Context context, Shape shape) {
        return context.getModel().toSet(shapeType.getShapeClass()).contains(shape)
                ? ContainsShape.YES
                : ContainsShape.NO;
    }
}
