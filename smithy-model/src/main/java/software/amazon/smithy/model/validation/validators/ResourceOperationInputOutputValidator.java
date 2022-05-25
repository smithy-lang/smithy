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
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.IdentifierBindingIndex;
import software.amazon.smithy.model.knowledge.MemberPropertyIndex;
import software.amazon.smithy.model.knowledge.OperationIndex;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Validates that resource properties are correctly used in resource-bound operations.
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
        MemberPropertyIndex memberPropertyIndex = MemberPropertyIndex.of(model);

        resource.getPut().flatMap(model::getShape).flatMap(Shape::asOperationShape).ifPresent(operation -> {
            propertiesInOperations.addAll(getAllOperationProperties(memberPropertyIndex, operation));
            validateOperationInputOutput(model, memberPropertyIndex, operationIndex, resource, operation,
                    "put", events);
        });

        resource.getCreate().flatMap(model::getShape).flatMap(Shape::asOperationShape).ifPresent(operation -> {
            propertiesInOperations.addAll(getAllOperationProperties(memberPropertyIndex, operation));
            validateOperationInputOutput(model, memberPropertyIndex, operationIndex, resource, operation,
                    "create", events);
        });

        resource.getRead().flatMap(model::getShape).flatMap(Shape::asOperationShape).ifPresent(operation -> {
            propertiesInOperations.addAll(getAllOperationProperties(memberPropertyIndex, operation));
            validateOperationInputOutput(model, memberPropertyIndex, operationIndex, resource, operation,
                    "read", events);
        });

        resource.getUpdate().flatMap(model::getShape).flatMap(Shape::asOperationShape).ifPresent(operation -> {
            propertiesInOperations.addAll(getAllOperationProperties(memberPropertyIndex, operation));
            validateOperationInputOutput(model, memberPropertyIndex, operationIndex, resource, operation,
                    "update", events);
        });

        resource.getDelete().flatMap(model::getShape).flatMap(Shape::asOperationShape).ifPresent(operation -> {
            propertiesInOperations.addAll(getAllOperationProperties(memberPropertyIndex, operation));
            validateOperationInputOutput(model, memberPropertyIndex, operationIndex, resource, operation,
                    "delete", events);
        });

        for (ShapeId operationId : resource.getOperations()) {
            model.getShape(operationId).flatMap(Shape::asOperationShape).ifPresent(operation -> {
                propertiesInOperations.addAll(getAllOperationProperties(memberPropertyIndex, operation));
                validateOperationInputOutput(model, memberPropertyIndex, operationIndex, resource, operation,
                        operation.getId().getName(), events);
            });
        }

        Set<String> definedProperties = new HashSet<>(resource.getProperties().keySet());
        definedProperties.removeAll(propertiesInOperations);
        for (String propertyNotInLifecycleOp : definedProperties) {
            events.add(error(resource, String.format("Resource property `%s` is not used in the input or output"
                    + " of any lifecycle operation.", propertyNotInLifecycleOp)));
        }

        return events;
    }

    private List<String> getAllOperationProperties(
            MemberPropertyIndex memberPropertyIndex,
            OperationShape operation
    ) {
        List<String> properties = new ArrayList<>();
        for (MemberShape member : memberPropertyIndex.getInputPropertiesShape(operation).members()) {
            if (memberPropertyIndex.isMemberShapeProperty(member.getId())) {
                properties.add(memberPropertyIndex.getPropertyName(member.getId()).get());
            }
        }
        for (MemberShape member : memberPropertyIndex.getOutputPropertiesShape(operation).members()) {
            if (memberPropertyIndex.isMemberShapeProperty(member.getId())) {
                properties.add(memberPropertyIndex.getPropertyName(member.getId()).get());
            }
        }
        return properties;
    }

    private void validateOperationInputOutput(
            Model model,
            MemberPropertyIndex memberPropertyIndex,
            OperationIndex operationIndex,
            ResourceShape resource,
            OperationShape operation,
            String lifecycleOperationName,
            List<ValidationEvent> events
    ) {
        validateOperationInput(model, memberPropertyIndex, operationIndex, resource,
                operation, lifecycleOperationName, events);
        validateOperationOutput(model, memberPropertyIndex, operationIndex, resource,
                operation, lifecycleOperationName, events);
    }

    private void validateOperationOutput(
            Model model,
            MemberPropertyIndex memberPropertyIndex,
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
            .getOperationOutputBindings(resource, operation).values());

        Shape shape = memberPropertyIndex.getOutputPropertiesShape(operation);
        for (MemberShape member : shape.members()) {
            if (memberPropertyIndex.isMemberShapeProperty(member.getId())) {
                validateMember(events, lifecycleOperationName, memberPropertyIndex, resource, member,
                        identifierMembers, properties, propertyToMemberMappings);
            }
        }
        validateConflictingProperties(events, shape, propertyToMemberMappings);

        if (!shape.getId().equals(operationIndex.getOutputShape(operation).get().getId())) {
            // This mismatch implies NestedPropertiesTrait has been used, verify all
            operationIndex.getOutputMembers(operation).values().stream()
                    .filter(memberShape -> memberPropertyIndex.doesMemberShapeRequireProperty(memberShape.getId()))
                    .forEach(memberShape -> events.add(error(memberShape,
                            String.format("Member '%s' must have @notProperty trait applied",
                                    memberShape.getMemberName()))));
        }
    }

    private void validateOperationInput(
            Model model,
            MemberPropertyIndex memberPropertyIndex,
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
            .getOperationOutputBindings(resource, operation).values());

        Shape shape = memberPropertyIndex.getInputPropertiesShape(operation);
        for (MemberShape member : shape.members()) {
            if (memberPropertyIndex.isMemberShapeProperty(member.getId())) {
                validateMember(events, lifecycleOperationName, memberPropertyIndex, resource, member,
                        identifierMembers, properties, propertyToMemberMappings);
            }
        }
        validateConflictingProperties(events, shape, propertyToMemberMappings);

        if (!shape.getId().equals(operationIndex.getInputShape(operation).get().getId())) {
            // This mismatch implies NestedPropertiesTrait has been used, verify all
            operationIndex.getInputMembers(operation).values().stream()
                    .filter(memberShape -> memberPropertyIndex.doesMemberShapeRequireProperty(memberShape.getId()))
                    .forEach(memberShape -> events.add(error(memberShape,
                            String.format("Member '%s' must have @notProperty trait applied",
                                    memberShape.getMemberName()))));
        }
    }

    private void validateConflictingProperties(
            List<ValidationEvent> events,
            Shape shape,
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
            MemberPropertyIndex memberPropertyIndex,
            ResourceShape resource,
            MemberShape member,
            Set<String> identifierMembers,
            Map<String, ShapeId> properties,
            Map<String, Set<MemberShape>> propertyToMemberMappings
    ) {
        String propertyName = memberPropertyIndex.getPropertyName(member.getId()).get();
        propertyToMemberMappings.computeIfAbsent(propertyName, m -> new TreeSet<>()).add(member);

        if (properties.containsKey(propertyName)) {
            if (!properties.get(propertyName).equals(member.getTarget())) {
                events.add(error(resource, String.format("The resource property `%s` has a conflicting"
                        + " target shape `%s` with the `%s` lifecycle operation which targets `%s`.",
                        propertyName, lifecycleOperationName, properties)));
            }
        } else if (identifierMembers.contains(member.getMemberName())) {
            /* empty if */
        } else {
            events.add(error(member, String.format("Member `%s` targets does not target a resource property",
                    member.getMemberName(),
                    propertyName)));
        }
    }
}
