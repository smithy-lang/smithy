/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.model.loader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Consumer;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.Validator;

/**
 * Represents a shape that is pending other shapes in order to be created.
 */
interface PendingShape {

    /**
     * Create a singular pending shape.
     *
     * @param id ID of the shape.
     * @param sourceLocation Where the shape was defined.
     * @param mixins Mixins the shape is waiting on to resolve.
     * @param creator The factory used to create the resolved shape.
     * @return Returns the created pending shape.
     */
    static PendingShape create(
            ShapeId id,
            FromSourceLocation sourceLocation,
            Set<ShapeId> mixins,
            Consumer<Map<ShapeId, Shape>> creator
    ) {
        return new Singular(id, sourceLocation, mixins, creator);
    }

    /**
     * Merge {@code right} into {@code left} and the updated value.
     *
     * @param left  Left value to merge into.
     * @param right Right value to merge into left.
     * @return Returns the merged value.
     */
    static PendingConflict mergeIntoLeft(PendingShape left, PendingShape right) {
        if (!left.getId().equals(right.getId())) {
            throw new IllegalArgumentException("Cannot merge conflicting shapes with different IDs");
        }

        PendingConflict result;
        if (left instanceof PendingConflict) {
            result = (PendingConflict) left;
        } else {
            result = new PendingConflict(left);
        }

        result.pendingDelegates.add(right);
        result.pendingShapes.addAll(right.getPendingShapes());

        return result;
    }

    /**
     * Gets the shape ID of the shape to create.
     *
     * @return Returns the shape ID.
     */
    ShapeId getId();

    /**
     * Gets the set of shapes that are pending.
     *
     * @return Returns the set of pending shape IDs.
     */
    Set<ShapeId> getPendingShapes();

    /**
     * Builds the shape, and populates the built shape and any members into the
     * given mutable shapeMap.
     *
     * <p>Any conflicts that occurs between shapes is handled implicitly in the
     * given {@code shapeMap}. There is no need to account for conflicts when
     * implementing {@code buildShapes}.
     *
     * @param shapeMap Mutable map of shapes to populate with the created shapes.
     */
    void buildShapes(Map<ShapeId, Shape> shapeMap);

    /**
     * Creates validation events for any shapes that are still unresolved.
     *
     * @param resolved     The map of shapes that have been resolved.
     * @param otherPending The map of other shapes that were not resolved.
     * @return Returns the validation events to emit.
     */
    List<ValidationEvent> unresolved(Map<ShapeId, Shape> resolved, Map<ShapeId, PendingShape> otherPending);

    /**
     * A singular pending shape to resolve.
     */
    class Singular implements PendingShape {
        private final ShapeId id;
        private final SourceLocation sourceLocation;
        private final Set<ShapeId> mixins;
        private final Set<ShapeId> pending;
        private final Consumer<Map<ShapeId, Shape>> creator;

        Singular(
                ShapeId id,
                FromSourceLocation sourceLocation,
                Set<ShapeId> mixins,
                Consumer<Map<ShapeId, Shape>> creator
        ) {
            this.id = id;
            this.sourceLocation = sourceLocation.getSourceLocation();
            this.mixins = mixins;
            this.pending = new HashSet<>(mixins);
            this.creator = creator;
        }

        @Override
        public ShapeId getId() {
            return id;
        }

        @Override
        public Set<ShapeId> getPendingShapes() {
            return pending;
        }

        @Override
        public void buildShapes(Map<ShapeId, Shape> shapeMap) {
            creator.accept(shapeMap);
        }

        @Override
        public List<ValidationEvent> unresolved(Map<ShapeId, Shape> resolved, Map<ShapeId, PendingShape> pending) {
            // A rare case when there are conflicting shapes, and only some are unresolved.
            if (getPendingShapes().isEmpty()) {
                return Collections.emptyList();
            }

            List<ShapeId> nonMixinDependencies = new ArrayList<>();
            List<ShapeId> notFoundShapes = new ArrayList<>();
            List<ShapeId> missingTransitive = new ArrayList<>();
            List<ShapeId> cycles = new ArrayList<>();
            for (ShapeId id : getPendingShapes()) {
                if (resolved.containsKey(id)) {
                    nonMixinDependencies.add(id);
                } else if (!pending.containsKey(id)) {
                    notFoundShapes.add(id);
                } else if (anyMissingTransitiveDependencies(id, resolved, pending, new HashSet<>())) {
                    missingTransitive.add(id);
                } else {
                    cycles.add(id);
                }
            }

            StringJoiner message = new StringJoiner(" ");
            message.add("Unable to resolve mixins;");

            if (!nonMixinDependencies.isEmpty()) {
                message.add("attempted to mixin shapes with no mixin trait: " + nonMixinDependencies);
            }

            if (!notFoundShapes.isEmpty()) {
                message.add("attempted to mixin shapes that are not in the model: " + notFoundShapes);
            }

            if (!missingTransitive.isEmpty()) {
                message.add("unable to resolve due to missing transitive mixins: " + missingTransitive);
            }

            if (!cycles.isEmpty()) {
                message.add("cycles detected between this shape and " + cycles);
            }

            return Collections.singletonList(ValidationEvent.builder()
                    .id(Validator.MODEL_ERROR)
                    .severity(Severity.ERROR)
                    .shapeId(getId())
                    .sourceLocation(sourceLocation)
                    .message(message.toString())
                    .build());
        }

        private boolean anyMissingTransitiveDependencies(
                ShapeId current,
                Map<ShapeId, Shape> resolved,
                Map<ShapeId, PendingShape> otherPending,
                Set<ShapeId> visited
        ) {
            if (resolved.containsKey(current)) {
                return false;
            } else if (!otherPending.containsKey(current)) {
                return true;
            } else if (visited.contains(current)) {
                visited.remove(current);
                return false;
            }

            visited.add(current);
            for (ShapeId next : otherPending.get(current).getPendingShapes()) {
                if (anyMissingTransitiveDependencies(next, resolved, otherPending, visited)) {
                    return true;
                }
            }

            return false;
        }
    }

    /**
     * Aggregates together one or more shapes to implicitly handle aggregating and
     * resolving the dependencies of conflicting shapes defined in the model.
     *
     * <p>Smithy allows conflicting shapes to be defined, and only emits errors if
     * the shapes are not exactly equivalent. It's not possible to know if the
     * shapes are equivalent until they are fully built, so that has to be deferred
     * until all conflicts are built - hence this class.
     */
    class PendingConflict implements PendingShape {
        private final List<PendingShape> pendingDelegates = new ArrayList<>();
        private final Set<ShapeId> pendingShapes;

        PendingConflict(PendingShape pending) {
            this.pendingDelegates.add(pending);
            this.pendingShapes = new HashSet<>(pending.getPendingShapes());
        }

        @Override
        public ShapeId getId() {
            return pendingDelegates.get(0).getId();
        }

        @Override
        public Set<ShapeId> getPendingShapes() {
            return pendingShapes;
        }

        @Override
        public void buildShapes(Map<ShapeId, Shape> shapeMap) {
            for (PendingShape p : pendingDelegates) {
                p.buildShapes(shapeMap);
            }
        }

        @Override
        public List<ValidationEvent> unresolved(Map<ShapeId, Shape> resolved, Map<ShapeId, PendingShape> pending) {
            List<ValidationEvent> events = new ArrayList<>();
            for (PendingShape p : pendingDelegates) {
                events.addAll(p.unresolved(resolved, pending));
            }
            return events;
        }
    }
}
