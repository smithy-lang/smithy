/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.transform;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;

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
}
