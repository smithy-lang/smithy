/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.selector;

import java.util.Collection;
import java.util.List;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.Trait;

/**
 * Specialized selector for the extremely common {@code [trait|X]} existence check.
 */
final class TraitExistenceSelector implements InternalSelector {

    private final ShapeId traitId;

    TraitExistenceSelector(ShapeId traitId) {
        this.traitId = traitId;
    }

    /**
     * Creates a TraitExistenceSelector if the path represents a simple {@code [trait|X]} existence check,
     * otherwise returns null.
     *
     * @param path The attribute path parsed from the selector.
     * @return A TraitExistenceSelector if applicable, otherwise null.
     */
    static TraitExistenceSelector tryCreate(List<String> path) {
        if (path.size() == 2
                && path.get(0).equals("trait")
                && !path.get(1).startsWith("(")) {
            ShapeId traitId = ShapeId.from(Trait.makeAbsoluteName(path.get(1)));
            return new TraitExistenceSelector(traitId);
        }
        return null;
    }

    @Override
    public Response push(Context context, Shape shape, Receiver next) {
        if (shape.hasTrait(traitId)) {
            return next.apply(context, shape);
        }
        return Response.CONTINUE;
    }

    @Override
    public Collection<? extends Shape> getStartingShapes(Model model) {
        return model.getShapesWithTrait(traitId);
    }

    @Override
    public ContainsShape containsShapeOptimization(Context context, Shape shape) {
        return shape.hasTrait(traitId) ? ContainsShape.YES : ContainsShape.NO;
    }
}
