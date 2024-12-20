/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.transform;

import java.util.ArrayList;
import java.util.List;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.MixinTrait;

/**
 * Flattens mixins out of the model.
 */
final class FlattenAndRemoveMixins {
    Model transform(ModelTransformer transformer, Model model) {
        List<Shape> updatedShapes = new ArrayList<>();
        List<ShapeId> toRemove = new ArrayList<>();

        for (Shape shape : model.toSet()) {
            if (shape.hasTrait(MixinTrait.class)) {
                toRemove.add(shape.getId());
            } else if (!shape.getMixins().isEmpty()) {
                updatedShapes.add(Shape.shapeToBuilder(shape).flattenMixins().build());
            }
        }

        if (!updatedShapes.isEmpty() || !toRemove.isEmpty()) {
            Model.Builder builder = model.toBuilder();
            updatedShapes.forEach(builder::addShape);
            // Don't use the removeShapes transform because that would further mutate shapes and remove the things
            // that were just flattened into the shapes. It's safe to just remove mixin shapes here.
            toRemove.forEach(builder::removeShape);
            model = builder.build();
        }

        return model;
    }
}
