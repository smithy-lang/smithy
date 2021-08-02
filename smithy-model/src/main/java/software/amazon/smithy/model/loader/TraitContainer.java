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

package software.amazon.smithy.model.loader;

import static java.lang.String.format;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.DynamicTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.TraitFactory;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.Validator;

/**
 * Aggregates, merges, and creates traits.
 */
public interface TraitContainer {

    /** Shared empty, immutable instance. */
    TraitContainer EMPTY = new TraitContainer() {
        @Override
        public Map<ShapeId, Map<ShapeId, Trait>> traits() {
            return Collections.emptyMap();
        }

        @Override
        public Map<ShapeId, Trait> getTraitsForShape(ShapeId shape) {
            return Collections.emptyMap();
        }

        @Override
        public void clearTraitsForShape(ShapeId shape) {
            // Do nothing.
        }

        @Override
        public Map<ShapeId, Map<ShapeId, Trait>> getTraitsAppliedToPrelude() {
            return Collections.emptyMap();
        }

        @Override
        public void onTrait(ShapeId target, Trait value) {
            throw new UnsupportedOperationException("Cannot add trait " + value.toShapeId() + " to " + target);
        }

        @Override
        public void onTrait(ShapeId target, ShapeId traitId, Node value) {
            throw new UnsupportedOperationException("Cannot add trait " + traitId + " to " + target);
        }
    };

    /**
     * @return Gets all traits in the value map.
     */
    Map<ShapeId, Map<ShapeId, Trait>> traits();

    /**
     * Gets the traits applied to a shape.
     *
     * @param shape Shape to get the traits of.
     * @return Returns the traits of the shape.
     */
    Map<ShapeId, Trait> getTraitsForShape(ShapeId shape);

    /**
     * Clears the traits applied to a shape.
     *
     * <p>This is useful in the event of errors that occur while attempting to
     * create a shape so that validation events about traits applied to shapes
     * that couldn't be created are not emitted.
     *
     * @param shape Shape to clear the traits for.
     */
    void clearTraitsForShape(ShapeId shape);

    /**
     * Gets all traits applied to the prelude.
     *
     * @return Returns the traits applied to prelude shapes.
     */
    Map<ShapeId, Map<ShapeId, Trait>> getTraitsAppliedToPrelude();

    /**
     * Add a trait.
     *
     * @param target Shape to add the trait to.
     * @param value Trait to add.
     */
    void onTrait(ShapeId target, Trait value);

    /**
     * Create and add a trait.
     *
     * @param target Shape to add the trait to.
     * @param traitId Trait shape ID to create.
     * @param value The value to assign to the trait.
     */
    void onTrait(ShapeId target, ShapeId traitId, Node value);

    /**
     * The actual, mutable implementation used to aggregate traits.
     */
    final class TraitHashMap implements TraitContainer {
        private static final Logger LOGGER = Logger.getLogger(TraitContainer.class.getName());

        private final Map<ShapeId, Map<ShapeId, Trait>> targetToTraits = new HashMap<>();
        private final Map<ShapeId, Map<ShapeId, Trait>> traitsAppliedToPrelude = new HashMap<>();
        private final TraitFactory traitFactory;
        private final List<ValidationEvent> events;

        /**
         * @param traitFactory Factory used to create traits.
         * @param events Mutable, by-reference validation event list.
         */
        TraitHashMap(TraitFactory traitFactory, List<ValidationEvent> events) {
            this.traitFactory = Objects.requireNonNull(traitFactory, "Trait factory must not be null");
            this.events = Objects.requireNonNull(events, "events must not be null");
        }

        @Override
        public Map<ShapeId, Map<ShapeId, Trait>> traits() {
            return targetToTraits;
        }

        @Override
        public Map<ShapeId, Trait> getTraitsForShape(ShapeId shape) {
            return targetToTraits.getOrDefault(shape, Collections.emptyMap());
        }

        @Override
        public void clearTraitsForShape(ShapeId shape) {
            targetToTraits.remove(shape);
        }

        @Override
        public Map<ShapeId, Map<ShapeId, Trait>> getTraitsAppliedToPrelude() {
            return traitsAppliedToPrelude;
        }

        @Override
        public void onTrait(ShapeId target, Trait value) {
            ShapeId traitId = value.toShapeId();
            Map<ShapeId, Trait> traits = targetToTraits.computeIfAbsent(target, id -> new HashMap<>());

            if (traits.containsKey(traitId)) {
                Trait previousTrait = traits.get(traitId);

                if (LoaderUtils.isSameLocation(previousTrait, value) && previousTrait.equals(value)) {
                    // The assumption here is that if the trait value is exactly the
                    // same and from the same location, then the same model file was
                    // included more than once in a way that side-steps file and URL
                    // de-duplication. For example, this can occur when a Model is assembled
                    // through a ModelAssembler using model discovery, then the Model is
                    // added to a subsequent ModelAssembler, and then model discovery is
                    // performed again using the same classpath.
                    LOGGER.finest(() -> String.format("Ignoring duplicate %s trait value on %s at same exact location",
                                                      traitId, target));
                    return;
                }

                Node previous = previousTrait.toNode();
                Node updated = value.toNode();

                if (previous.isArrayNode() && updated.isArrayNode()) {
                    // You can merge trait arrays.
                    ArrayNode merged = previous.expectArrayNode().merge(updated.expectArrayNode());
                    value = createTrait(target, traitId, merged);
                    if (value == null) {
                        return;
                    }
                } else if (previous.equals(updated)) {
                    LOGGER.fine(() -> String.format("Ignoring duplicate %s trait value on %s", traitId, target));
                    return;
                } else {
                    events.add(ValidationEvent.builder()
                            .id(Validator.MODEL_ERROR)
                            .severity(Severity.ERROR)
                            .sourceLocation(value.getSourceLocation())
                            .shapeId(target)
                            .message(String.format(
                                    "Conflicting `%s` trait found on shape `%s`. The previous trait was defined at "
                                    + "`%s`, and a conflicting trait was defined at `%s`.",
                                    traitId, target, previous.getSourceLocation(), value.getSourceLocation()))
                            .build());
                    return;
                }
            }

            traits.put(traitId, value);

            if (target.getNamespace().equals(Prelude.NAMESPACE)) {
                traitsAppliedToPrelude.computeIfAbsent(target, id -> new HashMap<>()).put(traitId, value);
            }
        }

        @Override
        public void onTrait(ShapeId target, ShapeId traitId, Node value) {
            Trait trait = createTrait(target, traitId, value);
            if (trait != null) {
                onTrait(target, trait);
            }
        }

        /**
         * Creates a trait and returns null if it can't be created.
         *
         * @param target Shape to apply the trait to.
         * @param traitId Trait shape ID being created.
         * @param traitValue The value to assign to the trait.
         * @return Returns the created trait on success, or null on failure.
         */
        private Trait createTrait(ShapeId target, ShapeId traitId, Node traitValue) {
            try {
                return traitFactory.createTrait(traitId, target, traitValue)
                        .orElseGet(() -> new DynamicTrait(traitId, traitValue));
            } catch (SourceException e) {
                String message = format("Error creating trait `%s`: ", Trait.getIdiomaticTraitName(traitId));
                events.add(ValidationEvent.fromSourceException(e, message)
                                   .toBuilder()
                                   .shapeId(target)
                                   .build());
                return null;
            }
        }
    }
}
