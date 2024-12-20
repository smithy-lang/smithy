/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.transform;

import java.util.List;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.Trait;

/**
 * Filters traits out of the Model that do not match a predicate that
 * accepts the shape that the trait is attached to and the trait.
 *
 * <p>Shapes are only modified and replaced into the model if one of their
 * traits are modified.
 *
 * @see ModelTransformer#filterTraits(Model, BiPredicate)
 */
final class FilterTraits {
    private final BiPredicate<Shape, Trait> predicate;

    FilterTraits(BiPredicate<Shape, Trait> predicate) {
        this.predicate = predicate;
    }

    Model transform(ModelTransformer transformer, Model model) {
        return transformer.mapShapes(model, this::filterTraits);
    }

    private Shape filterTraits(Shape shape) {
        List<Trait> keepTraits = shape.getAllTraits()
                .values()
                .stream()
                .filter(trait -> predicate.test(shape, trait))
                .collect(Collectors.toList());

        return keepTraits.size() == shape.getAllTraits().size()
                ? shape
                : Shape.shapeToBuilder(shape).clearTraits().addTraits(keepTraits).build();
    }
}
