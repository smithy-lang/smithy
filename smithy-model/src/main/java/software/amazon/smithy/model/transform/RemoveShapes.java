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

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.SetShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeIndex;
import software.amazon.smithy.model.shapes.ShapeVisitor;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.utils.ListUtils;

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
        Set<Shape> removed = new HashSet<>();
        ShapeIndex index = model.getShapeIndex();
        Deque<Shape> queue = index.shapes()
                .filter(toRemove::contains)
                .collect(Collectors.toCollection(ArrayDeque::new));
        ShapeRemovalVisitor removalVisitor = new ShapeRemovalVisitor();

        // Iteratively add each shape that needs to be removed from the index using multiple rounds.
        while (!queue.isEmpty()) {
            Shape shape = queue.pop();
            if (!removed.contains(shape)) {
                queue.addAll(shape.accept(removalVisitor));
            }
            removed.add(shape);
        }

        ShapeIndex.Builder builder = index.toBuilder();
        removed.forEach(shape -> builder.removeShape(shape.getId()));
        Model result = model.toBuilder().shapeIndex(builder.build()).build();

        for (ModelTransformerPlugin plugin : plugins) {
            result = plugin.onRemove(transformer, removed, result);
        }

        return result;
    }

    /**
     * This visitor returns a shallow list of shapes that need to be
     * removed when a shape is removed.
     */
    static final class ShapeRemovalVisitor extends ShapeVisitor.Default<Collection<? extends Shape>> {

        @Override
        public Collection<? extends Shape> getDefault(Shape shape) {
            return ListUtils.of();
        }

        @Override
        public Collection<? extends Shape> listShape(ListShape shape) {
            return ListUtils.of(shape.getMember());
        }

        @Override
        public Collection<? extends Shape> setShape(SetShape shape) {
            return ListUtils.of(shape.getMember());
        }

        @Override
        public Collection<? extends Shape> mapShape(MapShape shape) {
            return ListUtils.of(shape.getKey(), shape.getValue());
        }

        @Override
        public Collection<? extends Shape> structureShape(StructureShape shape) {
            return shape.getAllMembers().values();
        }

        @Override
        public Collection<? extends Shape> unionShape(UnionShape shape) {
            return shape.getAllMembers().values();
        }
    }
}
