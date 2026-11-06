/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Predicate;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.NeighborProviderIndex;
import software.amazon.smithy.model.neighbor.NeighborProvider;
import software.amazon.smithy.model.neighbor.Relationship;
import software.amazon.smithy.model.neighbor.RelationshipDirection;
import software.amazon.smithy.model.selector.PathFinder;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.SetShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.model.traits.RequiredTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Ensures that list, set, and map shapes are not directly recursive,
 * meaning that if they do have a recursive reference to themselves,
 * one or more references that form the recursive path travels through
 * a structure or union shape. And ensures that structure members are
 * not infinitely mutually recursive using the required trait.
 *
 * <p>This check removes an entire class of problems from things like
 * code generators where a list of itself or a list of maps of itself
 * is impossible to define.
 *
 * <p>Each recursive shape gets one event with one of its recursion paths.
 * Every possible path is not enumerated, because the number of paths can be
 * exponential in the size of the model.
 */
public final class ShapeRecursionValidator extends AbstractValidator {

    // The number of shapes shown at each end of a truncated recursion path.
    private static final int MAX_PATH_ENDPOINT_SHAPES = 16;

    @Override
    public List<ValidationEvent> validate(Model model) {
        NeighborProvider reverse = NeighborProviderIndex.of(model).getReverseProvider();
        List<ValidationEvent> events = new ArrayList<>();
        validateListMapSetShapes(reverse, model, events);
        validateStructurePaths(reverse, model, events);
        validateUnions(model, events);
        return events;
    }

    private void validateListMapSetShapes(NeighborProvider reverse, Model model, List<ValidationEvent> events) {
        CycleFinder finder = new CycleFinder(reverse,
                rel -> !(rel.getShape().isStructureShape() || rel.getShape().isUnionShape()));

        for (ListShape shape : model.getListShapes()) {
            validateListMapSetShapes(shape, finder, events);
        }

        for (SetShape shape : model.getSetShapes()) {
            validateListMapSetShapes(shape, finder, events);
        }

        for (MapShape shape : model.getMapShapes()) {
            validateListMapSetShapes(shape, finder, events);
        }
    }

    private void validateListMapSetShapes(Shape shape, CycleFinder finder, List<ValidationEvent> events) {
        finder.findCycle(shape)
                .ifPresent(path -> events.add(error(shape,
                        String.format(
                                "Found invalid shape recursion: %s. A recursive list, set, or map shape is only "
                                        + "valid if an intermediate reference is through a union or structure.",
                                formatPath(path)))));
    }

    private void validateStructurePaths(NeighborProvider reverse, Model model, List<ValidationEvent> events) {
        CycleFinder finder = new CycleFinder(reverse, rel -> {
            if (rel.getShape().isStructureShape()) {
                return rel.getNeighborShape().get().hasTrait(RequiredTrait.ID);
            } else {
                return rel.getShape().isMemberShape();
            }
        });

        for (StructureShape shape : model.getStructureShapes()) {
            finder.findCycle(shape)
                    .ifPresent(path -> events.add(error(shape,
                            String.format(
                                    "Found invalid shape recursion: %s. A structure cannot be mutually recursive through all "
                                            + "required members.",
                                    formatPath(path)))));
        }
    }

    private String formatPath(RecursionPath path) {
        // Each relationship adds the shape it points to. The subject itself is
        // left out to shorten the error message.
        StringJoiner joiner = new StringJoiner(" > ");
        for (Relationship rel : path.head) {
            joiner.add(rel.getNeighborShapeId().toString());
        }
        if (path.omitted > 0) {
            joiner.add("... (" + path.omitted + " more) ...");
            for (Relationship rel : path.tail) {
                joiner.add(rel.getNeighborShapeId().toString());
            }
        }
        return joiner.toString();
    }

    private void validateUnions(Model model, List<ValidationEvent> events) {
        Set<ShapeId> terminating = findTerminatingShapes(model);
        for (UnionShape union : model.getUnionShapes()) {
            // Don't evaluate empty unions since that's a different error.
            if (!union.members().isEmpty() && !terminating.contains(union.getId())) {
                events.add(error(union, "It is impossible to create instances of this recursive union"));
            }
        }
    }

