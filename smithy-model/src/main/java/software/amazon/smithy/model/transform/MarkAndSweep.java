/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.transform;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.neighbor.NeighborProvider;
import software.amazon.smithy.model.neighbor.Relationship;
import software.amazon.smithy.model.neighbor.RelationshipDirection;
import software.amazon.smithy.model.neighbor.RelationshipType;
import software.amazon.smithy.model.shapes.Shape;

/**
 * Performs a garbage collection style cleanup of a model by removing
 * unreferenced shapes based on a custom {@code marker} function.
 *
 * <p>The marker function is invoked at the start of each round and passed
 * a context object. The marker can query the context object and mark shapes
 * as needing to be removed. The MarkAndSweep then finds all shapes
 * that have targets to them but is only targeted by shapes that have been
 * marked for removal. These matching shapes are then marked for removal as
 * well, potentially freeing up other shapes to be marked for removal in the
 * next round. This process continues until no new shapes are marked in a
 * round.
 */
final class MarkAndSweep {

    private final Predicate<Shape> sweepFilter;
    private final Consumer<MarkerContext> marker;

    MarkAndSweep(Consumer<MarkerContext> marker, Predicate<Shape> sweepFilter) {
        this.marker = marker;
        this.sweepFilter = sweepFilter;
    }

    Set<Shape> markAndSweep(Model model) {
        NeighborProvider reverseNeighbors = NeighborProvider.reverse(model);
        MarkerContext context = new MarkerContext(reverseNeighbors, model, sweepFilter);

        int currentSize;
        do {
            currentSize = context.getMarkedForRemoval().size();
            marker.accept(context);
            // Find shapes that are only referenced by a shape that has been marked for removal.
            model.shapes().filter(shape -> !shape.isMemberShape()).forEach(shape -> {
                if (!context.getMarkedForRemoval().contains(shape)) {
                    Set<Shape> targetedFrom = context.getTargetedFrom(shape);
                    if (!targetedFrom.isEmpty()) {
                        targetedFrom.removeAll(context.getMarkedForRemoval());
                        if (targetedFrom.isEmpty()) {
                            context.markShape(shape);
                        }
                    }
                }
            });
        } while (currentSize != context.getMarkedForRemoval().size());

        return context.getMarkedForRemoval();
    }

    /**
     * Context object passed to the marked in each pass on the model.
     */
    static final class MarkerContext {

        private final NeighborProvider reverseProvider;
        private final Model model;
        private final Set<Shape> markedForRemoval = new HashSet<>();
        private final Predicate<Shape> sweepFilter;

        MarkerContext(NeighborProvider reverseProvider, Model model, Predicate<Shape> sweepFilter) {
            this.reverseProvider = reverseProvider;
            this.model = model;
            this.sweepFilter = sweepFilter;
        }

        /**
         * @return Gets the model being evaluated.
         */
        Model getModel() {
            return model;
        }

        /**
         * @return Gets the immutable set of shapes marked for removal.
         */
        Set<Shape> getMarkedForRemoval() {
            return Collections.unmodifiableSet(markedForRemoval);
        }

        /**
         * Marks a shape for removal.
         *
         * @param shape Shape to remove.
         */
        void markShape(Shape shape) {
            if (sweepFilter.test(shape)) {
                markedForRemoval.add(shape);
                markedForRemoval.addAll(shape.members());
            }
        }

        /**
         * Gets the set of shapes that refer to the given shape.
         *
         * @param shape Shape to check for relationships to.
         * @return Returns the shapes that reference the given shape.
         */
        Set<Shape> getTargetedFrom(Shape shape) {
            return findRelationshipsTo(shape).map(Relationship::getShape).collect(Collectors.toSet());
        }

        private Stream<Relationship> findRelationshipsTo(Shape shape) {
            return reverseProvider.getNeighbors(shape)
                    .stream()
                    // We are only interested in references to this shape from
                    // other shapes, not references to this shape that the shape
                    // contains (like members).
                    .filter(rel -> {
                        RelationshipType type = rel.getRelationshipType();
                        return type.getDirection() == RelationshipDirection.DIRECTED && !type.isMemberBinding();
                    })
                    // Don't allow recursive member references to exclude themselves.
                    // This check ensures that recursive member references don't exclude
                    // themselves from being marked by seeing if the relationship is a member
                    // target (e.g., an aggregate shape that targets a member)
                    .filter(rel -> rel.getRelationshipType() != RelationshipType.MEMBER_TARGET
                            || !rel.getShape().getId().withoutMember().equals(rel.getNeighborShapeId()));
        }
    }
}
