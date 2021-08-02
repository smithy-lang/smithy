/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
    private final Collection<Shape> toRemove;
    private final List<ModelTransformerPlugin> plugins;

    RemoveShapes(Collection<Shape> toRemove, List<ModelTransformerPlugin> plugins) {
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
