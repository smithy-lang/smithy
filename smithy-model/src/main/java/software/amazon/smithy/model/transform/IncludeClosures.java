/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.transform;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.ShapeClosureIndex;
import software.amazon.smithy.model.node.ExpectationNotMetException;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Filters the model down to the shapes in one or more {@code shapeClosures}.
 *
 * @see ModelTransformer#includeClosures
 */
final class IncludeClosures {
    private final Set<String> closures;

    IncludeClosures(Collection<String> closures) {
        this.closures = new LinkedHashSet<>(closures);
    }

    Model transform(ModelTransformer transformer, Model model) {
        ShapeClosureIndex index = ShapeClosureIndex.of(model);

        Set<ShapeId> kept = new HashSet<>();
        for (String closure : closures) {
            try {
                for (Shape shape : index.getShapesInClosure(closure)) {
                    kept.add(shape.getId());
                }
            } catch (ExpectationNotMetException e) {
                throw new ModelTransformException(e.getMessage(), e);
            }
        }

        return transformer.filterShapes(model, shape -> kept.contains(shape.getId()));
    }
}
