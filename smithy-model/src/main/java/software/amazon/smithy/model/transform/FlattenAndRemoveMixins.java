/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.model.transform;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.model.traits.MixinTrait;

/**
 * Flattens mixins out of the model.
 */
final class FlattenAndRemoveMixins {
    Model transform(ModelTransformer transformer, Model model) {
        List<Shape> updatedShapes = new ArrayList<>();
        List<Shape> toRemove = new ArrayList<>();

        Stream.concat(model.shapes(StructureShape.class), model.shapes(UnionShape.class)).forEach(shape -> {
            if (shape.hasTrait(MixinTrait.class)) {
                toRemove.add(shape);
            } else if (!shape.getMixins().isEmpty()) {
                updatedShapes.add(Shape.shapeToBuilder(shape).flattenMixins().build());
            }
        });

        if (!updatedShapes.isEmpty()) {
            Model.Builder builder = model.toBuilder();
            updatedShapes.forEach(builder::addShape);
            model = builder.build();
        }

        if (!toRemove.isEmpty()) {
            model = transformer.removeShapes(model, toRemove);
        }

        return model;
    }
}
