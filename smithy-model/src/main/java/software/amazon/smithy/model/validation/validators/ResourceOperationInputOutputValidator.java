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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.IdentifierBindingIndex;
import software.amazon.smithy.model.knowledge.OperationIndex;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.NestedPropertiesTrait;
import software.amazon.smithy.model.traits.NotPropertyTrait;
import software.amazon.smithy.model.traits.PropertyTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Validates that resource are applied appropriately to resources.
 */
public final class ResourceOperationInputOutputValidator extends AbstractValidator {

    @Override
    public List<ValidationEvent> validate(Model model) {
        return model.shapes(ResourceShape.class)
                .filter(ResourceShape::hasProperties)
                .flatMap(shape -> validateResource(model, shape).stream())
                .collect(Collectors.toList());
    }

    private List<ValidationEvent> validateResource(Model model, ResourceShape resource) {
        List<ValidationEvent> events = new ArrayList<>();

        Set<String> propertiesInOperations = new TreeSet<>();
        OperationIndex operationIndex = OperationIndex.of(model);
        IdentifierBindingIndex identifierBindingIndex = IdentifierBindingIndex.of(model);

        resource.getPut().flatMap(model::getShape).flatMap(Shape::asOperationShape).ifPresent(operation -> {
            Set<String> identifierMembers = new HashSet<>(identifierBindingIndex
                    .getAllOperationIdentifiers(resource, operation));
            propertiesInOperations.addAll(getAllOperationProperties(model, identifierMembers,
                    operationIndex, operation));
            validateOperationInputOutput(model, operationIndex, resource, operation, "put", events);
        });

        resource.getCreate().flatMap(model::getShape).flatMap(Shape::asOperationShape).ifPresent(operation -> {
            Set<String> identifierMembers = new HashSet<>(identifierBindingIndex
                    .getAllOperationIdentifiers(resource, operation));
            propertiesInOperations.addAll(getAllOperationProperties(model, identifierMembers,
                    operationIndex, operation));
            validateOperationInputOutput(model, operationIndex, resource, operation, "create", events);
        });

        resource.getRead().flatMap(model::getShape).flatMap(Shape::asOperationShape).ifPresent(operation -> {
            Set<String> identifierMembers = new HashSet<>(identifierBindingIndex
                    .getAllOperationIdentifiers(resource, operation));
            propertiesInOperations.addAll(getAllOperationProperties(model, identifierMembers,
                    operationIndex, operation));
            validateOperationInputOutput(model, operationIndex, resource, operation, "read", events);
        });

        resource.getUpdate().flatMap(model::getShape).flatMap(Shape::asOperationShape).ifPresent(operation -> {
            Set<String> identifierMembers = new HashSet<>(identifierBindingIndex
                    .getAllOperationIdentifiers(resource, operation));
            propertiesInOperations.addAll(getAllOperationProperties(model, identifierMembers,
                    operationIndex, operation));
            validateOperationInputOutput(model, operationIndex, resource, operation, "update", events);
        });

        resource.getDelete().flatMap(model::getShape).flatMap(Shape::asOperationShape).ifPresent(operation -> {
            Set<String> identifierMembers = new HashSet<>(identifierBindingIndex
                    .getAllOperationIdentifiers(resource, operation));
            propertiesInOperations.addAll(getAllOperationProperties(model, identifierMembers,
                    operationIndex, operation));
            validateOperationInputOutput(model, operationIndex, resource, operation, "delete", events);
        });

        Set<String> definedProperties = new HashSet<>(resource.getProperties().keySet());
        definedProperties.removeAll(propertiesInOperations);
        for (String propertyNotInLifecycleOp : definedProperties) {
            events.add(error(resource, String.format("Resource shape's `%s` property is not mapped to an input or "
                     + "output member for any lifecycle operation.", propertyNotInLifecycleOp)));
        }

        return events;
    }

    private List<String> getAllOperationProperties(
            Model model,
            Set<String> identifiers,
            OperationIndex index,
            OperationShape operation
    ) {
        List<String> properties = new ArrayList<>();
        getInputPropertiesShape(model, index, operation).ifPresent(inputShape -> {
            for (MemberShape member : inputShape.members()) {
                if (!isMemberIgnoredForProperties(member, identifiers, model)) {
                    properties.add(getPropertyNameFromMember(member));
                }
            }
        });
        getOutputPropertiesShape(model, index, operation).ifPresent(outputShape -> {
            for (MemberShape member : outputShape.members()) {
                if (!isMemberIgnoredForProperties(member, identifiers, model)) {
                    properties.add(getPropertyNameFromMember(member));
                }
            }
        });
        return properties;
    }

    private String getPropertyNameFromMember(MemberShape member) {
        return member.getTrait(PropertyTrait.class)
                    .flatMap(trait -> trait.getName()).orElse(member.getMemberName());
    }

    private boolean isMemberIgnoredForProperties(MemberShape member, Set<String> identifierMembers, Model model) {
        return member.hasTrait(NotPropertyTrait.class) || member.hasTrait(NestedPropertiesTrait.class)
                || identifierMembers.contains(member.getMemberName())
                || member.getAllTraits().values().stream()
                    .filter(trait -> model.getShape(trait.toShapeId())
                                            .map((shape -> shape.getTrait(NotPropertyTrait.class)
                                                                    .isPresent())).orElse(false))
                    .findAny().isPresent();
    }

    private void validateOperationInputOutput(
        Model model,
        OperationIndex operationIndex,
        ResourceShape resource,
        OperationShape operation,
        String lifecycleOperationName,
        List<ValidationEvent> events
    ) {
        validateOperationInput(model, operationIndex, resource, operation, lifecycleOperationName, events);
        validateOperationOutput(model, operationIndex, resource, operation, lifecycleOperationName, events);
    }

