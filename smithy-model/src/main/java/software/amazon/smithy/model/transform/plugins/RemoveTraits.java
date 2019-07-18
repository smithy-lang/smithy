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

package software.amazon.smithy.model.transform.plugins;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.AbstractShapeBuilder;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.TraitDefinition;
import software.amazon.smithy.model.transform.ModelTransformer;
import software.amazon.smithy.model.transform.ModelTransformerPlugin;

/**
 * Removes traits from shapes when trait definitions are removed.
 */
public final class RemoveTraits implements ModelTransformerPlugin {
    private static final Logger LOGGER = Logger.getLogger(RemoveTraits.class.getName());

    @Override
    public Model onRemove(ModelTransformer transformer, Collection<Shape> shapes, Model model) {
        // Find all shapes with the "@trait" trait to ensure references to it are removed
        // from other shapes.
        Set<ShapeId> removedTraits = shapes.stream()
                .filter(shape -> shape.hasTrait(TraitDefinition.class))
                .map(Shape::getId)
                .collect(Collectors.toSet());

        if (removedTraits.isEmpty()) {
            return model;
        } else {
            LOGGER.fine(() -> String.format("Detected the following removed traits: %s", removedTraits));
            return transformer.replaceShapes(model, determineShapesToUpdate(model, removedTraits));
        }
    }

    private List<Shape> determineShapesToUpdate(Model model, Set<ShapeId> removedTraits) {
        List<Shape> shapes = model.getShapeIndex().shapes()
                .filter(shape -> !removedTraits.contains(shape.getId()))
                .filter(shape -> isShapeInNeedOfUpdate(shape, removedTraits))
                .map(shape -> removeTraitsFromShape(shape, removedTraits))
                .collect(Collectors.toList());

        if (!shapes.isEmpty()) {
            LOGGER.fine(() -> String.format(
                    "Replacing shapes %s that need the following traits removed: %s", shapes, removedTraits));
        }

        return shapes;
    }

    private boolean isShapeInNeedOfUpdate(Shape shape, Set<ShapeId> removedTraits) {
        return shape.getAllTraits().keySet().stream().anyMatch(removedTraits::contains);
    }

    private Shape removeTraitsFromShape(Shape shape, Set<ShapeId> removedTraits) {
        AbstractShapeBuilder<?, ?> builder = Shape.shapeToBuilder(shape);
        for (ShapeId id : removedTraits) {
            builder.removeTrait(id);
        }

        return builder.build();
    }
}
