/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
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

    /** Default cap on the total edges kept across all recursive closures. */
    static final int DEFAULT_MAX_CLOSURE_EDGES = 1_000_000;

    private static final Logger LOGGER = Logger.getLogger(TopologicalIndex.class.getName());

    // Max number of truncated shape IDs listed in the warning.
    private static final int MAX_WARNED_SHAPES = 10;

    private static final int UNVISITED = 0;
    private static final int ON_STACK = 1;
    private static final int DONE = 2;

    private final Set<Shape> shapes = new LinkedHashSet<>();

    // Recursive shape -> number of closure paths kept, ordered by closure edge count.
    private final Map<Shape, Integer> recursiveShapes = new LinkedHashMap<>();

    // Graph of reachable shapes. After construction, only recursive shapes and their recursive edges remain.
    private final Map<Shape, Node> nodes = new HashMap<>();

    // Recursive shapes whose closure was truncated, sorted by shape ID.
    private final Set<Shape> truncatedShapes = new TreeSet<>();

    // Closures built lazily by getRecursiveClosure.
    private final Map<Shape, Set<PathFinder.Path>> closures = new ConcurrentHashMap<>();

    public TopologicalIndex(Model model) {
        this(model, DEFAULT_MAX_CLOSURE_EDGES);
    }

    TopologicalIndex(Model model, int maxClosureEdges) {
        // Explore sorted shapes not in the prelude for a stable result order.
        Set<Shape> roots = new TreeSet<>();
        for (Shape shape : model.toSet()) {
            if (!Prelude.isPreludeShape(shape)) {
                roots.add(shape);
            }
        }

        NeighborProvider provider = NeighborProviderIndex.of(model).getProvider();

        // Phase 1: mark recursive shapes and order the rest dependencies first.
        for (Shape shape : roots) {
            classify(node(shape), provider);
        }

        // Only edges to recursive shapes can be part of a recursive closure.
        for (Node node : nodes.values()) {
            if (node.recursive) {
                List<Relationship> recRels = new ArrayList<>();
                List<Node> recTargets = new ArrayList<>();
                for (int i = 0; i < node.targets.length; i++) {
                    if (node.targets[i].recursive) {
                        recRels.add(node.rels[i]);
                        recTargets.add(node.targets[i]);
                    }
                }
                node.recRels = recRels.toArray(new Relationship[0]);
                node.recTargets = recTargets.toArray(new Node[0]);
            }
        }

        // Phase 2: count closure paths and edges without building them, up to the edge budget.
        // This map ensures that more recursive shapes come after less recursive shapes.
        Map<Integer, Map<Shape, Integer>> frequencyMap = new TreeMap<>();
        Map<Node, FirstPath> firstPaths = new HashMap<>();
        // One-element arrays (also count and edges below) so the walk visitor lambda can update them.
        long[] remaining = {maxClosureEdges};

        for (Shape shape : roots) {
            Node node = nodes.get(shape);
            if (!node.recursive) {
                continue;
            }

            if (remaining[0] <= 0) {
                // Budget spent: keep only the first closure path.
                FirstPath first = firstPath(node, firstPaths);
                if (!first.single) {
                    truncatedShapes.add(shape);
                }
                frequencyMap.computeIfAbsent(first.length, e -> new LinkedHashMap<>()).put(shape, 1);
                continue;
            }

            int[] count = {0};
            int[] edges = {0};
            boolean complete = walk(node, (prefix, last) -> {
                if (remaining[0] <= 0) {
                    // Budget spent: stop and drop this path.
                    return false;
                }
                int size = prefix.size() + 1;
                count[0]++;
                edges[0] += size;
                remaining[0] -= size;
                return true;
            });
            if (!complete) {
                truncatedShapes.add(shape);
            }
            frequencyMap.computeIfAbsent(edges[0], e -> new LinkedHashMap<>()).put(shape, count[0]);
        }

        // getRecursiveClosure only needs recursive nodes and their recursive edges.
        nodes.values().removeIf(node -> !node.recursive);
        for (Node node : nodes.values()) {
            node.rels = null;
            node.targets = null;
        }

        // Flatten the ordered frequency map into the collection of all recursive values.
        for (Map<Shape, Integer> entry : frequencyMap.values()) {
            recursiveShapes.putAll(entry);
        }

        if (!truncatedShapes.isEmpty()) {
            LOGGER.warning(() -> truncationWarning(maxClosureEdges));
        }
    }

    private String truncationWarning(int maxClosureEdges) {
        StringBuilder message = new StringBuilder("Recursive closures of the model were truncated because they "
                + "contain more than ").append(maxClosureEdges)
                .append(" relationships in total. ")
                .append(truncatedShapes.size())
                .append(" shapes have truncated closures: ");
        int written = 0;
        for (Shape shape : truncatedShapes) {
            if (written == MAX_WARNED_SHAPES) {
                message.append(", ...");
                break;
            }
            if (written > 0) {
                message.append(", ");
            }
            message.append(shape.getId());
            written++;
        }
        return message.toString();
    }

    private static final class FirstPath {
        // Number of edges in the first closure path.
        final int length;
        // True if the first closure path is the only one.
        final boolean single;

        FirstPath(int length, boolean single) {
            this.length = length;
            this.single = single;
        }
    }

    private static final class Node {
        final Shape shape;
        // Directed edges to other shapes, sorted by target, and their target nodes.
        Relationship[] rels;
        Node[] targets;
        // Edges whose target is recursive. Only set on recursive nodes.
        Relationship[] recRels;
        Node[] recTargets;
        int state = UNVISITED;
        boolean recursive;

        Node(Shape shape) {
            this.shape = shape;
        }
    }

    // A depth-first search stack entry: a node and the index of its next edge to explore.
    private static final class Frame {
        final Node node;
        int next;

        Frame(Node node) {
            this.node = node;
        }
    }

    private interface PathVisitor {
        // Returns false to stop the walk.
        boolean visit(List<Relationship> prefix, Relationship last);
    }

    private Node node(Shape shape) {
        Node node = nodes.get(shape);
        if (node == null) {
            node = new Node(shape);
            nodes.put(shape, node);
        }
        return node;
    }

    private void computeEdges(Node node, NeighborProvider provider) {
        // Sort edges alphabetically by shape to make the order predictable.
        Map<Shape, Relationship> shapeRelationshipMap = new TreeMap<>();
        for (Relationship rel : provider.getNeighbors(node.shape)) {
            if (rel.getRelationshipType().getDirection() == RelationshipDirection.DIRECTED) {
                if (!rel.getNeighborShapeId().equals(node.shape.getId()) && rel.getNeighborShape().isPresent()) {
                    shapeRelationshipMap.put(rel.getNeighborShape().get(), rel);
                }
            }
        }

        node.rels = new Relationship[shapeRelationshipMap.size()];
        node.targets = new Node[shapeRelationshipMap.size()];
        int i = 0;
        for (Map.Entry<Shape, Relationship> entry : shapeRelationshipMap.entrySet()) {
            node.rels[i] = entry.getValue();
            node.targets[i] = node(entry.getKey());
            i++;
        }
    }

    // Iterative DFS. A shape is recursive if it is on, or can reach, a cycle.
    // Non-recursive shapes are added to `shapes` once finished, so dependencies come first.
    private void classify(Node root, NeighborProvider provider) {
        if (root.state != UNVISITED) {
            return;
        }

        Deque<Frame> stack = new ArrayDeque<>();
        root.state = ON_STACK;
        computeEdges(root, provider);
        stack.push(new Frame(root));

        while (!stack.isEmpty()) {
            Frame frame = stack.peek();
            Node node = frame.node;
            if (frame.next < node.targets.length) {
                Node target = node.targets[frame.next++];
                if (target.state == UNVISITED) {
                    target.state = ON_STACK;
                    computeEdges(target, provider);
                    stack.push(new Frame(target));
                } else if (target.state == ON_STACK || target.recursive) {
                    node.recursive = true;
                }
            } else {
                stack.pop();
                node.state = DONE;
                if (!node.recursive) {
                    shapes.add(node.shape);
                } else if (!stack.isEmpty()) {
                    stack.peek().node.recursive = true;
                }
            }
        }
    }

    // The first path walk() would emit: follow each shape's first recursive target until a shape repeats.
    // It is the only path iff every shape on it has exactly one recursive target. Memoized in `known`.
    private static FirstPath firstPath(Node start, Map<Node, FirstPath> known) {
        FirstPath result = known.get(start);
        if (result != null) {
            return result;
        }

        List<Node> sequence = new ArrayList<>();
        Map<Node, Integer> positions = new HashMap<>();
        Node current = start;
        while (!known.containsKey(current) && !positions.containsKey(current)) {
            positions.put(current, sequence.size());
            sequence.add(current);
            current = current.recTargets[0];
        }

        int end = sequence.size();
        Integer cycleStart = positions.get(current);
        if (cycleStart != null) {
            // A new cycle: every node on it has a first path as long as the cycle.
            int cycleLength = sequence.size() - cycleStart;
            boolean single = true;
            for (int i = cycleStart; i < sequence.size(); i++) {
                single &= sequence.get(i).recTargets.length == 1;
            }
            FirstPath cycle = new FirstPath(cycleLength, single);
            for (int i = cycleStart; i < sequence.size(); i++) {
                known.put(sequence.get(i), cycle);
            }
            end = cycleStart;
        }

        for (int i = end - 1; i >= 0; i--) {
            Node node = sequence.get(i);
            FirstPath next = known.get(node.recTargets[0]);
            known.put(node, new FirstPath(next.length + 1, next.single && node.recTargets.length == 1));
        }

        return known.get(start);
    }

    // Visits the closure paths of a recursive shape in stable depth-first order.
    // Returns false if the visitor stopped early.
    private static boolean walk(Node root, PathVisitor visitor) {
        Set<Node> onPath = new HashSet<>();
        List<Relationship> path = new ArrayList<>();
        Deque<Frame> stack = new ArrayDeque<>();
        onPath.add(root);
        stack.push(new Frame(root));

        while (!stack.isEmpty()) {
            Frame frame = stack.peek();
            Node node = frame.node;
            if (frame.next < node.recTargets.length) {
                int i = frame.next++;
                Node target = node.recTargets[i];
                if (onPath.contains(target)) {
                    if (!visitor.visit(path, node.recRels[i])) {
                        return false;
                    }
                } else {
                    path.add(node.recRels[i]);
                    onPath.add(target);
                    stack.push(new Frame(target));
                }
            } else {
                stack.pop();
                onPath.remove(node);
                if (!path.isEmpty()) {
                    path.remove(path.size() - 1);
                }
            }
        }

        return true;
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
     * <p>If all recursive closures together exceed 1,000,000 edges, closures
     * are truncated (see {@link #getRecursiveClosure(ToShapeId)}) and the
     * order uses the edge count of the kept paths only.
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
        return findRecursiveShape(shape) != null;
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
     * <p>If all recursive closures together exceed 1,000,000 edges, a
     * closure may contain only its first paths in depth-first order
     * (always at least one).
     *
     * @param shape Shape to get the recursive closures of.
     * @return The closures of the shape, or an empty {@code Set} if the shape is not recursive.
     */
    public Set<PathFinder.Path> getRecursiveClosure(ToShapeId shape) {
        Shape recursiveShape = findRecursiveShape(shape);
        if (recursiveShape == null) {
            return Collections.emptySet();
        }

        int limit = recursiveShapes.get(recursiveShape);
        return closures.computeIfAbsent(recursiveShape, s -> {
            Set<PathFinder.Path> paths = new LinkedHashSet<>();
            walk(nodes.get(s), (prefix, last) -> {
                List<Relationship> relationships = new ArrayList<>(prefix.size() + 1);
                relationships.addAll(prefix);
                relationships.add(last);
                paths.add(new PathFinder.Path(relationships));
                return paths.size() < limit;
            });
            return Collections.unmodifiableSet(paths);
        });
    }

    // True if at least one shape has a truncated recursive closure. Package-private for tests.
    boolean isTruncated() {
        return !truncatedShapes.isEmpty();
    }

    /**
     * Checks if the recursive closure of a shape was truncated.
     *
     * <p>When the recursive closures of a model contain more than 1,000,000
     * edges in total, {@link #getRecursiveClosure(ToShapeId)} returns only
     * the first paths of some closures. The edge budget is spent in shape ID
     * order, so adding shapes to a model can truncate the closure of an
     * existing shape.
     *
     * @param shape Shape to check.
     * @return True if the closure of the shape holds fewer paths than the shape has.
     */
    public boolean isTruncated(ToShapeId shape) {
        Shape recursiveShape = findRecursiveShape(shape);
        return recursiveShape != null && truncatedShapes.contains(recursiveShape);
    }

    private Shape findRecursiveShape(ToShapeId shape) {
        if (shape instanceof Shape) {
            return recursiveShapes.containsKey(shape) ? (Shape) shape : null;
        }

        // If given an ID, we need to scan the recursive shapes to look for a matching ID.
        ShapeId id = shape.toShapeId();
        for (Shape recursiveShape : recursiveShapes.keySet()) {
            if (recursiveShape.getId().equals(id)) {
                return recursiveShape;
            }
        }

        return null;
    }
}
