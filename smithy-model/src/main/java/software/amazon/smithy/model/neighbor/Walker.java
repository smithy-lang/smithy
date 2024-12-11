/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.neighbor;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Predicate;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.NeighborProviderIndex;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.FunctionalUtils;

/**
 * Walks connected shapes within a Model.
 *
 * <p>Any shape that is connected to another shape is "walked". A single
 * shape can have multiple relationships to the same shape. For example,
 * a resource can have both a "get" and a "child" relationship to an
 * operation; however, the referenced operation will appear only once
 * in the walker output.
 *
 * <p>Only shapes form a connected graph. Relationships created by traits
 * are not traversed by the walker.
 */
public final class Walker {

    private final NeighborProvider provider;

    /**
     * @param model Model to traverse.
     */
    public Walker(Model model) {
        this(NeighborProviderIndex.of(model).getProvider());
    }

    /**
     * @param provider Neighbor provider used to traverse relationships.
     */
    public Walker(NeighborProvider provider) {
        this.provider = provider;
    }

    /**
     * Walks connected shapes in the model, returning them in a set.
     *
     * @param shape The shape to start the traversal from.
     * @return Returns a set of connected shapes.
     */
    public Set<Shape> walkShapes(Shape shape) {
        return walkShapes(shape, FunctionalUtils.alwaysTrue());
    }

    /**
     * Walks connected shapes in the model (including the given shape),
     * and returns them in a set.
     *
     * @param shape The shape to start the traversal from.
     * @param predicate Predicate used to prevent traversing relationships.
     * @return Returns a set of connected shapes.
     */
    public Set<Shape> walkShapes(Shape shape, Predicate<Relationship> predicate) {
        Set<Shape> result = new LinkedHashSet<>();
        Iterator<Shape> shapeIterator = iterateShapes(shape, predicate);
        while (shapeIterator.hasNext()) {
            result.add(shapeIterator.next());
        }

        return result;
    }

    /**
     * Walks connected shapes in the model, returning their IDs in a set.
     *
     * @param shape The shape to start the traversal from.
     * @return Returns a set of connected shape IDs.
     */
    public Set<ShapeId> walkShapeIds(Shape shape) {
        return walkShapeIds(shape, FunctionalUtils.alwaysTrue());
    }

    /**
     * Walks connected shapes in the model (including the given shape),
     * and returns a set of shape IDs.
     *
     * @param shape The shape to start the traversal from.
     * @param predicate Predicate used to prevent traversing relationships.
     * @return Returns a set of connected shape IDs.
     */
    public Set<ShapeId> walkShapeIds(Shape shape, Predicate<Relationship> predicate) {
        Set<ShapeId> result = new LinkedHashSet<>();
        Iterator<Shape> shapeIterator = iterateShapes(shape, predicate);
        while (shapeIterator.hasNext()) {
            result.add(shapeIterator.next().getId());
        }

        return result;
    }

    /**
     * Lazily iterates over all of the relationships in the closure of
     * the given shape, including the given shape.
     *
     * @param shape Shape to find the closure of.
     * @return Returns an iterator of shapes connected to {@code shape}.
     */
    public Iterator<Shape> iterateShapes(Shape shape) {
        return iterateShapes(shape, FunctionalUtils.alwaysTrue());
    }

    /**
     * Lazily iterates over all of the unique shapes in the closure of
     * the given shape, including the given shape.
     *
     * @param shape Shape to find the closure of.
     * @param predicate Predicate used to short-circuit relationship branches.
     * @return Returns an iterator of shapes connected to {@code shape}.
     */
    public Iterator<Shape> iterateShapes(Shape shape, Predicate<Relationship> predicate) {
        return new ShapeIterator(shape, predicate, provider);
    }

    private static final class ShapeIterator implements Iterator<Shape> {
        private final Predicate<Relationship> predicate;
        private final Deque<Relationship> stack = new ArrayDeque<>();
        private final Set<ShapeId> traversed = new HashSet<>();
        private final NeighborProvider provider;
        private Shape queued;

        ShapeIterator(Shape shape, Predicate<Relationship> predicate, NeighborProvider provider) {
            this.predicate = predicate;
            this.provider = provider;

            // Always include the given shape in the results.
            queued = shape;
            traversed.add(shape.getId());
            pushNeighbors(provider.getNeighbors(shape));
        }

        @Override
        public boolean hasNext() {
            if (queued != null) {
                return true;
            }

            while (!stack.isEmpty()) {
                // Every relationship is returned, even if the same shape is pointed
                // to multiple times from a single shape.
                Relationship relationship = stack.pop();

                // Only traverse this relationship if the shape it points to hasn't
                // already been traversed.
                if (traversed.add(relationship.getNeighborShapeId())) {
                    queued = relationship.getNeighborShape().get();
                    pushNeighbors(provider.getNeighbors(queued));
                    return true;
                }
            }

            return false;
        }

        @Override
        public Shape next() {
            if (queued == null) {
                throw new NoSuchElementException("No relationships in relationship iterator stack");
            }

            Shape shape = queued;
            queued = null;
            return shape;
        }

        private void pushNeighbors(List<Relationship> relationships) {
            for (Relationship rel : relationships) {
                // Only look at valid relationships that pass the predicate.
                if (rel.getNeighborShape().isPresent() && predicate.test(rel)) {
                    stack.push(rel);
                }
            }
        }
    }
}
