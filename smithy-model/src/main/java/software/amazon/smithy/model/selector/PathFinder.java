/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.selector;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Logger;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.knowledge.NeighborProviderIndex;
import software.amazon.smithy.model.neighbor.NeighborProvider;
import software.amazon.smithy.model.neighbor.Relationship;
import software.amazon.smithy.model.neighbor.RelationshipDirection;
import software.amazon.smithy.model.neighbor.RelationshipType;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.ToShapeId;
import software.amazon.smithy.utils.FunctionalUtils;
import software.amazon.smithy.utils.ListUtils;

/**
 * Finds the possible directed relationship paths from a starting shape to
 * shapes connected to the starting shape that match a selector.
 *
 * <p>For example, the {@code PathFinder} can answer the question of
 * "Where are all of the shapes in the closure of the input of an operation
 * marked with the {@code sensitive} trait?"
 *
 * <pre>{@code
 * PathFinder pathFinder = PathFinder.create(myModel);
 * List<PathFinder.Path> results = pathFinder.search(myOperationInput, "[trait|sensitive]");
 * }</pre>
 *
 * <p>{@code PathFinder} is directed, meaning it only traverses relationships
 * from shapes that define a relationship to shapes that it targets. In other
 * words, {@code PathFinder} will not traverse relationships from a resource to
 * the resource's parent or from a member to the shape that contains it
 * because those are inverted relationships.
 */
public final class PathFinder {
    private static final Logger LOGGER = Logger.getLogger(PathFinder.class.getName());

    private final Model model;
    private final NeighborProvider reverseProvider;
    private Predicate<Relationship> filter = FunctionalUtils.alwaysTrue();

    private PathFinder(Model model) {
        this.model = model;
        this.reverseProvider = NeighborProviderIndex.of(model).getReverseProvider();
    }

    /**
     * Creates a {@code PathFinder} that uses the given {@code Model}.
     *
     * @param model Model to search using a {@code PathFinder}.
     * @return Returns the crated {@code PathFinder}.
     */
    public static PathFinder create(Model model) {
        return new PathFinder(model);
    }

    /**
     * Sets a predicate function to prevents traversing specific relationships.
     *
     * @param predicate Predicate that must return true in order to continue traversing relationships.
     */
    public void relationshipFilter(Predicate<Relationship> predicate) {
        this.filter = predicate;
    }

    /**
     * Finds all of the possible paths from the starting shape to all shapes
     * connected to the starting shape that match the given selector.
     *
     * @param startingShape Starting shape to find the paths from.
     * @param targetSelector Selector that matches shapes to find the path to.
     * @return Returns the list of matching paths.
     */
    public List<Path> search(ToShapeId startingShape, String targetSelector) {
        return search(startingShape, Selector.parse(targetSelector));
    }

    /**
     * Finds all of the possible paths from the starting shape to all shapes
     * connected to the starting shape that match the given selector.
     *
     * @param startingShape Starting shape to find the paths from.
     * @param targetSelector Selector that matches shapes to find the path to.
     * @return Returns the list of matching paths.
     */
    public List<Path> search(ToShapeId startingShape, Selector targetSelector) {
        // Find all shapes that match the selector then work backwards from there.
        Set<Shape> candidates = targetSelector.select(model);

        if (candidates.isEmpty()) {
            LOGGER.info(() -> "No shapes matched the PathFinder selector of `" + targetSelector + "`");
            return ListUtils.of();
        }

        LOGGER.finest(() -> candidates.size() + " shapes matched the PathFinder selector of " + targetSelector);
        return searchFromShapeToSet(startingShape, candidates);
    }

    private List<Path> searchFromShapeToSet(ToShapeId startingShape, Collection<Shape> candidates) {
        Shape shape = model.getShape(startingShape.toShapeId()).orElse(null);
        if (shape == null || candidates.isEmpty()) {
            return ListUtils.of();
        } else {
            return new Search(reverseProvider, shape, candidates, filter).execute();
        }
    }

