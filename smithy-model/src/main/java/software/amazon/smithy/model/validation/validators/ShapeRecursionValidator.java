/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import java.util.ArrayDeque;
import java.util.ArrayList;
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
import software.amazon.smithy.model.shapes.ShapeVisitor;
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
 * <p>Each recursive shape gets one event with the shortest recursion path.
 * Every possible path is not enumerated, because the number of paths can be
 * exponential in the size of the model.
 */
public final class ShapeRecursionValidator extends AbstractValidator {

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

    private String formatPath(PathFinder.Path path) {
        StringJoiner joiner = new StringJoiner(" > ");
        List<Shape> shapes = path.getShapes();
        for (int i = 0; i < shapes.size(); i++) {
            // Skip the first shape (the subject) to shorten the error message.
            if (i > 0) {
                joiner.add(shapes.get(i).getId().toString());
            }
        }
        return joiner.toString();
    }

    private void validateUnions(Model model, List<ValidationEvent> events) {
        UnionTerminatesVisitor visitor = new UnionTerminatesVisitor(model);
        for (UnionShape union : model.getUnionShapes()) {
            // Don't evaluate empty unions since that's a different error.
            if (!union.members().isEmpty() && !union.accept(visitor)) {
                events.add(error(union, "It is impossible to create instances of this recursive union"));
            }
            visitor.reset();
        }
    }

    private static final class UnionTerminatesVisitor extends ShapeVisitor.Default<Boolean> {

        private final Set<MemberShape> visited = new HashSet<>();
        private final Model model;

        UnionTerminatesVisitor(Model model) {
            this.model = model;
        }

        void reset() {
            this.visited.clear();
        }

        @Override
        protected Boolean getDefault(Shape shape) {
            return true;
        }

        @Override
        public Boolean structureShape(StructureShape shape) {
            if (shape.members().isEmpty()) {
                return true;
            }

            // If the structure has any non-required members, then it terminates.
            for (MemberShape member : shape.members()) {
                if (!member.isRequired()) {
                    return true;
                }
            }

            // Now check if any of the required members terminate.
            for (MemberShape member : shape.members()) {
                if (member.isRequired()) {
                    if (memberShape(member)) {
                        return true;
                    }
                }
            }

            return false;
        }

        @Override
        public Boolean unionShape(UnionShape shape) {
            for (MemberShape member : shape.members()) {
                if (memberShape(member)) {
                    return true;
                }
            }

            return false;
        }

        @Override
        public Boolean memberShape(MemberShape shape) {
            return visited.add(shape)
                    ? model.expectShape(shape.getTarget()).accept(this)
                    : false;
        }
    }

    /**
     * Finds a single recursion path from a shape back to itself.
     *
     * <p>All simple paths are not enumerated because their number can grow
     * exponentially with the size of the model (for example, a chain of
     * structures that each have two required members targeting the next
     * structure). Instead, strongly connected components of the filtered
     * graph are computed once with an iterative Tarjan's algorithm, and a
     * breadth-first search confined to the component of the subject finds
     * the shortest recursion path, if one exists. Everything is iterative so
     * that deep models can't overflow the stack.
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
        private final Deque<Shape> sccStack = new ArrayDeque<>();
        private final Set<ShapeId> onStack = new HashSet<>();
        private int nextIndex;
        private int nextComponent;

        CycleFinder(NeighborProvider reverse, Predicate<Relationship> filter) {
            this.reverse = reverse;
            this.filter = filter;
        }

        Optional<PathFinder.Path> findCycle(Shape start) {
            ShapeId startId = start.getId();
            if (!index.containsKey(startId)) {
                strongConnect(start);
            }

            int comp = component.get(startId);
            Map<ShapeId, Relationship> parent = new HashMap<>();
            Set<ShapeId> visited = new HashSet<>();
            visited.add(startId);
            Deque<Shape> queue = new ArrayDeque<>();
            queue.add(start);

            while (!queue.isEmpty()) {
                Shape current = queue.poll();
                for (Relationship rel : edges(current)) {
                    Shape previous = rel.getShape();
                    ShapeId previousId = previous.getId();
                    if (previousId.equals(startId)) {
                        return Optional.of(buildPath(startId, rel, current.getId(), parent));
                    }
                    Integer previousComponent = component.get(previousId);
                    if (previousComponent != null
                            && previousComponent == comp
                            && visited.add(previousId)) {
                        parent.put(previousId, rel);
                        queue.add(previous);
                    }
                }
            }

            return Optional.empty();
        }

        private PathFinder.Path buildPath(
                ShapeId startId,
                Relationship closing,
                ShapeId current,
                Map<ShapeId, Relationship> parent
        ) {
            List<Relationship> relationships = new ArrayList<>();
            relationships.add(closing);
            ShapeId next = current;
            while (!next.equals(startId)) {
                Relationship rel = parent.get(next);
                relationships.add(rel);
                next = rel.getNeighborShapeId();
            }
            return new PathFinder.Path(relationships);
        }

        private List<Relationship> edges(Shape shape) {
            List<Relationship> result = new ArrayList<>();
            for (Relationship rel : reverse.getNeighbors(shape)) {
                if (rel.getDirection() == RelationshipDirection.DIRECTED && filter.test(rel)) {
                    result.add(rel);
                }
            }
            return result;
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
                        ShapeId member;
                        do {
                            member = sccStack.pop().getId();
                            onStack.remove(member);
                            component.put(member, nextComponent);
                        } while (!member.equals(v));
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
}
