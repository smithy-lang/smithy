/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.IdentifierBindingIndex;
import software.amazon.smithy.model.knowledge.IdentifierBindingIndex.BindingType;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.ResourceIdentifierTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidationUtils;

/**
 * Validates that operations bound to resource shapes have identifier
 * bindings for all the identifiers of the parent of the binding resource,
 * that operations bound to a resource with the {@code collection}
 * trait are bound using a collection binding, and operations bound with
 * no {@code collection} trait are bound using an instance binding.
 */
public final class ResourceIdentifierBindingValidator extends AbstractValidator {

    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        validateResourceIdentifierTraits(model, events);
        validateOperationBindings(model, events);
        return events;
    }

    // Check if this shape has conflicting resource identifier bindings due to trait bindings.
    private void validateResourceIdentifierTraits(Model model, List<ValidationEvent> events) {
        for (ShapeId container : findStructuresWithResourceIdentifierTraits(model)) {
            if (!model.getShape(container).isPresent()) {
                continue;
            }

            Shape structure = model.expectShape(container);
            Map<String, Set<String>> bindings = computePotentialStructureBindings(structure);
            for (Map.Entry<String, Set<String>> entry : bindings.entrySet()) {
                // Only emit this event if the potential bindings contains
                // more than the implicit binding.
                if (entry.getValue().size() > 1 && entry.getValue().contains(entry.getKey())) {
                    Set<String> explicitBindings = entry.getValue();
                    explicitBindings.remove(entry.getKey());
                    events.add(warning(structure,
                            String.format(
                                    "Implicit resource identifier for '%s' is overridden by `resourceIdentifier` trait on "
                                            + "members: '%s'",
                                    entry.getKey(),
                                    String.join("', '", explicitBindings))));
                }
            }

            validateResourceIdentifierTraitConflicts(structure, events);
        }
    }

    private Set<ShapeId> findStructuresWithResourceIdentifierTraits(Model model) {
        Set<ShapeId> containers = new HashSet<>();
        for (MemberShape member : model.getMemberShapesWithTrait(ResourceIdentifierTrait.class)) {
            containers.add(member.getContainer());
        }
        return containers;
    }

    private Map<String, Set<String>> computePotentialStructureBindings(Shape structure) {
        Map<String, Set<String>> bindings = new HashMap<>();
        // Ensure no two members are bound to the same identifier.
        for (MemberShape member : structure.members()) {
            String bindingName = member.getTrait(ResourceIdentifierTrait.class)
                    .map(ResourceIdentifierTrait::getValue)
                    .orElseGet(member::getMemberName);
            bindings.computeIfAbsent(bindingName, k -> new HashSet<>()).add(member.getMemberName());
        }
        return bindings;
    }

    private void validateResourceIdentifierTraitConflicts(Shape structure, List<ValidationEvent> events) {
        Map<String, Set<String>> explicitBindings = new HashMap<>();
        // Ensure no two members use a resourceIdentifier trait to bind to
        // the same identifier.
        for (MemberShape member : structure.members()) {
            if (member.hasTrait(ResourceIdentifierTrait.ID)) {
                explicitBindings.computeIfAbsent(member.expectTrait(ResourceIdentifierTrait.class).getValue(),
                        k -> new HashSet<>()).add(member.getMemberName());
            }
        }

        for (Map.Entry<String, Set<String>> entry : explicitBindings.entrySet()) {
            if (entry.getValue().size() > 1) {
                events.add(error(structure,
                        String.format(
                                "Conflicting resource identifier member bindings found for identifier '%s' between "
                                        + "members: '%s'",
                                entry.getKey(),
                                String.join("', '", entry.getValue()))));
            }
        }
    }

    private void validateOperationBindings(Model model, List<ValidationEvent> events) {
        IdentifierBindingIndex bindingIndex = IdentifierBindingIndex.of(model);
        for (ResourceShape resource : model.getResourceShapes()) {
            validateResource(model, resource, bindingIndex, events);
            validateCollectionBindings(model, resource, bindingIndex, events);
            validateInstanceBindings(model, resource, bindingIndex, events);
        }
    }

    private void validateResource(
            Model model,
            ResourceShape parent,
            IdentifierBindingIndex bindingIndex,
            List<ValidationEvent> events
    ) {
        for (ShapeId childId : parent.getResources()) {
            ResourceShape child = model.expectShape(childId, ResourceShape.class);
            for (ShapeId operationId : child.getAllOperations()) {
                OperationShape operation = model.expectShape(operationId, OperationShape.class);
                validateOperation(parent, child, operation, bindingIndex).ifPresent(events::add);
            }
        }
    }

    private Optional<ValidationEvent> validateOperation(
            ResourceShape parent,
            ResourceShape child,
            OperationShape operation,
            IdentifierBindingIndex bindingIndex
    ) {
        if (bindingIndex.getOperationBindingType(child, operation) != BindingType.NONE) {
            Set<String> bindings = bindingIndex.getOperationInputBindings(child, operation).keySet();
            Set<String> missing = new LinkedHashSet<>(parent.getIdentifiers().keySet());
            missing.removeAll(bindings);
            if (!missing.isEmpty()) {
                return Optional.of(error(operation,
                        String.format(
                                "This operation is bound to the `%s` resource, which is a child of the `%s` resource, "
                                        + "and it is missing the following resource identifier bindings of `%s`: [%s]",
                                child.getId(),
                                parent.getId(),
                                parent.getId(),
                                ValidationUtils.tickedList(missing))));
            }
        }

        return Optional.empty();
    }

    private void validateCollectionBindings(
            Model model,
            ResourceShape resource,
            IdentifierBindingIndex bindingIndex,
            List<ValidationEvent> events
    ) {
        for (ShapeId operationId : resource.getAllOperations()) {
            if (bindingIndex.getOperationBindingType(resource, operationId) != BindingType.COLLECTION) {
                continue;
            }

            OperationShape operation = model.expectShape(operationId, OperationShape.class);
            if (hasAllIdentifiersBound(model, resource, operation, bindingIndex)) {
                events.add(error(operation,
                        format(
                                "This operation is bound as a collection operation on the `%s` resource, but it "
                                        + "improperly binds all of the identifiers of the resource to members of the "
                                        + "operation input.",
                                resource.getId())));
            }
        }
    }

    private void validateInstanceBindings(
            Model model,
            ResourceShape resource,
            IdentifierBindingIndex bindingIndex,
            List<ValidationEvent> events
    ) {
        for (ShapeId operationId : resource.getAllOperations()) {
            if (bindingIndex.getOperationBindingType(resource, operationId) != BindingType.INSTANCE) {
                continue;
            }

            OperationShape operation = model.expectShape(operationId, OperationShape.class);
            if (!hasAllIdentifiersBound(model, resource, operation, bindingIndex)) {
                String expectedIdentifiers = createBindingMessage(resource.getIdentifiers());
                String boundIds = createBindingMessage(bindingIndex.getOperationInputBindings(resource, operation));
                events.add(error(operation,
                        format(
                                "This operation does not form a valid instance operation when bound to resource `%s`. "
                                        + "All of the identifiers of the resource were not implicitly or explicitly "
                                        + "bound to the input of the operation. Expected the following identifier "
                                        + "bindings: [%s]. Found the following identifier bindings: [%s]",
                                resource.getId(),
                                expectedIdentifiers,
                                boundIds)));
            }
        }
    }

    private boolean hasAllIdentifiersBound(
            Model model,
            ResourceShape resource,
            OperationShape operation,
            IdentifierBindingIndex bindingIndex
    ) {
        StructureShape inputShape = model.expectShape(operation.getInputShape(), StructureShape.class);
        Map<String, String> bindings = bindingIndex.getOperationInputBindings(resource, operation);
        for (Map.Entry<String, ShapeId> identifier : resource.getIdentifiers().entrySet()) {
            if (!bindings.containsKey(identifier.getKey())) {
                return false;
            }

            MemberShape identifierMember = inputShape.getMember(bindings.get(identifier.getKey())).get();
            if (!identifierMember.getTarget().equals(identifier.getValue())) {
                return false;
            }
        }
        return true;
    }

    private String createBindingMessage(Map<String, ?> bindings) {
        return bindings.entrySet()
                .stream()
                .map(entry -> format("required member named `%s` that targets `%s`",
                        entry.getKey(),
                        entry.getValue().toString()))
                .sorted()
                .collect(Collectors.joining(", "));
    }
}