    /**
     * Finds all of the possible paths from the {@code startingShape} to
     * any of the provided shapes in {@code targetShapes}.
     *
     * @param startingShape Starting shape to find the paths from.
     * @param targetShapes The shapes to try to find a path to.
     * @return Returns the list of matching paths.
     */
    public List<Path> search(ToShapeId startingShape, Collection<Shape> targetShapes) {
        return searchFromShapeToSet(startingShape, targetShapes);
    }

    /**
     * Creates a {@code Path} to an operation input member if it exists.
     *
     * @param operationId Operation to start from.
     * @param memberName Input member name to find in the operation input.
     * @return Returns the optionally found {@code Path} to the member.
     */
    public Optional<Path> createPathToInputMember(ToShapeId operationId, String memberName) {
        return createPathTo(operationId, memberName, RelationshipType.INPUT);
    }

    /**
     * Creates a {@code Path} to an operation output member if it exists.
     *
     * @param operationId Operation to start from.
     * @param memberName Output member name to find in the operation output.
     * @return Returns the optionally found {@code Path} to the member.
     */
    public Optional<Path> createPathToOutputMember(ToShapeId operationId, String memberName) {
        return createPathTo(operationId, memberName, RelationshipType.OUTPUT);
    }

    private Optional<Path> createPathTo(ToShapeId operationId, String memberName, RelationshipType rel) {
        OperationShape operation = model.getShape(operationId.toShapeId())
                .flatMap(Shape::asOperationShape)
                .orElse(null);

        if (operation == null) {
            return Optional.empty();
        }

        ShapeId structId = rel == RelationshipType.INPUT ? operation.getInputShape() : operation.getOutputShape();
        StructureShape struct = model.getShape(structId).flatMap(Shape::asStructureShape).orElse(null);

        if (struct == null) {
            return Optional.empty();
        }

        MemberShape member = struct.getMember(memberName).orElse(null);

        if (member == null) {
            return Optional.empty();
        }

        Shape target = model.getShape(member.getTarget()).orElse(null);

        if (target == null) {
            return Optional.empty();
        }

        Path path = new Path(Relationship.create(member, RelationshipType.MEMBER_TARGET, target), null);
        path = new Path(Relationship.create(struct, RelationshipType.STRUCTURE_MEMBER, member), path);
        path = new Path(Relationship.create(operation, rel, struct), path);
        return Optional.of(path);
    }

    /**
     * An immutable {@code Relationship} path from a starting shape to an end shape.
     */
    public static final class Path extends AbstractList<Relationship> {
        private final Relationship value;
        private Path next;
        private final int size;

        public Path(List<Relationship> relationships) {
            if (relationships.isEmpty()) {
                throw new IllegalArgumentException("Relationships cannot be empty!");
            }

            this.size = relationships.size();
            this.value = relationships.get(0);

            if (relationships.size() == 1) {
                next = null;
            } else {
                Path current = this;
                for (int i = 1; i < relationships.size(); i++) {
                    current.next = new Path(relationships.get(i), null);
                    current = current.next;
                }
            }
        }

        private Path(Relationship value, Path next) {
            this.value = value;
            this.next = next;
            this.size = 1 + ((next == null) ? 0 : next.size);
        }

        @Override
        public int size() {
            return size;
        }

        @Override
        public Relationship get(int index) {
            Path current = this;
            for (int i = 0; i < index; i++) {
                current = current.next;
                if (current == null) {
                    throw new IndexOutOfBoundsException("Invalid index " + index + "; size " + size());
                }
            }
            return current.value;
        }

        @Override
        public Iterator<Relationship> iterator() {
            return new Iterator<Relationship>() {
                private Path current = Path.this;

                @Override
                public boolean hasNext() {
                    return current != null;
                }

                @Override
                public Relationship next() {
                    if (current == null) {
                        throw new NoSuchElementException();
                    }
                    Relationship result = current.value;
                    current = current.next;
                    return result;
                }
            };
        }

