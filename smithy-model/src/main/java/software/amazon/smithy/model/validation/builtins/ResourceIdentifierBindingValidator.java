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

package software.amazon.smithy.model.validation.builtins;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.Pair;
import software.amazon.smithy.model.knowledge.IdentifierBindingIndex;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeIndex;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidationUtils;

/**
 * Validates that operations bound to resource shapes have identifier
 * bindings for all of the identifiers of the parent of the binding resource.
 */
public final class ResourceIdentifierBindingValidator extends AbstractValidator {

    @Override
    public List<ValidationEvent> validate(Model model) {
        IdentifierBindingIndex bindingIndex = model.getKnowledge(IdentifierBindingIndex.class);
        ShapeIndex index = model.getShapeIndex();
        return index.shapes(ResourceShape.class)
                .flatMap(resource -> validateResource(index, resource, bindingIndex))
                .collect(Collectors.toList());
    }

    private Stream<ValidationEvent> validateResource(
            ShapeIndex index,
            ResourceShape parent,
            IdentifierBindingIndex bindingIndex
    ) {
        return parent.getResources().stream()
                .flatMap(childId -> index.getShape(childId).flatMap(Shape::asResourceShape).stream())
                .flatMap(child -> child.getAllOperations().stream()
                        .flatMap(id -> index.getShape(id).flatMap(Shape::asOperationShape).stream())
                        .map(operation -> new Pair<>(child, operation)))
                .flatMap(pair -> validateOperation(parent, pair.getLeft(), pair.getRight(), bindingIndex).stream());
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
                    .filter(Predicate.not(bindings::contains))
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
}
