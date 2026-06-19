/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.selector;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.EnumTrait;
import software.amazon.smithy.model.traits.synthetic.SyntheticEnumTrait;

/**
 * Specialized selector for the extremely common {@code [trait|X]} existence check.
 */
final class TraitExistenceSelector implements InternalSelector {

    private final ShapeId traitId;

    TraitExistenceSelector(ShapeId traitId) {
        this.traitId = traitId;
    }

    @Override
    public Response push(Context context, Shape shape, Receiver next) {
        return shape.hasTrait(traitId)
                ? next.apply(context, shape)
                : Response.CONTINUE;
    }

    @Override
    public Collection<? extends Shape> getStartingShapes(Model model) {
        Set<Shape> shapes = model.getShapesWithTrait(traitId);
        // Enum shapes carry the synthetic enum trait rather than smithy.api#enum, so the trait index does not list
        // them under smithy.api#enum. Include them when matching on the enum trait so this starting-shape
        // optimization doesn't skip every enum shape (which their hasTrait check would otherwise match).
        if (traitId.equals(EnumTrait.ID)) {
            Set<Shape> enumShapes = model.getShapesWithTrait(SyntheticEnumTrait.ID);
            if (!enumShapes.isEmpty()) {
                Set<Shape> combined = new LinkedHashSet<>(shapes);
                combined.addAll(enumShapes);
                return combined;
            }
        }
        return shapes;
    }

    @Override
    public ContainsShape containsShapeOptimization(Context context, Shape shape) {
        return shape.hasTrait(traitId) ? ContainsShape.YES : ContainsShape.NO;
    }

    @Override
    public ContainsShape emitsAnyOptimization(Context context, Shape input) {
        return input.hasTrait(traitId) ? ContainsShape.YES : ContainsShape.NO;
    }
}
