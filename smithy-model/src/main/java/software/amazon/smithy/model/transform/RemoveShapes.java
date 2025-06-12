/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.transform;

import static java.lang.String.format;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Removes shapes from a model while ensuring that relationships to/from
 * the shape are cleaned up.
 *
 * <p>When a list, set, map, structure, or union are removed,
 * their member shapes are also removed.
 *
 * <p><strong>Important</strong>: the removal of list, set, and map members
 * without removing their containers will leave the model in an inconsistent
 * state. The appropriate way to remove these shapes is to remove their
 * containing shape rather than the member.
 */
final class RemoveShapes {
    private static final Logger LOGGER = Logger.getLogger(RemoveShapes.class.getName());

    private final Collection<? extends Shape> toRemove;
    private final List<ModelTransformerPlugin> plugins;

    RemoveShapes(Collection<? extends Shape> toRemove, List<ModelTransformerPlugin> plugins) {
        this.toRemove = toRemove;
        this.plugins = plugins;
    }

    Model transform(ModelTransformer transformer, Model model) {
        Model.Builder builder = model.toBuilder();

        // Iteratively add each shape that needs to be removed from the index using multiple rounds.
        Set<Shape> removed = new HashSet<>(toRemove);
        for (Shape removedShape : toRemove) {
            validateShapeCopiedFromMixin(model, removedShape);
            builder.removeShape(removedShape.getId());
            // We don't need to remove members from the builder since
            // members are automatically removed with the container.
            removed.addAll(removedShape.members());
        }

        Model result = builder.build();

        for (ModelTransformerPlugin plugin : plugins) {
            result = plugin.onRemove(transformer, removed, result);
        }

        return result;
    }

    private void validateShapeCopiedFromMixin(Model model, Shape shape) {
        if (!shape.isMemberShape()) {
            return;
        }
        MemberShape memberShape = shape.asMemberShape().get();
        Shape container = model.getShape(memberShape.getContainer())
                .orElseThrow(() -> new ModelTransformException(
                        format("Cannot find the container shape for member `%s`.",
                                memberShape.getMemberName())));
        for (ShapeId mixinId : container.getMixins()) {
            Shape mixinShape = model.expectShape(mixinId);
            if (mixinShape.getMemberNames().contains(memberShape.getMemberName())
                    && !toRemove.contains(mixinShape)) {
                LOGGER.warning(format("Removing mixed in member `%s` from mixin shape `%s` "
                        + "in `%s` will result in an inconsistent model.",
                        memberShape.getMemberName(),
                        mixinShape.getId(),
                        container.getId().getName()));
            }
        }
    }
}
