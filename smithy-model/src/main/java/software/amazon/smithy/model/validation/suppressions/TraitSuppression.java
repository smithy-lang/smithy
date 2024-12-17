/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.suppressions;

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
        if (!event.getShapeId().filter(shape::equals).isPresent()) {
            return false;
        }

        for (String value : trait.getValues()) {
            if (event.containsId(value)) {
                return true;
            }
        }

        return false;
    }
}
