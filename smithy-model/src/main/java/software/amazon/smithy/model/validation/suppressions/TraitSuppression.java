/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.suppressions;

import java.util.Optional;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.SuppressTrait;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * A suppression based on the {@link SuppressTrait}.
 */
final class TraitSuppression implements Suppression {

    private final ShapeId shape;
    private final SuppressTrait trait;

    TraitSuppression(ShapeId shape, SuppressTrait trait) {
        this.shape = shape;
        this.trait = trait;
    }

    @Override
    public boolean test(ValidationEvent event) {
        return matchingValue(event).isPresent();
    }

    /**
     * Gets the first value of the {@link SuppressTrait} that matches the given event, if any.
     *
     * <p>Tracking the matched value on a per-value basis allows no-op suppressions to be detected
     * for each value of the trait rather than for the trait as a whole.
     *
     * @param event Event to test.
     * @return Returns the matching value of the trait, if any.
     */
    Optional<String> matchingValue(ValidationEvent event) {
        if (!event.getShapeId().filter(shape::equals).isPresent()) {
            return Optional.empty();
        }

        for (String value : trait.getValues()) {
            if (event.containsId(value)) {
                return Optional.of(value);
            }
        }

        return Optional.empty();
    }

    SuppressTrait getTrait() {
        return trait;
    }
}
