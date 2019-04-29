/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.knowledge.NeighborProviderIndex;
import software.amazon.smithy.model.neighbor.NeighborProvider;
import software.amazon.smithy.model.neighbor.Relationship;
import software.amazon.smithy.model.neighbor.RelationshipType;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeIndex;
import software.amazon.smithy.model.shapes.ToShapeId;
import software.amazon.smithy.utils.ListUtils;

/**
 * Finds the possible downward directed relationship paths from a starting
 * shape to shapes connected to the starting shape that match a selector.
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
 * <p>{@code PathFinder} is downward directed, meaning it only traverses
 * relationships from containers to containees. In other words, {@code PathFinder}
 * will not traverse relationships from a resource to the resource's parent,
 * from a member to the shape that contains it, etc.
 */
public final class PathFinder {
    private static final Logger LOGGER = Logger.getLogger(PathFinder.class.getName());

    private final ShapeIndex index;
    private final NeighborProvider neighborProvider;
    private final NeighborProvider reverseProvider;

    private PathFinder(ShapeIndex index, NeighborProvider neighborProvider) {
        this.index = index;
        this.neighborProvider = neighborProvider;
        this.reverseProvider = NeighborProvider.reverse(index, neighborProvider);
    }

    /**
     * Creates a {@code PathFinder} that uses the given {@code Model}.
     *
     * @param model Model to search using a {@code PathFinder}.
     * @return Returns the crated {@code PathFinder}.
     */
    public static PathFinder create(Model model) {
        return new PathFinder(model.getShapeIndex(), model.getKnowledge(NeighborProviderIndex.class).getProvider());
    }

    /**
     * Creates a {@code PathFinder} that uses the given {@code ShapeIndex}
     * and computes the neighbors.
     *
     * @param index Shape index to search using a {@code PathFinder}.
     * @return Returns the crated {@code PathFinder}.
     */
    public static PathFinder create(ShapeIndex index) {
        return new PathFinder(index, NeighborProvider.of(index));
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
        Shape shape = index.getShape(startingShape.toShapeId()).orElse(null);

        if (shape == null) {
            return ListUtils.of();
        }

        // Find all shapes that match the selector then work backwards from there.
        Set<Shape> candidates = targetSelector.select(neighborProvider, index);
        if (candidates.isEmpty()) {
            LOGGER.info(() -> "No shapes matched the PathFinder selector of `" + targetSelector + "`");
            return ListUtils.of();
        }

        LOGGER.finest(() -> candidates.size() + " shapes matched the PathFinder selector of " + targetSelector);
        return new Search(reverseProvider, shape, candidates).execute();
    }

    /**
     * An immutable {@code Relationship} path from a starting shape to an end shape.
     */
    public static final class Path extends AbstractList<Relationship> {
        private List<Relationship> relationships;

        Path(List<Relationship> relationships) {
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
         * @throws SourceException if the relationship is invalid.
         */
        public Shape getEndShape() {
            Relationship last = relationships.get(relationships.size() - 1);
            return last.getNeighborShape().orElseThrow(() -> new SourceException(
                    "Relationship points to a shape that is invalid: "+ last,
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
                    result.append(" -[").append(NeighborSelector.getRelType(rel.getRelationshipType())).append("]-> ");
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
                switch (relationship.getRelationshipType()) {
                    case MEMBER_CONTAINER:
                    case BOUND:
                        // Don't traverse up through containers.
                        continue;
                    default:
                        LinkedList<Relationship> newPath = new LinkedList<>(path);
                        newPath.addFirst(relationship);
                        traverseUp(relationship.getShape(), newPath, newVisited);
                }
            }
        }
    }
}
