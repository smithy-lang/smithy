/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
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
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.JsonNameTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;

public final class JsonNameValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        Set<ShapeId> visitedContainers = new HashSet<>();

        // Find every member marked with a jsonName trait. The containing shapes of these members are
        // the only structure/union shapes that need to be validated.
        for (MemberShape member : model.getMemberShapesWithTrait(JsonNameTrait.class)) {
            // If the container hasn't been visited yet, then validate it's members.
            if (visitedContainers.add(member.getContainer())) {
                validateMembersOfContainer(model.expectShape(member.getContainer()), events);
            }
        }
        return events;
    }

    private void validateMembersOfContainer(Shape container, List<ValidationEvent> events) {
        Map<String, Set<MemberShape>> memberMappings = new TreeMap<>();
        for (MemberShape m : container.members()) {
            String jsonName = m.getTrait(JsonNameTrait.class)
                    .map(JsonNameTrait::getValue)
                    .orElseGet(m::getMemberName);
            memberMappings.computeIfAbsent(jsonName, n -> new TreeSet<>()).add(m);
        }

        for (Map.Entry<String, Set<MemberShape>> entry : memberMappings.entrySet()) {
            if (entry.getValue().size() > 1) {
                events.add(error(container,
                        String.format(
                                "This shape contains members with conflicting JSON names that resolve to '%s': %s",
                                entry.getKey(),
                                entry.getValue()
                                        .stream()
                                        .map(MemberShape::getMemberName)
                                        .collect(Collectors.joining(", ")))));
            }
        }
    }
}