    /**
     * Finds every structure and union shape that terminates, meaning a value
     * of the shape can be built without infinite recursion.
     *
     * <p>Any shape other than a structure or union terminates. A structure
     * terminates if it has no members, has a member that isn't required, or
     * has a required member that targets a terminating shape. A union
     * terminates if any member targets a terminating shape.
     *
     * <p>This is computed once for the whole model by propagating termination
     * backward from member targets to the shapes that contain them, rather
     * than searching from each union, so it runs in linear time and doesn't
     * recurse.
     */
    private static Set<ShapeId> findTerminatingShapes(Model model) {
        Set<ShapeId> terminating = new HashSet<>();
        Map<ShapeId, List<ShapeId>> containers = new HashMap<>();
        Deque<ShapeId> queue = new ArrayDeque<>();

        for (StructureShape structure : model.getStructureShapes()) {
            boolean terminates = structure.members().isEmpty();
            for (MemberShape member : structure.members()) {
                if (!member.isRequired()) {
                    terminates = true;
                    break;
                }
            }
            if (terminates) {
                markTerminating(structure.getId(), terminating, queue);
            } else {
                addContainer(model, structure, containers, terminating, queue);
            }
        }

        for (UnionShape union : model.getUnionShapes()) {
            addContainer(model, union, containers, terminating, queue);
        }

        while (!queue.isEmpty()) {
            for (ShapeId container : containers.getOrDefault(queue.poll(), Collections.emptyList())) {
                markTerminating(container, terminating, queue);
            }
        }

        return terminating;
    }

    private static void addContainer(
            Model model,
            Shape container,
            Map<ShapeId, List<ShapeId>> containers,
            Set<ShapeId> terminating,
            Deque<ShapeId> queue
    ) {
        for (MemberShape member : container.members()) {
            Shape target = model.expectShape(member.getTarget());
            if (target.isStructureShape() || target.isUnionShape()) {
                containers.computeIfAbsent(target.getId(), id -> new ArrayList<>()).add(container.getId());
            } else {
                markTerminating(container.getId(), terminating, queue);
            }
        }
    }

    private static void markTerminating(ShapeId id, Set<ShapeId> terminating, Deque<ShapeId> queue) {
        if (terminating.add(id)) {
            queue.add(id);
        }
    }

    /**
     * Finds a single recursion path from a shape back to itself.
     *
     * <p>All simple paths are not enumerated because their number can grow
     * exponentially with the size of the model (for example, a chain of
     * structures that each have two required members targeting the next
     * structure). Instead, strongly connected components of the filtered
     * graph are computed once with an iterative Tarjan's algorithm. Every
     * shape in a component with a cycle is recursive. For each such component,
     * a root shape is chosen and two breadth-first searches find a path from
     * every shape in the component to the root and from the root back to it.
     * The recursion path of a shape is its path to the root followed by the
     * root's path back to the shape. This path isn't necessarily the shortest,
     * and it may visit a shape more than once, but it's found without searching
     * the component again for each shape. Everything is iterative so that deep
     * models can't overflow the stack.
     *
     * <p>The graph is walked over the reverse neighbor provider, following
     * directed relationships that match the filter, which is the same graph
     * that {@link PathFinder} walks.
     */
    private static final class CycleFinder {
        private final NeighborProvider reverse;
        private final Predicate<Relationship> filter;
        private final Map<ShapeId, Integer> index = new HashMap<>();
        private final Map<ShapeId, Integer> lowlink = new HashMap<>();
        private final Map<ShapeId, Integer> component = new HashMap<>();
        private final Map<Integer, RootPaths> rootPaths = new HashMap<>();
        private final Map<ShapeId, List<Relationship>> edges = new HashMap<>();
        private final Deque<Shape> sccStack = new ArrayDeque<>();
        private final Set<ShapeId> onStack = new HashSet<>();
        private int nextIndex;
        private int nextComponent;

        CycleFinder(NeighborProvider reverse, Predicate<Relationship> filter) {
            this.reverse = reverse;
            this.filter = filter;
        }

        Optional<RecursionPath> findCycle(Shape start) {
            ShapeId startId = start.getId();
            if (!index.containsKey(startId)) {
                strongConnect(start);
            }

            // Only components with a cycle have root paths.
            RootPaths paths = rootPaths.get(component.get(startId));
            return paths == null ? Optional.empty() : Optional.of(paths.pathOf(startId));
        }

        private boolean inComponent(Shape shape, int comp) {
            Integer shapeComponent = component.get(shape.getId());
            return shapeComponent != null && shapeComponent == comp;
        }

        // Filtered edges are computed once per shape and reused by every search.
        private List<Relationship> edges(Shape shape) {
            return edges.computeIfAbsent(shape.getId(), id -> {
                List<Relationship> result = new ArrayList<>();
                for (Relationship rel : reverse.getNeighbors(shape)) {
                    if (rel.getDirection() == RelationshipDirection.DIRECTED && filter.test(rel)) {
                        result.add(rel);
                    }
                }
                return result;
            });
        }

