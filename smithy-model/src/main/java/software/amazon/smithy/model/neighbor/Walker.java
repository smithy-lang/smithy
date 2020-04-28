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

package software.amazon.smithy.model.neighbor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.function.Predicate;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.NeighborProviderIndex;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Walks connected shapes within a Model.
 *
 * <p>Any shape that is connected to another shape is "walked". A single
 * shape can have multiple relationships to the same shape. For example,
 * a resource can have both a "get" and a "child" relationship to an
 * operation; however, the referenced operation will appear only once
 * in the walker output.
 *
 * <p>Only shapes form a connected graph. Relationships created by traits
 * are not traversed by the walker.
 */
public final class Walker {

    private final NeighborProvider provider;

    public Walker(Model model) {
        this(model.getKnowledge(NeighborProviderIndex.class).getProvider());
    }

    public Walker(NeighborProvider provider) {
        this.provider = provider;
    }

    /**
     * Walks connected shapes in the model, returning them in a set.
     *
     * @param shape The shape to start the traversal from.
     * @return Returns a set of connected shapes.
     */
    public Set<Shape> walkShapes(Shape shape) {
        return walkShapes(shape, rel -> true);
    }

    /**
     * Walks connected shapes in the model, returning them in a set.
     *
     * @param shape The shape to start the traversal from.
     * @param predicate Predicate used to prevent traversing relationships.
     * @return Returns a set of connected shapes.
     */
    public Set<Shape> walkShapes(Shape shape, Predicate<Relationship> predicate) {
        Set<Shape> connectedShapes = new HashSet<>();
        connectedShapes.add(shape);
        for (Relationship rel : walk(shape, predicate)) {
            if (rel.getNeighborShape().isPresent()) {
                connectedShapes.add(rel.getNeighborShape().get());
            }
        }
        return connectedShapes;
    }

    private List<Relationship> walk(Shape shape, Predicate<Relationship> predicate) {
        List<Relationship> relationships = new ArrayList<>();
        Set<ShapeId> traversed = new HashSet<>();
        traversed.add(shape.getId());
        Stack<Relationship> stack = new Stack<>();
        pushNeighbors(stack, predicate, provider.getNeighbors(shape));
        Relationship relationship;

        while (!stack.isEmpty()) {
            relationship = stack.pop();
            if (relationship.getNeighborShape().isPresent()) {
                Shape neighbor = relationship.getNeighborShape().get();
                if (!traversed.contains(neighbor.getId())) {
                    traversed.add(neighbor.getId());
                    pushNeighbors(stack, predicate, provider.getNeighbors(neighbor));
                }
            }
            relationships.add(relationship);
        }

        return relationships;
    }

    private void pushNeighbors(
            Stack<Relationship> stack,
            Predicate<Relationship> predicate,
            Collection<Relationship> relationships
    ) {
        for (Relationship rel : relationships)  {
            if (predicate.test(rel)) {
                stack.push(rel);
            }
        }
    }
}
