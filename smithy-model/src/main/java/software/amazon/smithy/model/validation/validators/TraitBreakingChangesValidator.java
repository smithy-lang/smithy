/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import java.util.ArrayList;
import java.util.List;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.NodePointer;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.TraitDefinition;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Validates that the JSON pointer paths of the breakingChanges property
 * of a trait refers to valid parts of the model.
 */
public final class TraitBreakingChangesValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        for (Shape shape : model.getShapesWithTrait(TraitDefinition.class)) {
            validateTrait(model, shape, events);
        }
        return events;
    }

    private void validateTrait(Model model, Shape shape, List<ValidationEvent> events) {
        TraitDefinition trait = shape.expectTrait(TraitDefinition.class);

        for (int i = 0; i < trait.getBreakingChanges().size(); i++) {
            TraitDefinition.BreakingChangeRule diffRule = trait.getBreakingChanges().get(i);
            // No need to validate empty paths or paths that are "", meaning the entire trait.
            if (diffRule.getPath().isPresent() && !diffRule.getPath().get().toString().equals("")) {
                Shape current = shape;
                NodePointer pointer = diffRule.getPath().get();
                int segment = 0;
                for (String part : pointer.getParts()) {
                    Shape previous = current;
                    current = current.getMember(part)
                            .flatMap(member -> model.getShape(member.getTarget()))
                            .orElse(null);
                    if (current == null) {
                        events.add(emit(shape, i, segment, previous));
                        break;
                    }
                    segment++;
                }
            }
        }
    }

    private ValidationEvent emit(Shape shape, int element, int segment, Shape evaluated) {
        TraitDefinition definition = shape.expectTrait(TraitDefinition.class);
        NodePointer path = definition.getBreakingChanges().get(element).getDefaultedPath();
        return error(shape,
                definition,
                String.format(
                        "Invalid trait breakingChanges element %d, '%s', at segment '%s': "
                                + "Evaluated shape `%s`, a %s, has no member named `%s`",
                        element,
                        path,
                        path.getParts().get(segment),
                        evaluated.getId(),
                        evaluated.getType(),
                        path.getParts().get(segment)));
    }
}
