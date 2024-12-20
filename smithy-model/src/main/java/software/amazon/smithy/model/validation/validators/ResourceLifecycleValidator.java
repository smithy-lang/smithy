/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.IdempotentTrait;
import software.amazon.smithy.model.traits.ReadonlyTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Validates that resource are applied appropriately to resources.
 */
public final class ResourceLifecycleValidator extends AbstractValidator {

    @Override
    public List<ValidationEvent> validate(Model model) {
        return model.shapes(ResourceShape.class)
                .flatMap(shape -> validateResource(model, shape).stream())
                .collect(Collectors.toList());
    }

    private List<ValidationEvent> validateResource(Model model, ResourceShape resource) {
        List<ValidationEvent> events = new ArrayList<>();

        // Note: Whether or not these use a valid bindings is validated in ResourceIdentifierBindingValidator.
        resource.getPut().flatMap(model::getShape).flatMap(Shape::asOperationShape).ifPresent(operation -> {
            validateReadonly(resource, operation, "put", false).ifPresent(events::add);
            validateIdempotent(resource, operation, "put", "").ifPresent(events::add);
        });

        resource.getCreate().flatMap(model::getShape).flatMap(Shape::asOperationShape).ifPresent(operation -> {
            validateReadonly(resource, operation, "create", false).ifPresent(events::add);
        });

        resource.getRead().flatMap(model::getShape).flatMap(Shape::asOperationShape).ifPresent(operation -> {
            validateReadonly(resource, operation, "read", true).ifPresent(events::add);
        });

        resource.getUpdate().flatMap(model::getShape).flatMap(Shape::asOperationShape).ifPresent(operation -> {
            validateReadonly(resource, operation, "update", false).ifPresent(events::add);
        });

        resource.getDelete().flatMap(model::getShape).flatMap(Shape::asOperationShape).ifPresent(operation -> {
            validateReadonly(resource, operation, "delete", false).ifPresent(events::add);
            validateIdempotent(resource, operation, "delete", "").ifPresent(events::add);
        });

        resource.getList().flatMap(model::getShape).flatMap(Shape::asOperationShape).ifPresent(operation -> {
            validateReadonly(resource, operation, "list", true).ifPresent(events::add);
        });

        return events;
    }

    private Optional<ValidationEvent> validateReadonly(
            ResourceShape resource,
            OperationShape operation,
            String lifecycle,
            boolean requireReadOnly
    ) {
        if (requireReadOnly == operation.hasTrait(ReadonlyTrait.class)) {
            return Optional.empty();
        }

        return Optional.of(error(resource,
                format(
                        "The `%s` lifecycle operation of this resource targets an invalid operation, `%s`. The targeted "
                                + "operation %s be marked with the readonly trait.",
                        lifecycle,
                        operation.getId(),
                        requireReadOnly ? "must" : "must not")));
    }

    private Optional<ValidationEvent> validateIdempotent(
            ResourceShape resource,
            OperationShape operation,
            String lifecycle,
            String additionalMessage
    ) {
        if (operation.hasTrait(IdempotentTrait.class)) {
            return Optional.empty();
        }

        return Optional.of(error(resource,
                format(
                        "The `%s` lifecycle operation of this resource targets an invalid operation, `%s`. The targeted "
                                + "operation must be marked as idempotent.%s",
                        lifecycle,
                        operation.getId(),
                        additionalMessage)));
    }
}
