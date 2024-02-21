/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.model.validation.validators;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.neighbor.Walker;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.ConstrainShapesTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.SmithyInternalApi;

@SmithyInternalApi
public final class ConstrainShapesValidator extends AbstractValidator {

    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();

        // A map of shapes that need to be validated by multiple constrained traits.
        // This allows walking an applied shape closure just once before testing each selector.
        Map<Shape, List<Shape>> shapesToValidatingTraits = new HashMap<>();

        // Find shapes that have the constrainShapes trait.
        for (Shape traitWithValidators : model.getShapesWithTrait(ConstrainShapesTrait.class)) {
            if (isValid(traitWithValidators, events)) {
                // Find shapes that apply the trait that has the constrainShapes trait.
                for (Shape appliedShape : model.getShapesWithTrait(traitWithValidators.getId())) {
                    shapesToValidatingTraits
                            .computeIfAbsent(appliedShape, i -> new ArrayList<>())
                            .add(traitWithValidators);
                }
            }
        }

        emitIncompatibleShapes(model, shapesToValidatingTraits, events);

        return events;
    }

    private boolean isValid(Shape shape, List<ValidationEvent> events) {
        // Validate that the key IDs are compatible with event IDs.
        ConstrainShapesTrait trait = shape.expectTrait(ConstrainShapesTrait.class);
        boolean isValid = true;
        for (String key : trait.getDefinitions().keySet()) {
            if (!ShapeId.isValidNamespace(key)) {
                events.add(error(shape, trait, "`constrainShapes` key is not a valid event ID: `" + key + '`'));
                isValid = false;
            }
        }
        return isValid;
    }

    private void emitIncompatibleShapes(
            Model model,
            Map<Shape, List<Shape>> shapesToValidatingTraits,
            List<ValidationEvent> events
    ) {
        Walker walker = new Walker(model);

        for (Map.Entry<Shape, List<Shape>> validatingEntry : shapesToValidatingTraits.entrySet()) {
            Shape subject = validatingEntry.getKey();
            List<Shape> traitsToValidate = validatingEntry.getValue();
            // Walk the shape just once before testing each constraint.
            Set<Shape> closure = walker.walkShapes(subject);

            for (Shape constrainedTraitShape : traitsToValidate) {
                ConstrainShapesTrait trait = constrainedTraitShape.expectTrait(ConstrainShapesTrait.class);
                for (Map.Entry<String, ConstrainShapesTrait.Definition> entry : trait.getDefinitions().entrySet()) {
                    String id = entry.getKey();
                    ConstrainShapesTrait.Definition definition = entry.getValue();
                    for (Shape shape : definition.getSelector().select(model, closure)) {
                        String message = String.format(
                                "Found an incompatible shape when validating the constraints of the `%s` trait "
                                + "attached to `%s`: %s",
                                constrainedTraitShape.getId(), subject.getId(), definition.getMessage());
                        events.add(ValidationEvent.builder()
                                .id(id)
                                .sourceLocation(shape)
                                .shapeId(shape)
                                .message(message)
                                .severity(definition.getSeverity())
                                .build());
                    }
                }
            }
        }
    }
}
