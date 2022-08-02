/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.IdentifierBindingIndex;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidationUtils;
import software.amazon.smithy.utils.FunctionalUtils;
import software.amazon.smithy.utils.OptionalUtils;
import software.amazon.smithy.utils.Pair;

/**
 * Validates that operations bound to resource shapes have identifier
 * bindings for all of the identifiers of the parent of the binding resource,
 * that operations bound to a resource with the {@code collection}
 * trait are bound using a collection binding, and operations bound with
 * no {@code collection} trait are bound using an instance binding.
 */
public final class ResourceIdentifierBindingValidator extends AbstractValidator {

    @Override
    public List<ValidationEvent> validate(Model model) {
        IdentifierBindingIndex bindingIndex = IdentifierBindingIndex.of(model);

        return Stream.of(
                model.shapes(ResourceShape.class)
                        .flatMap(resource -> validateResource(model, resource, bindingIndex)),
                model.shapes(ResourceShape.class)
                        .flatMap(resource -> validateCollectionBindings(model, resource, bindingIndex)),
                model.shapes(ResourceShape.class)
                        .flatMap(resource -> validateInstanceBindings(model, resource, bindingIndex))
        ).flatMap(Function.identity()).collect(Collectors.toList());
    }

    private Stream<ValidationEvent> validateResource(
            Model model,
            ResourceShape parent,
            IdentifierBindingIndex bindingIndex
    ) {
        return parent.getResources().stream()
                .flatMap(childId -> OptionalUtils.stream(model.getShape(childId).flatMap(Shape::asResourceShape)))
                .flatMap(child -> child.getAllOperations().stream()
                        .flatMap(id -> OptionalUtils.stream(model.getShape(id).flatMap(Shape::asOperationShape)))
                        .map(operation -> Pair.of(child, operation)))
                .flatMap(pair -> OptionalUtils.stream(
                        validateOperation(parent, pair.getLeft(), pair.getRight(), bindingIndex)));
    }

    private Optional<ValidationEvent> validateOperation(
            ResourceShape parent,
            ResourceShape child,
            OperationShape operation,
            IdentifierBindingIndex bindingIndex
    ) {
        if (bindingIndex.getOperationBindingType(child, operation) != IdentifierBindingIndex.BindingType.NONE) {
            Set<String> bindings = bindingIndex.getOperationInputBindings(child, operation).keySet();
            Set<String> missing = parent.getIdentifiers().keySet().stream()
                    .filter(FunctionalUtils.not(bindings::contains))
                    .collect(Collectors.toSet());
            if (!missing.isEmpty()) {
                return Optional.of(error(operation, String.format(
                        "This operation is bound to the `%s` resource, which is a child of the `%s` resource, and "
                        + "it is missing the following resource identifier bindings of `%s`: [%s]",
                        child.getId(), parent.getId(), parent.getId(), ValidationUtils.tickedList(missing))));
            }
        }

        return Optional.empty();
    }

    private Stream<ValidationEvent> validateCollectionBindings(
            Model model,
            ResourceShape resource,
            IdentifierBindingIndex identifierIndex
    ) {
        return resource.getAllOperations().stream()
                // Find all collection operations bound to the resource.
                .filter(operation -> identifierIndex.getOperationBindingType(resource, operation)
                        == IdentifierBindingIndex.BindingType.COLLECTION)
                // Get their operation shapes.
                .flatMap(id -> OptionalUtils.stream(model.getShape(id).flatMap(Shape::asOperationShape)))
                // Find collection operations which improperly bind all the resource identifiers.
                .filter(operation -> hasAllIdentifiersBound(resource, operation, identifierIndex))
                .map(operation -> error(operation, format(
                        "This operation is bound as a collection operation on the `%s` resource, but it improperly "
                        + "binds all of the identifiers of the resource to members of the operation input.",
                        resource.getId()
                )));
    }

    private Stream<ValidationEvent> validateInstanceBindings(
            Model model,
            ResourceShape resource,
            IdentifierBindingIndex bindingIndex
    ) {
        return resource.getAllOperations().stream()
                // Find all instance operations bound to the resource.
                .filter(operation -> bindingIndex.getOperationBindingType(resource, operation)
                        == IdentifierBindingIndex.BindingType.INSTANCE)
                // Get their operation shapes.
                .flatMap(id -> OptionalUtils.stream(model.getShape(id).flatMap(Shape::asOperationShape)))
                // Find instance operations which do not bind all of the resource identifiers.
                .filter(operation -> !hasAllIdentifiersBound(resource, operation, bindingIndex))
                .map(operation -> {
                    String expectedIdentifiers = createBindingMessage(resource.getIdentifiers());
                    String boundIds = createBindingMessage(bindingIndex.getOperationInputBindings(resource, operation));
                    return error(operation, format(
                            "This operation does not form a valid instance operation when bound to resource `%s`. "
                                    + "All of the identifiers of the resource were not implicitly or explicitly bound "
                                    + "to the input of the operation. Expected the following identifier bindings: "
                                    + "[%s]. Found the following identifier bindings: [%s]",
                            resource.getId(), expectedIdentifiers, boundIds));
                });
    }

    private boolean hasAllIdentifiersBound(
            ResourceShape resource, OperationShape operation, IdentifierBindingIndex bindingIndex
    ) {
        return bindingIndex.getOperationInputBindings(resource, operation).keySet()
                .containsAll(resource.getIdentifiers().keySet());
    }

    private String createBindingMessage(Map<String, ?> bindings) {
        return bindings.entrySet().stream()
                .map(entry -> format("required member named `%s` that targets `%s`",
                                     entry.getKey(), entry.getValue().toString()))
                .sorted()
                .collect(Collectors.joining(", "));
    }
}
