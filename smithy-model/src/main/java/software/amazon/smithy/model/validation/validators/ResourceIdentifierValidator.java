/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Validates that the resource identifiers of children of a resource contain
 * all of the identifiers as their parents.
 */
public final class ResourceIdentifierValidator extends AbstractValidator {

    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        for (ResourceShape resource : model.getResourceShapes()) {
            events.addAll(validatePropertyRedefine(resource, model));
            events.addAll(validateAgainstChildren(resource, model));
        }
        return events;
    }

    private List<ValidationEvent> validatePropertyRedefine(ResourceShape resource, Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        if (resource.hasProperties()) {
            Map<String, String> propertyLowerCaseToActual = new HashMap<>();
            for (String propertyName : resource.getProperties().keySet()) {
                propertyLowerCaseToActual.put(propertyName.toLowerCase(Locale.ENGLISH), propertyName);
            }

            for (String identifier : resource.getIdentifiers().keySet()) {
                if (propertyLowerCaseToActual.containsKey(identifier.toLowerCase(Locale.ENGLISH))) {
                    events.add(error(resource,
                            String.format("Resource identifier `%s` cannot also be a"
                                    + " resource property", identifier)));
                }
            }
        }
        return events;
    }

    private List<ValidationEvent> validateAgainstChildren(ResourceShape resource, Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        for (ShapeId childResourceId : resource.getResources()) {
            ResourceShape childResource = model.expectShape(childResourceId, ResourceShape.class);
            checkForMissing(childResource, resource).ifPresent(e -> events.add(e));
            checkForMismatches(childResource, resource).ifPresent(e -> events.add(e));
        }
        return events;
    }

    private Optional<ValidationEvent> checkForMissing(ResourceShape resource, ResourceShape parent) {
        // Look for identifiers on the parent that are flat-out missing on the child.
        String missingKeys = parent.getIdentifiers()
                .entrySet()
                .stream()
                .filter(entry -> resource.getIdentifiers().get(entry.getKey()) == null)
                .map(Map.Entry::getKey)
                .sorted()
                .collect(Collectors.joining(", "));

        if (missingKeys.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(error(resource,
                String.format(
                        "This resource is bound as a child of `%s`, but it is invalid because its `identifiers` property "
                                + "is missing the following identifiers that are defined in `%s`: [%s]",
                        parent.getId(),
                        parent.getId(),
                        missingKeys)));
    }

    private Optional<ValidationEvent> checkForMismatches(ResourceShape resource, ResourceShape parent) {
        // Look for identifiers on the child that have the same key but target different shapes.
        String mismatchedTargets = parent.getIdentifiers()
                .entrySet()
                .stream()
                .filter(entry -> resource.getIdentifiers().get(entry.getKey()) != null)
                .filter(entry -> !resource.getIdentifiers().get(entry.getKey()).equals(entry.getValue()))
                .map(entry -> String.format(
                        "expected the `%s` member to target `%s`, but found a target of `%s`",
                        entry.getKey(),
                        entry.getValue(),
                        resource.getIdentifiers().get(entry.getKey())))
                .sorted()
                .collect(Collectors.joining("; "));

        if (mismatchedTargets.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(error(resource,
                String.format(
                        "The `identifiers` property of this resource is incompatible with its binding to `%s`: %s",
                        parent.getId(),
                        mismatchedTargets)));
    }
}
