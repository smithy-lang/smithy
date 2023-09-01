/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.model.transform;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.DefaultTrait;
import software.amazon.smithy.model.traits.RangeTrait;

/**
 * Removes default values from shapes where the default value is incompatible with
 * the constraint traits of the shape.
 */
final class RemoveInvalidDefaults {

    private static final Logger LOGGER = Logger.getLogger(RemoveInvalidDefaults.class.getName());

    Model transform(ModelTransformer transformer, Model model) {
        Set<Shape> invalidDefaults = new HashSet<>();
        Set<Shape> updates = new HashSet<>();

        // First collect invalid shapes. Members with invalid defaults either need to remove the default or
        // set the default to null if their target shape's default remains intact but the member is invalid.
        for (Shape shape : model.getShapesWithTrait(DefaultTrait.class)) {
            shape.getMemberTrait(model, RangeTrait.class).ifPresent(rangeTrait -> {
                DefaultTrait defaultTrait = shape.expectTrait(DefaultTrait.class);
                if (defaultTrait.toNode().isNumberNode()) {
                    defaultTrait.toNode().expectNumberNode().asBigDecimal().ifPresent(value -> {
                        if (rangeTrait.getMin().filter(min -> value.compareTo(min) < 0).isPresent()
                                || rangeTrait.getMin().filter(max -> value.compareTo(max) > 0).isPresent()) {
                            invalidDefaults.add(shape);
                        }
                    });
                }
            });
        }

        for (Shape shape : invalidDefaults) {
            updates.add(modify(shape, model, invalidDefaults));
        }

        return transformer.replaceShapes(model, updates);
    }

    private Shape modify(Shape shape, Model model, Set<Shape> otherShapes) {
        // To show up here, the shape has to have a range trait, or the target has to have one.
        RangeTrait rangeTrait = shape.getMemberTrait(model, RangeTrait.class).get();
        LOGGER.info(() -> "Removing default trait from " + shape.getId()
                          + " because of an incompatible range trait: "
                          + Node.printJson(rangeTrait.toNode()));

        // Members that target a shape with a default value need to set their default to null to override it.
        // Other members and other shapes can simply remove the default trait.
        if (shape.isMemberShape()) {
            MemberShape member = shape.asMemberShape().get();
            boolean targetHasDefault = model.getShape(member.getTarget())
                    // Treat target shapes that will have their default removed as if it doesn't have a default.
                    .filter(target -> !otherShapes.contains(target) && target.hasTrait(DefaultTrait.class))
                    .isPresent();
            if (targetHasDefault) {
                return member.toBuilder().addTrait(new DefaultTrait(Node.nullNode())).build();
            }
        }

        return Shape.shapeToBuilder(shape).removeTrait(DefaultTrait.ID).build();
    }
}
