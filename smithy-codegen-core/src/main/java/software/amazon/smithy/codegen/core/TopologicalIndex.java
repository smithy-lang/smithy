/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core;

import java.util.ArrayList;
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
import software.amazon.smithy.model.shapes.ToShapeId;

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
    private final Map<Shape, Set<PathFinder.Path>> recursiveShapes = new LinkedHashMap<>();

    public TopologicalIndex(Model model) {
        // Explore sorted shapes not in the prelude for a stable result order.
        Set<Shape> shapes = new TreeSet<>();
        for (Shape shape : model.toSet()) {
            if (!Prelude.isPreludeShape(shape)) {
                shapes.add(shape);
            }
        }

        // This map ensures that more recursive shapes come after less recursive shapes.
        Map<Integer, Map<Shape, Set<PathFinder.Path>>> frequencyMap = new TreeMap<>();
        NeighborProvider provider = NeighborProviderIndex.of(model).getProvider();

        for (Shape shape : shapes) {
            Set<PathFinder.Path> paths = explore(shape, Collections.emptyList(), Collections.emptySet(), provider);
            if (!paths.isEmpty()) {
                int edges = 0;
                for (PathFinder.Path path : paths) {
                    edges += path.size();
                }
                frequencyMap.computeIfAbsent(edges, e -> new LinkedHashMap<>()).put(shape, paths);
            }
        }

        // Flatten the ordered frequency map into the collection of all recursive values.
        for (Map.Entry<Integer, Map<Shape, Set<PathFinder.Path>>> entry : frequencyMap.entrySet()) {
            recursiveShapes.putAll(entry.getValue());
        }
    }

    private Set<PathFinder.Path> explore(
            Shape shape,
            List<Relationship> path,
            Set<Shape> visited,
            NeighborProvider provider
    ) {
        if (visited.contains(shape)) {
            return Collections.singleton(new PathFinder.Path(path));
        }

        Set<Shape> newVisited = new LinkedHashSet<>(visited);
        newVisited.add(shape);

        // Sort edges alphabetically by shape to make the order predictable.
        Map<Shape, Relationship> shapeRelationshipMap = new TreeMap<>();
        for (Relationship rel : provider.getNeighbors(shape)) {
            if (rel.getRelationshipType().getDirection() == RelationshipDirection.DIRECTED) {
                if (!rel.getNeighborShapeId().equals(shape.getId()) && rel.getNeighborShape().isPresent()) {
                    shapeRelationshipMap.put(rel.getNeighborShape().get(), rel);
                }
            }
        }

        if (shapeRelationshipMap.isEmpty()) {
            shapes.add(shape);
            return Collections.emptySet();
        }

        Set<PathFinder.Path> recursivePaths = new LinkedHashSet<>();
        for (Map.Entry<Shape, Relationship> entry : shapeRelationshipMap.entrySet()) {
            List<Relationship> newPath = new ArrayList<>(path.size() + 1);
            newPath.addAll(path);
            newPath.add(entry.getValue());
            recursivePaths.addAll(explore(entry.getKey(), newPath, newVisited, provider));
        }

        if (recursivePaths.isEmpty()) {
            shapes.add(shape);
        }

        return recursivePaths;
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
     * <p>The first element of each path is the given {@code shape},
     * and the last element of each path is the first shape that is
     * encountered a second time in the path (i.e., the point of
     * recursion).
     *
     * @param shape Shape to get the recursive closures of.
     * @return The closures of the shape, or an empty {@code Set} if the shape is not recursive.
     */
    public Set<PathFinder.Path> getRecursiveClosure(ToShapeId shape) {
        if (shape instanceof Shape) {
            return Collections.unmodifiableSet(recursiveShapes.getOrDefault(shape, Collections.emptySet()));
        }

        // If given an ID, we need to scan the recursive shapes to look for a matching ID.
        ShapeId id = shape.toShapeId();
        for (Map.Entry<Shape, Set<PathFinder.Path>> entry : recursiveShapes.entrySet()) {
            if (entry.getKey().getId().equals(id)) {
                return Collections.unmodifiableSet(entry.getValue());
            }
        }

        return Collections.emptySet();
    }
}