        /**
         * Paths between the root of a component and every other shape in it.
         *
         * <p>The root is the shape with the smallest ID so that the reported
         * paths don't depend on the order shapes are visited in.
         */
        private final class RootPaths {
            private final ShapeId root;
            // A relationship from a shape in the component to the root, if any.
            private Relationship closing;
            // For each shape, the next relationship on its path to the root.
            private final Map<ShapeId, Relationship> toRoot = new HashMap<>();
            // For each shape, the last relationship on the root's path to it.
            private final Map<ShapeId, Relationship> fromRoot = new HashMap<>();
            // The number of relationships on each shape's path to the root and from it.
            private final Map<ShapeId, Integer> toRootLength = new HashMap<>();
            private final Map<ShapeId, Integer> fromRootLength = new HashMap<>();
            // The relationships nearest the root, up to MAX_PATH_ENDPOINT_SHAPES of
            // them, at the end of each shape's path to the root and at the start of
            // the root's path to it. Shapes far from the root share their parent's list.
            private final Map<ShapeId, List<Relationship>> toRootEnd = new HashMap<>();
            private final Map<ShapeId, List<Relationship>> fromRootStart = new HashMap<>();

            RootPaths(int comp, Shape rootShape) {
                root = rootShape.getId();

                for (Relationship rel : edges(rootShape)) {
                    if (inComponent(rel.getShape(), comp)) {
                        closing = rel;
                        break;
                    }
                }
                if (closing == null) {
                    // A single shape without a relationship to itself isn't recursive.
                    return;
                }

                // Each edge of a shape is a relationship from another shape to it,
                // so searching along them from the root finds paths to the root.
                Map<ShapeId, List<Relationship>> outgoing = new HashMap<>();
                toRootLength.put(root, 0);
                toRootEnd.put(root, Collections.emptyList());
                fromRootLength.put(root, 0);
                fromRootStart.put(root, Collections.emptyList());
                Deque<Shape> queue = new ArrayDeque<>();
                queue.add(rootShape);
                Set<ShapeId> visited = new HashSet<>();
                visited.add(root);
                while (!queue.isEmpty()) {
                    Shape current = queue.poll();
                    for (Relationship rel : edges(current)) {
                        Shape previous = rel.getShape();
                        if (inComponent(previous, comp)) {
                            outgoing.computeIfAbsent(previous.getId(), id -> new ArrayList<>()).add(rel);
                            if (visited.add(previous.getId())) {
                                ShapeId id = previous.getId();
                                int length = toRootLength.get(current.getId());
                                List<Relationship> end = toRootEnd.get(current.getId());
                                if (length < MAX_PATH_ENDPOINT_SHAPES) {
                                    List<Relationship> longer = new ArrayList<>(end.size() + 1);
                                    longer.add(rel);
                                    longer.addAll(end);
                                    end = longer;
                                }
                                toRoot.put(id, rel);
                                toRootLength.put(id, length + 1);
                                toRootEnd.put(id, end);
                                queue.add(previous);
                            }
                        }
                    }
                }

                // Searching along the same relationships in their own direction
                // finds paths from the root.
                queue.add(rootShape);
                visited.clear();
                visited.add(root);
                while (!queue.isEmpty()) {
                    Shape current = queue.poll();
                    for (Relationship rel : outgoing.getOrDefault(current.getId(), Collections.emptyList())) {
                        Shape next = rel.getNeighborShape().get();
                        if (visited.add(next.getId())) {
                            ShapeId id = next.getId();
                            int length = fromRootLength.get(current.getId());
                            List<Relationship> start = fromRootStart.get(current.getId());
                            if (length < MAX_PATH_ENDPOINT_SHAPES) {
                                List<Relationship> longer = new ArrayList<>(start.size() + 1);
                                longer.addAll(start);
                                longer.add(rel);
                                start = longer;
                            }
                            fromRoot.put(id, rel);
                            fromRootLength.put(id, length + 1);
                            fromRootStart.put(id, start);
                            queue.add(next);
                        }
                    }
                }
            }

            /**
             * Gets the parts of a shape's recursion path that are printed.
             *
             * <p>The path is the shape's path to the root followed by the root's
             * path back to it. For the root itself, the second part is the root's
             * path to the start of the closing relationship, then that relationship.
             * Only the relationships at each end of the path are read, so this
             * doesn't depend on the length of the path.
             */
            RecursionPath pathOf(ShapeId shape) {
                boolean isRoot = shape.equals(root);
                ShapeId end = isRoot ? closing.getShape().getId() : shape;
                Relationship last = isRoot ? closing : null;
                int toLength = toRootLength.get(shape);
                int fromLength = fromRootLength.get(end) + (isRoot ? 1 : 0);
                int max = MAX_PATH_ENDPOINT_SHAPES;

                if (toLength + fromLength <= 2 * max) {
                    List<Relationship> all = firstToRoot(shape, toLength);
                    all.addAll(lastFromRoot(end, last, fromLength));
                    return new RecursionPath(all, 0, Collections.emptyList());
                }

                // If either part is shorter than max, the other part supplies
                // the rest of that end of the path.
                int headToRoot = Math.min(toLength, max);
                List<Relationship> head = firstToRoot(shape, headToRoot);
                head.addAll(firstFromRoot(end, last, max - headToRoot));
                int tailFromRoot = Math.min(fromLength, max);
                List<Relationship> tail = lastToRoot(shape, max - tailFromRoot);
                tail.addAll(lastFromRoot(end, last, tailFromRoot));
                return new RecursionPath(head, toLength + fromLength - 2 * max, tail);
            }

