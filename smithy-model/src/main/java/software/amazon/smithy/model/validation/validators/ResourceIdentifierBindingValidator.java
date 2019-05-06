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
import software.amazon.smithy.model.shapes.ShapeIndex;
import software.amazon.smithy.model.traits.CollectionTrait;
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
        IdentifierBindingIndex bindingIndex = model.getKnowledge(IdentifierBindingIndex.class);
        ShapeIndex index = model.getShapeIndex();

        return Stream.of(
                index.shapes(ResourceShape.class)
                        .flatMap(resource -> validateResource(index, resource, bindingIndex)),
                index.shapes(ResourceShape.class)
                        .flatMap(resource -> validateCollectionBindings(index, resource, bindingIndex)),
                index.shapes(ResourceShape.class)
                        .flatMap(resource -> validateInstanceBindings(index, resource, bindingIndex))
        ).flatMap(Function.identity()).collect(Collectors.toList());
    }

    private Stream<ValidationEvent> validateResource(
            ShapeIndex index,
            ResourceShape parent,
            IdentifierBindingIndex bindingIndex
    ) {
        return parent.getResources().stream()
                .flatMap(childId -> OptionalUtils.stream(index.getShape(childId).flatMap(Shape::asResourceShape)))
                .flatMap(child -> child.getAllOperations().stream()
                        .flatMap(id -> OptionalUtils.stream(index.getShape(id).flatMap(Shape::asOperationShape)))
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
            Set<String> bindings = bindingIndex.getOperationBindings(child, operation).keySet();
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
            ShapeIndex index,
            ResourceShape resource,
            IdentifierBindingIndex identifierIndex
    ) {
        return resource.getAllOperations().stream()
                // Find all operations bound to the resource.
                .flatMap(id -> OptionalUtils.stream(index.getShape(id).flatMap(Shape::asOperationShape)))
                // Create a pair of the operation and collection trait if it's found on the operation.
                .flatMap(operation -> OptionalUtils.stream(
                        operation.getTrait(CollectionTrait.class).map(t -> Pair.of(operation, t))))
                // Only emit events for operations bound using the incorrect binding type.
                .filter(pair -> identifierIndex.getOperationBindingType(resource, pair.getLeft())
                                != IdentifierBindingIndex.BindingType.COLLECTION)
                .map(pair -> error(pair.getLeft(), pair.getRight(), String.format(
                        "This operation is marked with the `collection` trait but is bound to "
                        + "the `%s` resource using an instance binding, meaning that all of the "
                        + "identifiers of the resource are bound to members of the operation input.",
                        resource.getId())));
    }

    private Stream<ValidationEvent> validateInstanceBindings(
            ShapeIndex index,
            ResourceShape resource,
            IdentifierBindingIndex bindingIndex
    ) {
        return resource.getAllOperations().stream()
                // Find all operations bound to the resource with the collection trait.
                .flatMap(id -> OptionalUtils.stream(index.getShape(id).flatMap(Shape::asOperationShape)))
                .filter(operation -> !operation.hasTrait(CollectionTrait.class))
                // Only emit events for operations bound using the incorrect binding type.
                .filter(operation -> bindingIndex.getOperationBindingType(resource, operation)
                                != IdentifierBindingIndex.BindingType.INSTANCE)
                .map(operation -> {
                    String expectedIdentifiers = createBindingMessage(resource.getIdentifiers());
                    String boundIds = createBindingMessage(bindingIndex.getOperationBindings(resource, operation));
                    return error(operation, format(
                            "This operation does not form a valid instance operation when bound to resource `%s`. "
                            + "All of the identifiers of the resource were not implicitly or explicitly bound to "
                            + "the input of the operation. Expected the following identifier bindings: [%s]. "
                            + "Found the following identifier bindings: [%s]",
                            resource.getId(), expectedIdentifiers, boundIds));
                });
    }

    private String createBindingMessage(Map<String, ?> bindings) {
        return bindings.entrySet().stream()
                .map(entry -> format("required member named `%s` that targets `%s`",
                                     entry.getKey(), entry.getValue().toString()))
                .sorted()
                .collect(Collectors.joining(", "));
    }
}
