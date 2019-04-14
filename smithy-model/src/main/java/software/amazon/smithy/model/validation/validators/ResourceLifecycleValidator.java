/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.model.validation.validators;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.IdentifierBindingIndex;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeIndex;
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
        IdentifierBindingIndex bindingIndex = model.getKnowledge(IdentifierBindingIndex.class);
        ShapeIndex index = model.getShapeIndex();
        return model.getShapeIndex().shapes(ResourceShape.class)
                .flatMap(shape -> validateResource(index, bindingIndex, shape).stream())
                .collect(Collectors.toList());
    }

    private List<ValidationEvent> validateResource(
            ShapeIndex index,
            IdentifierBindingIndex bindingIndex,
            ResourceShape resource
    ) {
        List<ValidationEvent> events = new ArrayList<>();
        events.addAll(resource.getCreate().flatMap(index::getShape).flatMap(Shape::asOperationShape)
                .map(operation -> validateCreate(resource, operation, bindingIndex))
                .orElseGet(Collections::emptyList));

        resource.getRead().flatMap(index::getShape).flatMap(Shape::asOperationShape).ifPresent(operation -> {
            ensureIsInstance(bindingIndex, resource, operation, "read").ifPresent(events::add);
            validateReadonly(resource, operation, "read", true).ifPresent(events::add);
        });

        resource.getUpdate().flatMap(index::getShape).flatMap(Shape::asOperationShape).ifPresent(operation -> {
            ensureIsInstance(bindingIndex, resource, operation, "update").ifPresent(events::add);
            validateReadonly(resource, operation, "update", false).ifPresent(events::add);
        });

        resource.getDelete().flatMap(index::getShape).flatMap(Shape::asOperationShape).ifPresent(operation -> {
            ensureIsInstance(bindingIndex, resource, operation, "delete").ifPresent(events::add);
            validateReadonly(resource, operation, "delete", false).ifPresent(events::add);
            validateIdempotent(resource, operation, "delete", true).ifPresent(events::add);
        });

        resource.getList().flatMap(index::getShape).flatMap(Shape::asOperationShape).ifPresent(operation -> {
            ensureIsCollection(bindingIndex, resource, operation, "list").ifPresent(events::add);
            validateReadonly(resource, operation, "list", true).ifPresent(events::add);
        });

        return events;
    }

    private List<ValidationEvent> validateCreate(
            ResourceShape resource,
            OperationShape operation,
            IdentifierBindingIndex bindingIndex
    ) {
        List<ValidationEvent> events = new ArrayList<>(2);
        validateReadonly(resource, operation, "create", false).ifPresent(events::add);
        IdentifierBindingIndex.BindingType bindingType = bindingIndex.getOperationBindingType(resource, operation);
        if (bindingType == IdentifierBindingIndex.BindingType.INSTANCE) {
            validateIdempotent(resource, operation, "create", true).ifPresent(events::add);
        }
        // Note: Whether or not it uses a valid bindings is validated in ResourceIdentifierBindingValidator.
        return events;
    }

    private Optional<ValidationEvent> ensureIsInstance(
            IdentifierBindingIndex bindingIndex,
            ResourceShape resource,
            OperationShape operation,
            String lifecycle
    ) {
        IdentifierBindingIndex.BindingType binding = bindingIndex.getOperationBindingType(resource, operation);
        // Ignore invalid bindings since that is validated elsewhere.
        if (binding == IdentifierBindingIndex.BindingType.INSTANCE
                || binding == IdentifierBindingIndex.BindingType.NONE) {
            return Optional.empty();
        }
        String expectedIdentifiers = createBindingMessage(resource.getIdentifiers());
        String boundIds = createBindingMessage(bindingIndex.getOperationBindings(resource, operation));
        return Optional.of(error(resource, format(
                "The `%s` operation bound to this resource as the `%s` lifecycle operation does not form a valid "
                + "instance operation. This means that all of the identifiers of the resource were not implicitly or "
                + "explicitly bound to the input of the operation. Expected the following identifier bindings: [%s]. "
                + "Found the following identifier bindings: [%s]",
                operation.getId(), lifecycle, expectedIdentifiers, boundIds)));
    }

    private String createBindingMessage(Map<String, ?> bindings) {
        return bindings.entrySet().stream()
                .map(entry -> format("required member named `%s` that targets `%s`",
                                     entry.getKey(), entry.getValue().toString()))
                .sorted()
                .collect(Collectors.joining(", "));
    }

    private Optional<ValidationEvent> ensureIsCollection(
            IdentifierBindingIndex bindingIndex,
            ResourceShape resource,
            OperationShape operation,
            String lifecycle
    ) {
        IdentifierBindingIndex.BindingType binding = bindingIndex.getOperationBindingType(resource, operation);
        // We're specifically looking for instance bindings that should have been collection.
        if (binding != IdentifierBindingIndex.BindingType.INSTANCE) {
            return Optional.empty();
        }

        String bindings = createBindingMessage(bindingIndex.getOperationBindings(resource, operation));
        // Note: whether or not grandparent bindings are present is validated in ResourceIdentifierBindingValidator.
        return Optional.of(error(resource, format(
                "The `%s` operation bound to this resource as the %s lifecycle operation does not form a valid "
                + "collection operation because all of the identifiers of the resource were bound to the input: [%s]",
                operation.getId(), lifecycle, bindings)));
    }

    private Optional<ValidationEvent> validateReadonly(
            ResourceShape resource,
            OperationShape operation,
            String lifecycle,
            boolean requireReadOnly
    ) {
        boolean isReadonly = operation.getTrait(ReadonlyTrait.class).isPresent();
        if (isReadonly == requireReadOnly) {
            return Optional.empty();
        }
        return Optional.of(error(resource, format(
                "The `%s` lifecycle operation of this resource targets an invalid operation, `%s`. The targeted "
                + "operation %s be marked with the readonly trait.",
                lifecycle, operation.getId(), requireReadOnly ? "must" : "must not")));
    }

    private Optional<ValidationEvent> validateIdempotent(
            ResourceShape resource,
            OperationShape operation,
            String lifecycle,
            boolean requireIdempotent
    ) {
        boolean isIdempotent = operation.getTrait(IdempotentTrait.class).isPresent();
        if (requireIdempotent == isIdempotent) {
            return Optional.empty();
        }
        return Optional.of(error(resource, format(
                "The `%s` lifecycle operation of this resource targets an invalid operation, `%s`. The targeted "
                + "operation %s be marked as idempotent.",
                lifecycle, operation.getId(), requireIdempotent ? "must" : "must not")));
    }
}
