/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.transform;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.function.BiFunction;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.Trait;

/**
 * Maps over traits in a model using a mapping function.
 *
 * <p>Shapes are only transformed and replaced in the model if one
 * of its traits are modified.
 *
 * @see ModelTransformer#mapTraits
 */
final class MapTraits {
    private final BiFunction<Shape, Trait, Trait> mapper;

    MapTraits(BiFunction<Shape, Trait, Trait> mapper) {
        this.mapper = mapper;
    }

    Model transform(ModelTransformer transformer, Model model) {
        return transformer.mapShapes(model, this::mapTraits);
    }

    private Shape mapTraits(Shape shape) {
        Collection<Trait> traits = new ArrayList<>();
        boolean changed = false;

        // Map over the traits and see if any of them changed. If none
        // changed, then there is no need to create a new copy of the shape.
        for (Trait trait : shape.getAllTraits().values()) {
            Trait mapped = Objects.requireNonNull(mapper.apply(shape, trait), "Trait mapper must not return null");
            traits.add(mapped);
            if (!mapped.equals(trait)) {
                changed = true;
            }
        }

        if (!changed) {
            return shape;
        }

        return Shape.shapeToBuilder(shape).clearTraits().addTraits(traits).build();
    }
}
