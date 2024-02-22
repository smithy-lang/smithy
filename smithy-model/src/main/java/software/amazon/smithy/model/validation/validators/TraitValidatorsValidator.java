/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.model.validation.validators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.selector.Selector;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.TraitValidatorsTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.SmithyInternalApi;

@SmithyInternalApi
public final class TraitValidatorsValidator extends AbstractValidator {

    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();

        for (Shape traitWithValidators : model.getShapesWithTrait(TraitValidatorsTrait.class)) {
            if (isValid(traitWithValidators, events)) {
                for (Shape appliedShape : model.getShapesWithTrait(traitWithValidators.getId())) {
                    emitIncompatibleShapes(model, traitWithValidators, appliedShape, events);
                }
            }
        }

        return events;
    }

    private boolean isValid(Shape shape, List<ValidationEvent> events) {
        // Validate that the key IDs are compatible with event IDs.
        TraitValidatorsTrait trait = shape.expectTrait(TraitValidatorsTrait.class);
        boolean isValid = true;
        for (String key : trait.getValidators().keySet()) {
            if (!ShapeId.isValidNamespace(key)) {
                events.add(error(shape, trait, "`" + TraitValidatorsTrait.ID + "` key is not a valid event ID: `"
                                               + key + '`'));
                isValid = false;
            }
        }
        return isValid;
    }

    private void emitIncompatibleShapes(
            Model model,
            Shape traitWithValidators,
            Shape appliedShape,
            List<ValidationEvent> events
    ) {
        Selector.StartingContext startingEnv = new Selector.StartingContext(Collections.singleton(appliedShape));
        TraitValidatorsTrait trait = traitWithValidators.expectTrait(TraitValidatorsTrait.class);

        for (Map.Entry<String, TraitValidatorsTrait.Validator> entry : trait.getValidators().entrySet()) {
            String id = entry.getKey();
            TraitValidatorsTrait.Validator definition = entry.getValue();
            for (Shape shape : definition.getSelector().select(model, startingEnv)) {
                String suffix = definition.getMessage().orElse(null);
                events.add(createEvent(traitWithValidators, appliedShape, id, shape,
                                       definition.getSeverity(), suffix));
            }
        }
    }

    private ValidationEvent createEvent(
            Shape traitWithValidators,
            Shape appliedShape,
            String eventId,
            Shape shape,
            Severity severity,
            String suffix
    ) {
        String suffixMessage = suffix == null ? "" : ": " + suffix;
        String message = String.format(
                "Found an incompatible shape when validating the constraints of the `%s` trait "
                + "attached to `%s`%s",
                traitWithValidators.getId(), appliedShape.getId(), suffixMessage);
        return ValidationEvent.builder().id(eventId).shape(shape).message(message).severity(severity).build();
    }
}
