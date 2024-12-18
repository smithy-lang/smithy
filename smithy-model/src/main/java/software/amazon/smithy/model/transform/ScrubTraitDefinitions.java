/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.transform;

import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.Prelude;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.PrivateTrait;
import software.amazon.smithy.model.traits.TraitDefinition;
import software.amazon.smithy.utils.FunctionalUtils;

/**
 * Removes trait definitions, excluding those retained using a keepFilter, from
 * a model and all shapes that are only connected to the graph by a removed
 * trait definition shape.
 *
 * <p>All shapes in the prelude marked as private are automatically removed,
 * and all prelude trait definitions are removed. However, no public prelude
 * shapes are ever removed.
 *
 * <p>This can be useful when serializing a Smithy model to a format that
 * does not include trait definitions and the shapes used by trait definitions
 * would have no meaning (e.g., OpenAPI).
 *
 * <p>TODO: Should there be an option to only remove private traits?
 *
 * @see ModelTransformer#scrubTraitDefinitions
 */
final class ScrubTraitDefinitions {
    Model transform(ModelTransformer transformer, Model model) {
        return transform(transformer, model, FunctionalUtils.alwaysTrue());
    }

    Model transform(ModelTransformer transformer, Model model, Predicate<Shape> keepFilter) {
        // Find all trait definition shapes, excluding those to be kept, and private shapes in the prelude.
        Set<Shape> toMark = Stream.concat(
                model.shapes().filter(shape -> isTraitDefinitionToRemove(shape, keepFilter)),
                model.shapes().filter(shape -> Prelude.isPreludeShape(shape) && shape.hasTrait(PrivateTrait.class)))
                .collect(Collectors.toSet());

        MarkAndSweep markAndSweep = new MarkAndSweep(
                // Mark shapes for removal that are private or remaining trait definitions.
                context -> {
                    toMark.forEach(context::markShape);
                    toMark.clear();
                },
                // Don't remove public prelude shapes.
                ScrubTraitDefinitions::notPublicPreludeShape);

        // Removing shapes that are traits automatically removes applications of that trait from other shapes.
        return transformer.removeShapes(model, markAndSweep.markAndSweep(model));
    }

    private static boolean notPublicPreludeShape(Shape shape) {
        return !(Prelude.isPublicPreludeShape(shape.getId()) && !shape.hasTrait(TraitDefinition.class));
    }

    private static boolean isTraitDefinitionToRemove(Shape shape, Predicate<Shape> keepFilter) {
        return shape.hasTrait(TraitDefinition.class) && keepFilter.test(shape);
    }
}