            // The first count relationships of the shape's path to the root.
            private List<Relationship> firstToRoot(ShapeId shape, int count) {
                List<Relationship> result = new ArrayList<>(count);
                ShapeId current = shape;
                while (result.size() < count) {
                    Relationship rel = toRoot.get(current);
                    result.add(rel);
                    current = rel.getNeighborShapeId();
                }
                return result;
            }

            // The last count relationships of the shape's path to the root, where
            // count is at most MAX_PATH_ENDPOINT_SHAPES.
            private List<Relationship> lastToRoot(ShapeId shape, int count) {
                List<Relationship> nearRoot = toRootEnd.get(shape);
                return new ArrayList<>(nearRoot.subList(nearRoot.size() - count, nearRoot.size()));
            }

            // The first count relationships of the root's path to end followed by
            // last, where count is at most MAX_PATH_ENDPOINT_SHAPES.
            private List<Relationship> firstFromRoot(ShapeId end, Relationship last, int count) {
                List<Relationship> nearRoot = fromRootStart.get(end);
                List<Relationship> result = new ArrayList<>(nearRoot.subList(0, Math.min(count, nearRoot.size())));
                if (result.size() < count) {
                    result.add(last);
                }
                return result;
            }

            // The last count relationships of the root's path to end followed by last.
            private List<Relationship> lastFromRoot(ShapeId end, Relationship last, int count) {
                Deque<Relationship> result = new ArrayDeque<>(count);
                if (last != null && count > 0) {
                    result.addFirst(last);
                }
                ShapeId current = end;
                while (result.size() < count) {
                    Relationship rel = fromRoot.get(current);
                    result.addFirst(rel);
                    current = rel.getShape().getId();
                }
                return new ArrayList<>(result);
            }
        }

        private void strongConnect(Shape root) {
            Deque<Frame> work = new ArrayDeque<>();
            work.push(visit(root));

            while (!work.isEmpty()) {
                Frame frame = work.peek();
                ShapeId v = frame.shape.getId();
                if (frame.position < frame.edges.size()) {
                    Shape w = frame.edges.get(frame.position++).getShape();
                    ShapeId wId = w.getId();
                    if (!index.containsKey(wId)) {
                        work.push(visit(w));
                    } else if (onStack.contains(wId)) {
                        lowlink.put(v, Math.min(lowlink.get(v), index.get(wId)));
                    }
                } else {
                    work.pop();
                    if (!work.isEmpty()) {
                        ShapeId p = work.peek().shape.getId();
                        lowlink.put(p, Math.min(lowlink.get(p), lowlink.get(v)));
                    }
                    if (lowlink.get(v).equals(index.get(v))) {
                        Shape member;
                        Shape smallest = frame.shape;
                        do {
                            member = sccStack.pop();
                            onStack.remove(member.getId());
                            component.put(member.getId(), nextComponent);
                            if (member.getId().compareTo(smallest.getId()) < 0) {
                                smallest = member;
                            }
                        } while (!member.getId().equals(v));
                        RootPaths paths = new RootPaths(nextComponent, smallest);
                        if (paths.closing != null) {
                            rootPaths.put(nextComponent, paths);
                        }
                        nextComponent++;
                    }
                }
            }
        }

        private Frame visit(Shape shape) {
            ShapeId id = shape.getId();
            index.put(id, nextIndex);
            lowlink.put(id, nextIndex);
            nextIndex++;
            sccStack.push(shape);
            onStack.add(id);
            return new Frame(shape, edges(shape));
        }

        private static final class Frame {
            private final Shape shape;
            private final List<Relationship> edges;
            private int position;

            Frame(Shape shape, List<Relationship> edges) {
                this.shape = shape;
                this.edges = edges;
            }
        }
    }

    /**
     * The printed parts of a recursion path, which starts and ends at the
     * subject shape. If omitted is zero, head holds the whole path.
     * Otherwise, head and tail hold the relationships at each end and
     * omitted relationships are left out between them.
     */
    private static final class RecursionPath {
        private final List<Relationship> head;
        private final int omitted;
        private final List<Relationship> tail;

        RecursionPath(List<Relationship> head, int omitted, List<Relationship> tail) {
            this.head = head;
            this.omitted = omitted;
            this.tail = tail;
        }
    }
}
