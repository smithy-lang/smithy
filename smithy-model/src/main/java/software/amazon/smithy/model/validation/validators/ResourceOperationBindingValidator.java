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

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.IdentifierBindingIndex;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeIndex;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.OptionalUtils;
import software.amazon.smithy.utils.Pair;

/**
 * Validates the {@code collectionOperation} and {@code instanceOperation}
 * trait bindings for operations bound to resources.
 */
public class ResourceOperationBindingValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        ShapeIndex index = model.getShapeIndex();
        IdentifierBindingIndex identifierIndex = model.getKnowledge(IdentifierBindingIndex.class);

        return Stream.concat(
                validateCollectionBindings(index, identifierIndex),
                validateInstanceBindings(index, identifierIndex)
        ).collect(Collectors.toList());
    }

    private Stream<ValidationEvent> validateCollectionBindings(
            ShapeIndex index,
            IdentifierBindingIndex identifierIndex
    ) {
        return index.shapes(ResourceShape.class)
                .flatMap(resource -> validateResource(index, resource, identifierIndex, "collectionOperation",
                        IdentifierBindingIndex.BindingType.COLLECTION, () -> String.format(
                                "This operation is marked with the `collectionOperation` trait but is bound to "
                                + "the `%s` resource using an instance binding, meaning that all of the "
                                + "identifiers of the resource are bound to members of the operation input.",
                                resource.getId())));
    }

    private Stream<ValidationEvent> validateInstanceBindings(
            ShapeIndex index,
            IdentifierBindingIndex identifierIndex
    ) {
        return index.shapes(ResourceShape.class)
                .flatMap(resource -> validateResource(index, resource, identifierIndex, "instanceOperation",
                        IdentifierBindingIndex.BindingType.INSTANCE, () -> String.format(
                                "This operation is marked with the `instanceOperation` trait but is bound to "
                                + "the `%s` resource using a collection binding, meaning that one or more of the "
                                + "identifiers of the resource are not bound to members of the operation input "
                                + "using required members.",
                                resource.getId())));
    }

    private Stream<ValidationEvent> validateResource(
            ShapeIndex index,
            ResourceShape resource,
            IdentifierBindingIndex identifierIndex,
            String traitName,
            IdentifierBindingIndex.BindingType expectedBinding,
            Supplier<String> errorMessage
    ) {
        return resource.getAllOperations().stream()
                // Find all operations bound to the resource.
                .flatMap(id -> OptionalUtils.stream(index.getShape(id).flatMap(Shape::asOperationShape)))
                // Create a pair of the trait is found on the operation.
                .flatMap(operation -> OptionalUtils.stream(
                        operation.findTrait(traitName).map(t -> Pair.of(operation, t))))
                // Only emit events for operations bound using the incorrect binding type.
                .filter(pair -> identifierIndex.getOperationBindingType(resource, pair.getLeft()) != expectedBinding)
                .map(pair -> error(pair.getLeft(), pair.getRight(), errorMessage.get()));
    }
}