        /**
         * Gets a list of all shapes in the path including the starting
         * shape all the way to the last shape.
         *
         * <p>The returned list does not return the last element (the
         * end shape targeted by the last neighbor) if it does not exist.
         *
         * @return Returns the list of shapes.
         */
        public List<Shape> getShapes() {
            List<Shape> results = new ArrayList<>(size());
            Iterator<Relationship> iterator = iterator();
            for (int i = 0; i < size(); i++) {
                Relationship rel = iterator.next();
                results.add(rel.getShape());
                // Add the shape pointed to by the tail to the result set if present
                // without need to get the tail after iterating (an O(N) operation).
                if (i == size() - 1) {
                    rel.getNeighborShape().ifPresent(results::add);
                }
            }
            return results;
        }

        /**
         * Gets the starting shape of the {@code Path}.
         *
         * @return Returns the starting shape of the Path.
         */
        public Shape getStartShape() {
            return value.getShape();
        }

        /**
         * Gets the ending shape of the {@code Path} that
         * matched the selector and is connected to the
         * starting shape.
         *
         * @return Returns the ending shape of the Path.
         * @throws SourceException if the last relationship is invalid.
         */
        public Shape getEndShape() {
            Relationship last = tail();
            return last.getNeighborShape()
                    .orElseThrow(() -> new SourceException(
                            "Relationship points to a shape that is invalid: " + last,
                            last.getShape()));
        }

        private Relationship tail() {
            Path current = this;
            while (current.next != null) {
                current = current.next;
            }
            return current.value;
        }

        /**
         * Converts the path to valid {@link Selector} syntax.
         *
         * @return Returns the path as a selector.
         */
        @Override
        public String toString() {
            StringBuilder result = new StringBuilder();
            result.append("[id|").append(getStartShape().getId()).append("]");

            for (Relationship rel : this) {
                if (rel.getRelationshipType() == RelationshipType.MEMBER_TARGET) {
                    result.append(" > ");
                } else {
                    result.append(" -[")
                            .append(rel.getRelationshipType()
                                    .getSelectorLabel()
                                    .orElseGet(() -> rel.getRelationshipType().toString()))
                            .append("]-> ");
                }
                result.append("[id|").append(rel.getNeighborShapeId()).append("]");
            }

            return result.toString();
        }
    }

    private static final class Search {
        private final Shape startingShape;
        private final NeighborProvider provider;
        private final Collection<Shape> candidates;
        private final List<Path> results = new ArrayList<>();
        private final Predicate<Relationship> filter;

        Search(
                NeighborProvider provider,
                Shape startingShape,
                Collection<Shape> candidates,
                Predicate<Relationship> filter
        ) {
            this.startingShape = startingShape;
            this.candidates = candidates;
            this.provider = provider;
            this.filter = filter;
        }

        List<Path> execute() {
            Set<ShapeId> visited = new HashSet<>();
            for (Shape candidate : candidates) {
                traverseUp(candidate, null, visited);
            }

            return results;
        }

        private void traverseUp(Shape current, Path path, Set<ShapeId> visited) {
            if (path != null && current.getId().equals(startingShape.getId())) {
                // Add the path to the result set if the target shape was reached.
                // But, don't add the path if no nodes have been traversed.
                results.add(path);
                return;
            }

            // Short-circuit recursion.
            if (visited.add(current.getId())) {
                for (Relationship relationship : provider.getNeighbors(current)) {
                    if (relationship.getDirection() == RelationshipDirection.DIRECTED) {
                        if (filter.test(relationship)) {
                            traverseUp(relationship.getShape(), new Path(relationship, path), visited);
                        }
                    }
                }
                // Let the less recursive addition remove the entry from the set.
                visited.remove(current.getId());
            }
        }
    }
}