    private void validateOperationOutput(
        Model model,
        OperationIndex operationIndex,
        ResourceShape resource,
        OperationShape operation,
        String lifecycleOperationName,
        List<ValidationEvent> events
    ) {
        Map<String, ShapeId> properties = resource.getProperties();
        Map<String, Set<MemberShape>> propertyToMemberMappings = new TreeMap<>();
        IdentifierBindingIndex identifierBindingIndex = IdentifierBindingIndex.of(model);
        Set<String> identifierMembers = new HashSet<>(identifierBindingIndex
            .getAllOperationIdentifiers(resource, operation));

        Optional<StructureShape> propertyShape = getOutputPropertiesShape(model, operationIndex, operation);
        propertyShape.ifPresent(shape -> {
            for (MemberShape member : shape.members()) {
                validateMember(events, lifecycleOperationName, model, resource, member,
                    identifierMembers, properties, propertyToMemberMappings);
            }
            validateConflictingProperties(events, shape, propertyToMemberMappings);

            if (!shape.getId().equals(operationIndex.getOutputShape(operation).get().getId())) {
                //This mismatch implies NestedPropertiesTrait has been used, verify all
                operationIndex.getOutputMembers(operation).values().stream()
                    .filter(memberShape -> !isMemberIgnoredForProperties(memberShape, identifierMembers, model))
                    .forEach(memberShape -> events.add(error(memberShape,
                        String.format("Member '%s' must have @notProperty trait applied",
                            memberShape.getMemberName()))));
            }
        });
    }

    private void validateOperationInput(
        Model model,
        OperationIndex operationIndex,
        ResourceShape resource,
        OperationShape operation,
        String lifecycleOperationName,
        List<ValidationEvent> events
    ) {
        Map<String, ShapeId> properties = resource.getProperties();
        Map<String, Set<MemberShape>> propertyToMemberMappings = new TreeMap<>();
        IdentifierBindingIndex identifierBindingIndex = IdentifierBindingIndex.of(model);
        Set<String> identifierMembers = new HashSet<>(identifierBindingIndex
            .getAllOperationIdentifiers(resource, operation));

        Optional<StructureShape> propertyShape = getInputPropertiesShape(model, operationIndex, operation);
        propertyShape.ifPresent(shape -> {
            for (MemberShape member : shape.members()) {
                validateMember(events, lifecycleOperationName, model, resource, member,
                    identifierMembers, properties, propertyToMemberMappings);
            }
            validateConflictingProperties(events, shape, propertyToMemberMappings);

            if (!shape.getId().equals(operationIndex.getInputShape(operation).get().getId())) {
                //This mismatch implies NestedPropertiesTrait has been used, verify all
                operationIndex.getInputMembers(operation).values().stream()
                    .filter(memberShape -> !isMemberIgnoredForProperties(memberShape, identifierMembers, model))
                    .forEach(memberShape -> events.add(error(memberShape,
                        String.format("Member '%s' must use @notProperty", memberShape.getMemberName()))));
            }
        });
    }

    private void validateConflictingProperties(
        List<ValidationEvent> events,
        StructureShape shape,
        Map<String, Set<MemberShape>> propertyToMemberMappings
    ) {
        for (Map.Entry<String, Set<MemberShape>> entry : propertyToMemberMappings.entrySet()) {
            if (entry.getValue().size() > 1) {
                events.add(error(shape, String.format(
                        "This shape contains members with conflicting property names that resolve to '%s': %s",
                        entry.getKey(),
                        entry.getValue().stream().map(MemberShape::getMemberName)
                            .collect(Collectors.joining(", ")))));
            }
        }
    }

    private void validateMember(
        List<ValidationEvent> events,
        String lifecycleOperationName,
        Model model,
        ResourceShape resource,
        MemberShape member,
        Set<String> identifierMembers,
        Map<String, ShapeId> properties,
        Map<String, Set<MemberShape>> propertyToMemberMappings
    ) {
        if (!isMemberIgnoredForProperties(member, identifierMembers, model)) {
            String propertyName = getPropertyNameFromMember(member);
            propertyToMemberMappings.computeIfAbsent(propertyName,
                m -> new TreeSet<>()).add(member);

            if (properties.containsKey(propertyName)) {
                if (!properties.get(propertyName).equals(member.getTarget())) {
                    events.add(error(resource, String.format("The resource property `%s` has a conflicting"
                        + " target shape `%s` with the `%s` lifecycle operation which targets `%s`.",
                        propertyName, lifecycleOperationName, properties)));
                }
            } else {
                events.add(error(member, String.format("Member `%s` targets does not target a resource property",
                    member.getMemberName(),
                    propertyName)));
            }
        }
    }

    private Optional<StructureShape> getOutputPropertiesShape(
        Model model,
        OperationIndex operationIndex,
        OperationShape operation
    ) {
        return operationIndex.getOutputMembers(operation).values().stream()
            .filter(member -> member.hasTrait(NestedPropertiesTrait.class))
            .map(member -> model.getShape(member.getTarget()).flatMap(s -> s.asStructureShape()))
            .findFirst().orElse(operationIndex.getOutputShape(operation).flatMap(s -> s.asStructureShape()));
    }

    private Optional<StructureShape> getInputPropertiesShape(
        Model model,
        OperationIndex operationIndex,
        OperationShape operation
    ) {
        return operationIndex.getInputMembers(operation).values().stream()
            .filter(member -> member.hasTrait(NestedPropertiesTrait.class))
            .map(member -> model.getShape(member.getTarget()).flatMap(s -> s.asStructureShape()))
            .findFirst().orElse(operationIndex.getInputShape(operation).flatMap(s -> s.asStructureShape()));
    }
}
