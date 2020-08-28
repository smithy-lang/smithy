/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.codegen.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.KnowledgeIndex;
import software.amazon.smithy.model.knowledge.NeighborProviderIndex;
import software.amazon.smithy.model.loader.Prelude;
import software.amazon.smithy.model.neighbor.NeighborProvider;
import software.amazon.smithy.model.neighbor.Relationship;
import software.amazon.smithy.model.neighbor.RelationshipDirection;
import software.amazon.smithy.model.selector.PathFinder;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.SimpleShape;
import software.amazon.smithy.model.shapes.ToShapeId;
import software.amazon.smithy.utils.FunctionalUtils;

/**
 * Creates a reverse-topological ordering of shapes.
 *
 * <p>This kind of reverse topological ordering is useful for languages
 * like C++ that need to define shapes before they can be referenced.
 * Only non-recursive shapes are reverse-topologically ordered using
 * {@link #getOrderedShapes()}. However, recursive shapes are queryable
 * through {@link #getRecursiveShapes()}. When this returned {@code Set} is
 * iterated, recursive shapes are ordered by their degree of recursion (the
 * number of edges across all recursive closures), and then by shape ID
 * when multiple shapes have the same degree of recursion.
 *
 * <p>The recursion closures of a shape can be queried using
 * {@link #getRecursiveClosure(ToShapeId)}. This method returns a list of
 * paths from the shape back to itself. This list can be useful for code
 * generation to generate different code based on if a recursive path
 * passes through particular types of shapes.
 */
public final class TopologicalIndex implements KnowledgeIndex {

    private final Set<Shape> shapes = new LinkedHashSet<>();
    private final Map<Shape, List<PathFinder.Path>> recursiveShapes = new LinkedHashMap<>();

    public TopologicalIndex(Model model) {
        // A reverse-topological sort can't be performed on recursive shapes,
        // so instead, recursive shapes are explored first and removed from
        // the topological sort.
        computeRecursiveShapes(model);

        // Next, the model is explored using a DFS so that targets of shapes
        // are ordered before the shape itself.
        NeighborProvider provider = NeighborProviderIndex.of(model).getProvider();
        model.shapes()
                // Note that while we do not scan the prelude here, shapes from
                // the prelude are pull into the ordered result if referenced.
                .filter(FunctionalUtils.not(Prelude::isPreludeShape))
                .filter(shape -> !recursiveShapes.containsKey(shape))
                // Sort here to provide a deterministic result.
                .sorted()
                .forEach(shape -> visitShape(provider, shape));
    }

    private void computeRecursiveShapes(Model model) {
        // PathFinder is used to find all paths from U -> U.
        PathFinder finder = PathFinder.create(model);

        // The order of recursive shapes is first by the number of edges
        // (the degree of recursion), and then alphabetically by shape ID.
        Map<Integer, Map<Shape, List<PathFinder.Path>>> edgesToShapePaths = new TreeMap<>();
        for (Shape shape : model.toSet()) {
            if (!Prelude.isPreludeShape(shape) && !(shape instanceof SimpleShape)) {
                // Find all paths from the shape back to itself.
                List<PathFinder.Path> paths = finder.search(shape, shape);
                if (!paths.isEmpty()) {
                    int edgeCount = 0;
                    for (PathFinder.Path path : paths) {
                        edgeCount += path.size();
                    }
                    edgesToShapePaths.computeIfAbsent(edgeCount, s -> new TreeMap<>())
                            .put(shape, Collections.unmodifiableList(paths));
                }
            }
        }

        for (Map.Entry<Integer, Map<Shape, List<PathFinder.Path>>> entry : edgesToShapePaths.entrySet()) {
            recursiveShapes.putAll(entry.getValue());
        }
    }

    private void visitShape(NeighborProvider provider, Shape shape) {
        // Visit members before visiting containers. Note that no 'visited'
        // set is needed since only non-recursive shapes are traversed.
        // We sort the neighbors to better order the result.
        Set<Shape> neighbors = new TreeSet<>();
        for (Relationship rel : provider.getNeighbors(shape)) {
            if (rel.getRelationshipType().getDirection() == RelationshipDirection.DIRECTED) {
                if (!rel.getNeighborShapeId().equals(shape.getId()) && rel.getNeighborShape().isPresent()) {
                    neighbors.add(rel.getNeighborShape().get());
                }
            }
        }

        for (Shape neighbor : neighbors) {
            visitShape(provider, neighbor);
        }

        shapes.add(shape);
    }

    /**
     * Creates a new {@code TopologicalIndex}.
     *
     * @param model Model to create the index from.
     * @return The created (or previously cached) {@code TopologicalIndex}.
     */
    public static TopologicalIndex of(Model model) {
        return model.getKnowledge(TopologicalIndex.class, TopologicalIndex::new);
    }

    /**
     * Gets all reverse-topologically ordered shapes, including members.
     *
     * <p>When the returned {@code Set} is iterated, shapes are returned in
     * reverse-topological. Note that the returned set does not contain
     * recursive shapes.
     *
     * @return Non-recursive shapes in a reverse-topological ordered {@code Set}.
     */
    public Set<Shape> getOrderedShapes() {
        return Collections.unmodifiableSet(shapes);
    }

    /**
     * Gets all shapes that have edges that are part of a recursive closure,
     * including container shapes (list/set/map/structure/union) and members.
     *
     * <p>When iterated, the returned {@code Set} is ordered from fewest number
     * of edges to the most number of edges in the recursive closures, and then
     * alphabetically by shape ID when there are multiple entries with
     * the same number of edges.
     *
     * @return All shapes that are part of a recursive closure.
     */
    public Set<Shape> getRecursiveShapes() {
        return Collections.unmodifiableSet(recursiveShapes.keySet());
    }

    /**
     * Checks if the given shape has edges with recursive references.
     *
     * @param shape Shape to check.
     * @return True if the shape has recursive edges.
     */
    public boolean isRecursive(ToShapeId shape) {
        return !getRecursiveClosure(shape).isEmpty();
    }

    /**
     * Gets the recursive closure of a given shape represented as
     * {@link PathFinder.Path} objects.
     *
     * @param shape Shape to get the recursive closures of.
     * @return The closures of the shape, or an empty {@code List} if the shape is not recursive.
     */
    public List<PathFinder.Path> getRecursiveClosure(ToShapeId shape) {
        if (shape instanceof Shape) {
            return recursiveShapes.getOrDefault(shape, Collections.emptyList());
        }

        // If given an ID, we need to scan the recursive shapes to look for a matching ID.
        ShapeId id = shape.toShapeId();
        for (Map.Entry<Shape, List<PathFinder.Path>> entry : recursiveShapes.entrySet()) {
            if (entry.getKey().getId().equals(id)) {
                return entry.getValue();
            }
        }

        return Collections.emptyList();
    }
}
