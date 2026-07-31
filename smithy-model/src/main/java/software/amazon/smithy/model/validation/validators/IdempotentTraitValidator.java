/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.IdempotentTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidationUtils;

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

        List<ValidationEvent> events = null;
        for (OperationShape operation : model.getOperationShapesWithTrait(IdempotentTrait.class)) {
            IdempotentTrait trait = operation.expectTrait(IdempotentTrait.class);
            List<ShapeId> existsErrors = trait.getExistsOrNull();
            List<ShapeId> notFoundErrors = trait.getNotFoundOrNull();

            if (existsErrors != null && notFoundErrors != null) {
                if (events == null) {
                    events = new ArrayList<>();
                }
                events.add(error(operation,
                        trait,
                        "The `exists` and `notFound` properties of the `@idempotent` trait are "
                                + "mutually exclusive. Only one may be specified."));
            }
            if (existsErrors != null) {
                events = validateErrors(operation, trait, existsErrors, "exists", events);
            }
            if (notFoundErrors != null) {
                events = validateErrors(operation, trait, notFoundErrors, "notFound", events);
            }
        }
        if (events == null) {
            return Collections.emptyList();
        }
        return events;
    }

    private List<ValidationEvent> validateErrors(
            OperationShape operation,
            IdempotentTrait trait,
            List<ShapeId> errors,
            String propertyName,
            List<ValidationEvent> events
    ) {

        Set<ShapeId> operationErrors = operation.getErrorsSet();
        for (ShapeId errorId : errors) {
            if (!operationErrors.contains(errorId)) {
                if (events == null) {
                    events = new ArrayList<>();
                }
                events.add(error(operation,
                        trait,
                        String.format(
                                "The `%s` property references error `%s`, but it is not in the "
                                        + "operation's errors list. Expected one of %s",
                                propertyName,
                                errorId,
                                ValidationUtils.tickedList(operationErrors))));
            }
        }

        return events;
    }
}
