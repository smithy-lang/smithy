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

package software.amazon.smithy.model.loader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Topologically sorts shapes based on their dependencies (i.e., mixins).
 *
 * <p>While this class is reusable, is is also stateful; shapes and edges
 * are enqueued, and when sorted, all shapes and edges are dequeued.
 */
public final class TopologicalShapeSort {

    private final Map<ShapeId, Set<ShapeId>> forwardDependencies = new HashMap<>();
    private final Queue<ShapeId> satisfiedShapes = new LinkedList<>();

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
        forwardDependencies.put(shape, new LinkedHashSet<>(dependencies));
    }

    /**
     * Sort the shapes and returns the ordered list of shape IDs.
     *
     * @return Returns the topologically sorted list of shape IDs.
     * @throws CycleException if cycles exist between shapes.
     */
    public List<ShapeId> dequeueSortedShapes() {
        Map<ShapeId, Set<ShapeId>> reverseDependencies = new HashMap<>();

        for (Map.Entry<ShapeId, Set<ShapeId>> entry : forwardDependencies.entrySet()) {
            if (entry.getValue().isEmpty()) {
                satisfiedShapes.offer(entry.getKey());
            } else {
                for (ShapeId dependent : entry.getValue()) {
                    reverseDependencies.computeIfAbsent(dependent, unused -> new HashSet<>()).add(entry.getKey());
                }
            }
        }

        return topologicalSort(reverseDependencies);
    }

    private List<ShapeId> topologicalSort(Map<ShapeId, Set<ShapeId>> reverseDependencies) {
        List<ShapeId> result = new ArrayList<>();

        while (!satisfiedShapes.isEmpty()) {
            ShapeId current = satisfiedShapes.poll();
            forwardDependencies.remove(current);
            result.add(current);

            for (ShapeId dependent : reverseDependencies.getOrDefault(current, Collections.emptySet())) {
                Set<ShapeId> dependentDependencies = forwardDependencies.get(dependent);
                dependentDependencies.remove(current);
                if (dependentDependencies.isEmpty()) {
                    satisfiedShapes.add(dependent);
                }
            }
        }

        if (!forwardDependencies.isEmpty()) {
            throw new CycleException(new TreeSet<>(forwardDependencies.keySet()));
        }

        return result;
    }

    /**
     * Thrown when cycles exist between shapes.
     */
    public static final class CycleException extends RuntimeException {
        private final Set<ShapeId> unresolved;

        public CycleException(Set<ShapeId> unresolved) {
            super("Mixin cycles detected among " + unresolved);
            this.unresolved = unresolved;
        }

        /**
         * Gets the entire set of shapes that could not be resolved.
         *
         * @return Returns the set of unresolved shapes.
         */
        public Set<ShapeId> getUnresolved() {
            return unresolved;
        }
    }
}
