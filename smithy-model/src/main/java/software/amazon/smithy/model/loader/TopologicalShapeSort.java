/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Topologically sorts shapes based on their dependencies (i.e., mixins).
 *
 * <p>While this class is reusable, it is also stateful; shapes and edges
 * are enqueued, and when sorted, all shapes and edges are dequeued.
 */
public final class TopologicalShapeSort {

    private final Map<ShapeId, Set<ShapeId>> forwardDependencies = new HashMap<>();
    private final Map<ShapeId, Set<ShapeId>> reverseDependencies = new HashMap<>();
    private final Deque<ShapeId> satisfiedShapes;

    public TopologicalShapeSort() {
        this(100);
    }

    TopologicalShapeSort(int ensureCapacity) {
        satisfiedShapes = new ArrayDeque<>(ensureCapacity);
    }

    /**
     * Add a shape to the sort queue, and automatically extract dependencies.
     *
     * @param shape Shape to add.
     */
    public void enqueue(Shape shape) {
        enqueue(shape.getId(), shape.getMixins());
    }

    /**
     * Add a shape to the sort queue, and provide an explicit dependencies list.
     *
     * @param shape Shape to add.
     * @param dependencies Dependencies of the shape.
     */
    public void enqueue(ShapeId shape, Collection<ShapeId> dependencies) {
        if (dependencies.isEmpty()) {
            satisfiedShapes.offer(shape);
        } else {
            for (ShapeId dependent : dependencies) {
                reverseDependencies.computeIfAbsent(dependent, unused -> new HashSet<>()).add(shape);
            }
            forwardDependencies.put(shape, new HashSet<>(dependencies));
        }
    }

    /**
     * Sort the shapes and returns the ordered list of shape IDs.
     *
     * @return Returns the topologically sorted list of shape IDs.
     * @throws CycleException if cycles exist between shapes.
     */
    public List<ShapeId> dequeueSortedShapes() {
        List<ShapeId> result = new ArrayList<>(satisfiedShapes.size() + forwardDependencies.size());

        while (!satisfiedShapes.isEmpty()) {
            ShapeId current = satisfiedShapes.poll();
            forwardDependencies.remove(current);
            result.add(current);

            for (ShapeId dependent : reverseDependencies.getOrDefault(current, Collections.emptySet())) {
                Set<ShapeId> dependentDependencies = forwardDependencies.get(dependent);
                dependentDependencies.remove(current);
                if (dependentDependencies.isEmpty()) {
                    satisfiedShapes.offer(dependent);
                }
            }
        }

        reverseDependencies.clear();

        if (!forwardDependencies.isEmpty()) {
            throw new CycleException(new TreeSet<>(forwardDependencies.keySet()), result);
        }

        return result;
    }

    /**
     * Thrown when cycles exist between shapes.
     */
    public static final class CycleException extends RuntimeException {
        private final Set<ShapeId> unresolved;
        private final List<ShapeId> resolved;

        public CycleException(Set<ShapeId> unresolved, List<ShapeId> resolved) {
            super("Mixin cycles detected among " + unresolved);
            this.unresolved = unresolved;
            this.resolved = resolved;
        }

        /**
         * Gets the entire set of shapes that could not be resolved.
         *
         * @return Returns the set of unresolved shapes.
         */
        public Set<ShapeId> getUnresolved() {
            return unresolved;
        }

        /**
         * @return Returns the set of resolved shapes.
         */
        public List<ShapeId> getResolved() {
            return resolved;
        }
    }
}
