/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.transform;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.AbstractShapeBuilder;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.MixinTrait;

/**
 * Flattens mixins out of the model.
 *
 * <p>Interface mixins (those with {@code interface = true}) are preserved in the model
 * and their references are maintained on shapes that use them, while non-interface
 * mixins are removed as before.
 */
final class FlattenAndRemoveMixins {
    Model transform(ModelTransformer transformer, Model model) {
        Set<ShapeId> interfaceMixinIds = new HashSet<>();
        for (Shape shape : model.toSet()) {
            if (MixinTrait.isInterfaceMixin(shape)) {
                interfaceMixinIds.add(shape.getId());
            }
        }

        List<Shape> updatedShapes = new ArrayList<>();
        List<Shape> toRemove = new ArrayList<>();

        for (Shape shape : model.toSet()) {
            if (shape.hasTrait(MixinTrait.ID) && !interfaceMixinIds.contains(shape.getId())) {
                // Non-interface mixin: remove from model
                toRemove.add(shape);
            } else if (!shape.getMixins().isEmpty()) {
                // Shape has mixins (either an interface mixin with parents, or a concrete shape).
                // Flatten all mixins, then re-add references to effective interface mixins.
                Set<ShapeId> effectiveInterfaceMixins = collectEffectiveInterfaceMixins(
                        shape,
                        model,
                        interfaceMixinIds);
                if (effectiveInterfaceMixins.isEmpty() && !shape.hasTrait(MixinTrait.ID)) {
                    // Simple case: no interface mixins involved, just flatten everything
                    updatedShapes.add(Shape.shapeToBuilder(shape).flattenMixins().build());
                } else if (!effectiveInterfaceMixins.isEmpty()) {
                    updatedShapes.add(flattenAndPreserveInterfaceRefs(
                            shape,
                            model,
                            effectiveInterfaceMixins));
                }
            }
        }

        if (!updatedShapes.isEmpty() || !toRemove.isEmpty()) {
            Model.Builder builder = model.toBuilder();
            updatedShapes.forEach(builder::addShape);
            toRemove.forEach(s -> builder.removeShape(s.getId()));
            model = builder.build();
        }
        return model;
    }

    /**
     * Collect the interface mixin IDs that should be preserved as references on this shape.
     * Direct interface mixins are kept as-is; non-interface mixins are walked to find
     * transitive interface ancestors.
     */
    private Set<ShapeId> collectEffectiveInterfaceMixins(
            Shape shape,
            Model model,
            Set<ShapeId> interfaceMixinIds
    ) {
        Set<ShapeId> result = new LinkedHashSet<>();
        for (ShapeId mixinId : shape.getMixins()) {
            if (interfaceMixinIds.contains(mixinId)) {
                result.add(mixinId);
            } else {
                collectTransitiveInterfaceMixins(model, model.expectShape(mixinId), interfaceMixinIds, result);
            }
        }
        return result;
    }

    /**
     * Flatten a shape's mixins and re-add references to the given interface mixins.
     * Traits that will be re-inherited from the interface mixins are removed from the
     * builder's introduced traits so they don't appear double-counted.
     */
    private Shape flattenAndPreserveInterfaceRefs(
            Shape shape,
            Model model,
            Set<ShapeId> interfaceMixinIds
    ) {
        AbstractShapeBuilder<?, ?> builder = Shape.shapeToBuilder(shape);
        Set<ShapeId> introducedTraitIds = shape.getIntroducedTraits().keySet();
        builder.flattenMixins();

        // Remove traits that will be re-inherited, then re-add the mixin refs
        for (ShapeId ifaceId : interfaceMixinIds) {
            Shape ifaceMixin = model.expectShape(ifaceId);
            for (ShapeId traitId : MixinTrait.getNonLocalTraitsFromMap(ifaceMixin.getAllTraits()).keySet()) {
                if (!introducedTraitIds.contains(traitId)) {
                    builder.removeTrait(traitId);
                }
            }
            builder.addMixinRef(ifaceMixin);
        }

        return builder.build();
    }

    private void collectTransitiveInterfaceMixins(
            Model model,
            Shape shape,
            Set<ShapeId> interfaceMixinIds,
            Set<ShapeId> result
    ) {
        for (ShapeId parentId : shape.getMixins()) {
            if (interfaceMixinIds.contains(parentId)) {
                result.add(parentId);
            } else {
                collectTransitiveInterfaceMixins(model, model.expectShape(parentId), interfaceMixinIds, result);
            }
        }
    }
}
