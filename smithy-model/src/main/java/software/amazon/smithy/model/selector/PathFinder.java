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

package software.amazon.smithy.model.selector;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
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

    private List<Path> searchFromShapeToSet(ToShapeId startingShape, Set<Shape> candidates) {
        Shape shape = model.getShape(startingShape.toShapeId()).orElse(null);
        if (shape == null || candidates.isEmpty()) {
            return ListUtils.of();
        } else {
            return new Search(reverseProvider, shape, candidates).execute();
        }
    }

    /**
     * Finds all of the possible paths from the {@code startingShape} to the
     * the {@code targetShape}.
     *
     * @param startingShape Starting shape to find the paths from.
     * @param targetShape The shape to try to find a path to.
     * @return Returns the list of matching paths.
     */
    public List<Path> search(ToShapeId startingShape, ToShapeId targetShape) {
        return searchFromShapeToSet(
                startingShape,
                model.getShape(targetShape.toShapeId()).map(Collections::singleton).orElse(Collections.emptySet()));
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

        Optional<ShapeId> structId = rel == RelationshipType.INPUT ? operation.getInput() : operation.getOutput();
        StructureShape struct = structId
                .flatMap(model::getShape)
                .flatMap(Shape::asStructureShape)
                .orElse(null);

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

        List<Relationship> relationships = new ArrayList<>();
        relationships.add(Relationship.create(operation, rel, struct));
        relationships.add(Relationship.create(struct, RelationshipType.STRUCTURE_MEMBER, member));
        relationships.add(Relationship.create(member, RelationshipType.MEMBER_TARGET, target));
        return Optional.of(new Path(relationships));
    }

    /**
     * An immutable {@code Relationship} path from a starting shape to an end shape.
     */
    public static final class Path extends AbstractList<Relationship> {
        private final List<Relationship> relationships;

        public Path(List<Relationship> relationships) {
            if (relationships.isEmpty()) {
                throw new IllegalArgumentException("Relationships cannot be empty!");
            }

            this.relationships = relationships;
        }

        @Override
        public int size() {
            return relationships.size();
        }

        @Override
        public Relationship get(int index) {
            return relationships.get(index);
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
            List<Shape> results = relationships.stream()
                    .map(Relationship::getShape)
                    .collect(Collectors.toList());
            Relationship last = relationships.get(relationships.size() - 1);
            if (last.getNeighborShape().isPresent()) {
                results.add(last.getNeighborShape().get());
            }

            return results;
        }

        /**
         * Gets the starting shape of the {@code Path}.
         *
         * @return Returns the starting shape of the Path.
         */
        public Shape getStartShape() {
            return relationships.get(0).getShape();
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
            Relationship last = relationships.get(relationships.size() - 1);
            return last.getNeighborShape().orElseThrow(() -> new SourceException(
                    "Relationship points to a shape that is invalid: " + last,
                    last.getShape()));
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

            for (Relationship rel : relationships) {
                if (rel.getRelationshipType() == RelationshipType.MEMBER_TARGET) {
                    result.append(" > ");
                } else {
                    result.append(" -[")
                            .append(rel.getRelationshipType().getSelectorLabel()
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

        Search(NeighborProvider provider, Shape startingShape, Collection<Shape> candidates) {
            this.startingShape = startingShape;
            this.candidates = candidates;
            this.provider = provider;
        }

        List<Path> execute() {
            for (Shape candidate : candidates) {
                traverseUp(candidate, new LinkedList<>(), new HashSet<>());
            }

            return results;
        }

        private void traverseUp(Shape current, List<Relationship> path, Set<ShapeId> visited) {
            if (!path.isEmpty() && current.getId().equals(startingShape.getId())) {
                // Add the path to the result set if the target shape was reached.
                // But, don't add the path if no nodes have been traversed.
                results.add(new Path(path));
                return;
            }

            // Short circuit any possible recursion. While it's not possible
            // to enter a recursive path with the built-in NeighborProvider
            // implementations and the bottom-up traversal, it is possible
            // that a custom neighbor provider has a different behavior, so
            // this check remains just in case.
            if (visited.contains(current.getId())) {
                return;
            }

            Set<ShapeId> newVisited = new HashSet<>(visited);
            newVisited.add(current.getId());

            for (Relationship relationship : provider.getNeighbors(current)) {
                // Don't traverse up through containers.
                if (relationship.getDirection() == RelationshipDirection.DIRECTED) {
                    List<Relationship> newPath = new ArrayList<>(path.size() + 1);
                    newPath.add(relationship);
                    newPath.addAll(path);
                    traverseUp(relationship.getShape(), newPath, newVisited);
                }
            }
        }
    }
}
