/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.IdempotentTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Validates that the {@code @idempotent} trait's properties are well-formed:
 *
 * <ul>
 *     <li>Only one of {@code exists} or {@code notFound} may be specified.</li>
 *     <li>Each error in the list must be present in the operation's errors list.</li>
 * </ul>
 */
public final class IdempotentTraitValidator extends AbstractValidator {

    @Override
    public List<ValidationEvent> validate(Model model) {
        if (!model.isTraitApplied(IdempotentTrait.class)) {
            return Collections.emptyList();
        }

        List<ValidationEvent> events = new ArrayList<>();
        for (OperationShape operation : model.getOperationShapesWithTrait(IdempotentTrait.class)) {
            IdempotentTrait trait = operation.expectTrait(IdempotentTrait.class);

            if (trait.getExists().isPresent() && trait.getNotFound().isPresent()) {
                events.add(error(operation,
                        trait,
                        "The `exists` and `notFound` properties of the `@idempotent` trait are "
                                + "mutually exclusive. Only one may be specified."));
            }

            trait.getExists().ifPresent(errors -> events.addAll(validateErrors(operation, trait, errors, "exists")));
            trait.getNotFound()
                    .ifPresent(errors -> events.addAll(validateErrors(operation, trait, errors, "notFound")));
        }
        return events;
    }

    private List<ValidationEvent> validateErrors(
            OperationShape operation,
            IdempotentTrait trait,
            List<ShapeId> errors,
            String propertyName
    ) {
        List<ValidationEvent> events = new ArrayList<>();
        List<ShapeId> operationErrors = operation.getErrors();

        for (ShapeId errorId : errors) {
            if (!operationErrors.contains(errorId)) {
                events.add(error(operation,
                        trait,
                        String.format(
                                "The `%s` property references error `%s`, but it is not in the "
                                        + "operation's errors list. Expected one of: [%s]",
                                propertyName,
                                errorId,
                                operationErrors.stream()
                                        .map(ShapeId::toString)
                                        .sorted()
                                        .collect(Collectors.joining(", ")))));
            }
        }

        return events;
    }
}
