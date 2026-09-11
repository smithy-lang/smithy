/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.IdentifierBindingIndex;
import software.amazon.smithy.model.knowledge.PropertyBindingIndex;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.NestedPropertiesTrait;
import software.amazon.smithy.model.traits.NotPropertyTrait;
import software.amazon.smithy.model.traits.PropertyTrait;
import software.amazon.smithy.model.traits.ResourceIdentifierTrait;
import software.amazon.smithy.model.traits.TraitDefinition;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Validates that resource properties are correctly used in resource-bound operations.
 */
public final class ResourceOperationInputOutputValidator extends AbstractValidator {

    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new LinkedList<>();
        for (ResourceShape resourceShape : model.getResourceShapes()) {
            if (resourceShape.hasProperties()) {
                events.addAll(validateResource(model, resourceShape));
            }
        }
        return events;
    }

    private List<ValidationEvent> validateResource(Model model, ResourceShape resource) {
        List<ValidationEvent> events = new ArrayList<>();
        Set<String> propertiesInOperations = new TreeSet<>();
        PropertyBindingIndex propertyBindingIndex = PropertyBindingIndex.of(model);

        processLifecycleOperationProperties(model,
                resource,
                "put",
                resource.getPut(),
                propertyBindingIndex,
                propertiesInOperations,
                events);
        processLifecycleOperationProperties(model,
                resource,
                "create",
                resource.getCreate(),
                propertyBindingIndex,
                propertiesInOperations,
                events);
        processLifecycleOperationProperties(model,
                resource,
                "read",
                resource.getRead(),
                propertyBindingIndex,
                propertiesInOperations,
                events);
        processLifecycleOperationProperties(model,
                resource,
                "update",
                resource.getUpdate(),
                propertyBindingIndex,
                propertiesInOperations,
                events);
        processLifecycleOperationProperties(model,
                resource,
                "delete",
                resource.getDelete(),
                propertyBindingIndex,
                propertiesInOperations,
                events);
        for (ShapeId operationId : resource.getOperations()) {
            processLifecycleOperationProperties(model,
                    resource,
                    operationId.getName(),
                    Optional.of(operationId),
                    propertyBindingIndex,
                    propertiesInOperations,
                    events);
        }
        resource.getList()
                .ifPresent(operationId -> processCollectionBoundOperation(model,
                        resource,
                        operationId,
                        "list",
                        propertyBindingIndex,
                        propertiesInOperations,
                        events));
        for (ShapeId operationId : resource.getCollectionOperations()) {
            processCollectionBoundOperation(model,
                    resource,
                    operationId,
                    operationId.getName(),
                    propertyBindingIndex,
                    propertiesInOperations,
                    events);
        }

        Set<String> definedProperties = new HashSet<>(resource.getProperties().keySet());
        definedProperties.removeAll(propertiesInOperations);
        for (String propertyNotInLifecycleOp : definedProperties) {
            events.add(error(resource,
                    String.format("Resource property `%s` is not used in the input or output of create, an"
                            + " instance operation, or the list element of a collection operation.",
                            propertyNotInLifecycleOp)));
        }

        return events;
    }

    private void processCollectionBoundOperation(
            Model model,
            ResourceShape resource,
            ShapeId operationId,
            String bindingName,
            PropertyBindingIndex propertyBindingIndex,
            Set<String> propertiesInOperations,
            List<ValidationEvent> events
    ) {
        IdentifierBindingIndex identifierBindingIndex = IdentifierBindingIndex.of(model);
        OperationShape operation = model.expectShape(operationId, OperationShape.class);

        Optional<ShapeId> outputElement = propertyBindingIndex.getOperationOutputElementShape(resource, operationId);
        if (outputElement.isPresent()) {
            Map<String, String> properties =
                    propertyBindingIndex.getOperationOutputElementProperties(resource, operationId);
            propertiesInOperations.addAll(properties.values());
            validateElementMembers(model,
                    resource,
                    bindingName,
                    outputElement.get(),
                    properties,
                    identifierBindingIndex.getOperationOutputElementBindings(resource, operationId),
                    propertyBindingIndex.isOperationOutputElementExplicit(resource, operationId),
                    events);
        } else {
            validateElementMarkerMisuse(model, operation.getOutputShape(), events);
        }

        Optional<ShapeId> inputElement = propertyBindingIndex.getOperationInputElementShape(resource, operationId);
        if (inputElement.isPresent()) {
            Map<String, String> properties =
                    propertyBindingIndex.getOperationInputElementProperties(resource, operationId);
            propertiesInOperations.addAll(properties.values());
            validateElementMembers(model,
                    resource,
                    bindingName,
                    inputElement.get(),
                    properties,
                    identifierBindingIndex.getOperationInputElementBindings(resource, operationId),
                    true,
                    events);
        } else {
            validateElementMarkerMisuse(model, operation.getInputShape(), events);
        }
    }

    /**
     * Reports members marked with {@code @nestedProperties} in a collection
     * operation's input or output that do not resolve to a list of
     * structures. Automatic detection is deliberately suppressed in this
     * case so the misuse is reported rather than silently reinterpreted.
     */
    private void validateElementMarkerMisuse(Model model, ShapeId ioShapeId, List<ValidationEvent> events) {
        model.getShape(ioShapeId).flatMap(Shape::asStructureShape).ifPresent(shape -> {
            for (MemberShape member : shape.members()) {
                if (member.hasTrait(NestedPropertiesTrait.ID)) {
                    events.add(error(member,
                            String.format(
                                    "The `%s` trait applied to a member of a collection operation's input or output "
                                            + "must target a list of structures.",
                                    NestedPropertiesTrait.ID)));
                }
            }
        });
    }

    /**
     * Validates the members of a collection operation's element structure.
     *
     * <p>When the element was explicitly marked with {@code @nestedProperties}
     * the validation is strict and findings are errors, mirroring top-level
     * member validation of instance operations. When the element was
     * automatically detected for a {@code list} lifecycle operation, findings
     * are warnings and members matching neither a property nor an identifier
     * are ignored, since automatically detected element structures may be
     * shared between resources and frequently carry derived data.
     */
    private void validateElementMembers(
            Model model,
            ResourceShape resource,
            String bindingName,
            ShapeId elementId,
            Map<String, String> properties,
            Map<String, String> identifierBindings,
            boolean strict,
            List<ValidationEvent> events
    ) {
        StructureShape element = model.expectShape(elementId, StructureShape.class);
        Set<String> identifierMembers = new HashSet<>(identifierBindings.values());
        Set<ShapeId> notPropertyTraits = computeNotPropertyTraits(model);
        Map<String, Set<MemberShape>> propertyToMemberMappings = new TreeMap<>();

        for (MemberShape member : element.members()) {
            if (identifierMembers.contains(member.getMemberName())) {
                continue;
            }
            String propertyName = properties.get(member.getMemberName());
            if (propertyName == null) {
                if (strict && notPropertyTraits.stream().noneMatch(member::hasTrait)) {
                    events.add(error(member,
                            String.format("Member `%s` does not target a property or identifier for resource "
                                    + "`%s`. If it is an identifier, apply the `%s` trait. If it is a property, apply "
                                    + "the `%s` trait. If it is neither, apply the `%s` trait.",
                                    member.getMemberName(),
                                    resource.getId().toString(),
                                    ResourceIdentifierTrait.ID,
                                    PropertyTrait.ID,
                                    NotPropertyTrait.ID)));
                }
                continue;
            }
            propertyToMemberMappings.computeIfAbsent(propertyName, m -> new TreeSet<>()).add(member);
            ShapeId expectedTarget = resource.getProperties().get(propertyName);
            if (expectedTarget == null) {
                String message = String.format(
                        "This member is marked with the `%s` trait but resolves to `%s`, which is not a resource "
                                + "property of the `%s` resource.",
                        PropertyTrait.ID,
                        propertyName,
                        resource.getId());
                events.add(strict ? error(member, message) : warning(member, message));
            } else if (!expectedTarget.equals(member.getTarget())) {
                String message = String.format(
                        "This member must target `%s`. This member is used as part of the `%s` operation of the `%s` "
                                + "resource and conflicts with its `%s` resource property.",
                        expectedTarget,
                        bindingName,
                        resource.getId(),
                        propertyName);
                events.add(strict ? error(member, message) : warning(member, message));
            }
        }
        validateConflictingProperties(events, element, propertyToMemberMappings, strict);
    }

    private Set<ShapeId> computeNotPropertyTraits(Model model) {
        return model.getShapesWithTrait(NotPropertyTrait.class)
                .stream()
                .filter(shape -> shape.hasTrait(TraitDefinition.ID))
                .map(Shape::toShapeId)
                .collect(Collectors.toSet());
    }

    private void processLifecycleOperationProperties(
            Model model,
            ResourceShape resource,
            String name,
            Optional<ShapeId> operationShapeId,
            PropertyBindingIndex propertyBindingIndex,
            Set<String> propertiesInOperations,
            List<ValidationEvent> events
    ) {
        operationShapeId.flatMap(model::getShape).flatMap(Shape::asOperationShape).ifPresent(operation -> {
            propertiesInOperations.addAll(getAllOperationProperties(propertyBindingIndex, operation));
            validateOperationInputOutput(model,
                    propertyBindingIndex,
                    resource,
                    operation,
                    name,
                    events);
        });
    }

    private List<String> getAllOperationProperties(
            PropertyBindingIndex propertyBindingIndex,
            OperationShape operation
    ) {
        List<String> properties = new ArrayList<>();
        for (MemberShape member : propertyBindingIndex.getInputPropertiesShape(operation).members()) {
            if (propertyBindingIndex.isMemberShapeProperty(member)) {
                properties.add(propertyBindingIndex.getPropertyName(member.getId()).get());
            }
        }
        for (MemberShape member : propertyBindingIndex.getOutputPropertiesShape(operation).members()) {
            if (propertyBindingIndex.isMemberShapeProperty(member)) {
                properties.add(propertyBindingIndex.getPropertyName(member.getId()).get());
            }
        }
        return properties;
    }

    private void validateOperationInputOutput(
            Model model,
            PropertyBindingIndex propertyBindingIndex,
            ResourceShape resource,
            OperationShape operation,
            String lifecycleOperationName,
            List<ValidationEvent> events
    ) {
        validateOperationInput(model, propertyBindingIndex, resource, operation, lifecycleOperationName, events);
        validateOperationOutput(model, propertyBindingIndex, resource, operation, lifecycleOperationName, events);
    }

    private void validateOperationOutput(
            Model model,
            PropertyBindingIndex propertyBindingIndex,
            ResourceShape resource,
            OperationShape operation,
            String lifecycleOperationName,
            List<ValidationEvent> events
    ) {
        Map<String, ShapeId> properties = resource.getProperties();
        Map<String, Set<MemberShape>> propertyToMemberMappings = new TreeMap<>();
        IdentifierBindingIndex identifierBindingIndex = IdentifierBindingIndex.of(model);
        Set<String> identifierMembers = new HashSet<>(identifierBindingIndex
                .getOperationOutputBindings(resource, operation)
                .values());

        Shape shape = propertyBindingIndex.getOutputPropertiesShape(operation);
        for (MemberShape member : shape.members()) {
            if (propertyBindingIndex.isMemberShapeProperty(member)) {
                validateMember(events,
                        lifecycleOperationName,
                        propertyBindingIndex,
                        resource,
                        member,
                        identifierMembers,
                        properties,
                        propertyToMemberMappings);
            }
        }
        validateConflictingProperties(events, shape, propertyToMemberMappings);
    }

    private void validateOperationInput(
            Model model,
            PropertyBindingIndex propertyBindingIndex,
            ResourceShape resource,
            OperationShape operation,
            String lifecycleOperationName,
            List<ValidationEvent> events
    ) {
        Map<String, ShapeId> properties = resource.getProperties();
        Map<String, Set<MemberShape>> propertyToMemberMappings = new TreeMap<>();
        IdentifierBindingIndex identifierBindingIndex = IdentifierBindingIndex.of(model);
        Set<String> identifierMembers = new HashSet<>(identifierBindingIndex
                .getOperationOutputBindings(resource, operation)
                .values());

        Shape shape = propertyBindingIndex.getInputPropertiesShape(operation);
        for (MemberShape member : shape.members()) {
            if (propertyBindingIndex.isMemberShapeProperty(member)) {
                validateMember(events,
                        lifecycleOperationName,
                        propertyBindingIndex,
                        resource,
                        member,
                        identifierMembers,
                        properties,
                        propertyToMemberMappings);
            }
        }
        validateConflictingProperties(events, shape, propertyToMemberMappings);
    }

    private void validateConflictingProperties(
            List<ValidationEvent> events,
            Shape shape,
            Map<String, Set<MemberShape>> propertyToMemberMappings
    ) {
        validateConflictingProperties(events, shape, propertyToMemberMappings, true);
    }

    private void validateConflictingProperties(
            List<ValidationEvent> events,
            Shape shape,
            Map<String, Set<MemberShape>> propertyToMemberMappings,
            boolean strict
    ) {
        for (Map.Entry<String, Set<MemberShape>> entry : propertyToMemberMappings.entrySet()) {
            if (entry.getValue().size() > 1) {
                String message = String.format(
                        "This shape contains members with conflicting resource property names that resolve to '%s': %s",
                        entry.getKey(),
                        entry.getValue()
                                .stream()
                                .map(MemberShape::getMemberName)
                                .collect(Collectors.joining(", ")));
                events.add(strict ? error(shape, message) : warning(shape, message));
            }
        }
    }

    private void validateMember(
            List<ValidationEvent> events,
            String lifecycleOperationName,
            PropertyBindingIndex propertyBindingIndex,
            ResourceShape resource,
            MemberShape member,
            Set<String> identifierMembers,
            Map<String, ShapeId> properties,
            Map<String, Set<MemberShape>> propertyToMemberMappings
    ) {
        String propertyName = propertyBindingIndex.getPropertyName(member.getId()).get();
        propertyToMemberMappings.computeIfAbsent(propertyName, m -> new TreeSet<>()).add(member);
        if (properties.containsKey(propertyName)) {
            if (!properties.get(propertyName).equals(member.getTarget())) {
                ShapeId expectedTarget = properties.get(propertyName);
                events.add(error(member,
                        String.format(
                                "This member must target `%s`. This member is used as part of the `%s` operation of the `%s` "
                                        + "resource and conflicts with its `%s` resource property.",
                                expectedTarget,
                                lifecycleOperationName,
                                resource.getId(),
                                propertyName)));
            }
        } else if (!identifierMembers.contains(member.getMemberName())) {
            events.add(error(member,
                    String.format("Member `%s` does not target a property or identifier for resource "
                            + "`%s`. If it is an identifier, apply the `%s` trait. If it is a property, apply the `%s` trait. "
                            + "If it is neither, apply the `%s` trait.",
                            member.getMemberName(),
                            resource.getId().toString(),
                            ResourceIdentifierTrait.ID,
                            PropertyTrait.ID,
                            NotPropertyTrait.ID)));
        }
    }
}
