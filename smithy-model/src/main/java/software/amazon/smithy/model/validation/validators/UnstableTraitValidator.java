/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.List;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.UnstableTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Emits a validation event if a model contains shapes that are bound to unstable traits.
 */
public final class UnstableTraitValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>(0);

        for (Shape trait : model.getShapesWithTrait(UnstableTrait.class)) {
            for (Shape appliedTo : model.getShapesWithTrait(trait)) {
                events.add(warning(
                        appliedTo,
                        appliedTo.findTrait(trait.getId()).get(), // point to the applied trait which for sure exists.
                        format("This shape applies a trait that is unstable: %s", trait.toShapeId()),
                        trait.toShapeId().toString()));
            }
        }

        return events;
    }
}
