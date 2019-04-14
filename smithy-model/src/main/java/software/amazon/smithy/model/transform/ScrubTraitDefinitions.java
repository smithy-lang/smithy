/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.Prelude;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeIndex;
import software.amazon.smithy.model.traits.TraitDefinition;
import software.amazon.smithy.utils.OptionalUtils;
import software.amazon.smithy.utils.Pair;

/**
 * Removes all trait definitions from a model and all shapes that are only
 * connected to the graph by a removed trait definition shape.
 *
 * <p>While all shapes in the prelude that are marked as private are
 * removed, no public shapes in the prelude are ever removed.
 *
 * <p>This can be useful when serializing a Smithy model to a format that
 * does not include trait definitions and the shapes used by trait definitions
 * would have no meaning (e.g., Swagger).
 *
 * @see ModelTransformer#scrubTraitDefinitions
 */
final class ScrubTraitDefinitions {
    Model transform(ModelTransformer transformer, Model model) {
        ShapeIndex index = model.getShapeIndex();
        // Find all shape to TraitDefinition groupings.
        Map<Shape, List<TraitDefinition>> remainingDefinitions = model.getTraitDefinitions().stream()
                .flatMap(def -> OptionalUtils.stream(def.getShape().flatMap(index::getShape)
                        .map(shape -> Pair.of(def, shape))))
                .collect(Collectors.groupingBy(
                        Pair::getRight,
                        Collectors.mapping(Pair::getLeft, Collectors.toList())));

        MarkAndSweep markAndSweep = new MarkAndSweep(
                // Mark shapes for removal that are only referenced by a trait definition.
                context -> {
                    Set<Shape> traitShapes = new HashSet<>(remainingDefinitions.keySet());
                    traitShapes.forEach(shape -> {
                        Set<Shape> targetedFrom = context.getTargetedFrom(shape);
                        targetedFrom.removeAll(context.getMarkedForRemoval());
                        if (targetedFrom.isEmpty()) {
                            context.markShape(shape);
                            remainingDefinitions.remove(shape);
                        }
                    });
                },
                // Don't remove public prelude shapes.
                ScrubTraitDefinitions::notPublicPreludeShape);

        Model result = transformer.removeShapes(model, markAndSweep.markAndSweep(model));

        return result.toBuilder().clearTraitDefinitions().build();
    }

    private static boolean notPublicPreludeShape(Shape shape) {
        return !Prelude.isPublicPreludeShape(shape.getId());
    }
}
