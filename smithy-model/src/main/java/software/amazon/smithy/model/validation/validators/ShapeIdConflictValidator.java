/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Ensures that no two shapes in the model have the same case-insensitive
 * shape ID.
 */
public final class ShapeIdConflictValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        // Set initial capacity of the HashMap under the assumption most models will have valid and unique IDs.
        int initialCapacity = (int) Math.ceil(model.getShapeIds().size() * 0.75 + 1);
        Map<String, Collection<ShapeId>> conflicts = new HashMap<>(initialCapacity);

        for (ShapeId id : model.getShapeIds()) {
            // Most shapes will have no conflicts, so set the initial capacity of the ArrayList to 1.
            conflicts.computeIfAbsent(id.toString().toLowerCase(Locale.ENGLISH), i -> new ArrayList<>(1)).add(id);
        }

        List<ValidationEvent> events = new ArrayList<>();
        for (Map.Entry<String, Collection<ShapeId>> entry : conflicts.entrySet()) {
            if (entry.getValue().size() > 1) {
                for (ShapeId value : entry.getValue()) {
                    String collideString = entry.getValue()
                            .stream()
                            .filter(id -> !id.equals(value))
                            .sorted()
                            .map(id -> "`" + id + "`")
                            .collect(Collectors.joining(", "));
                    events.add(error(model.expectShape(value),
                            String.format(
                                    "Shape ID `%s` conflicts with other shape IDs in the model: [%s]",
                                    value,
                                    collideString)));
                }
            }
        }

        return events;
    }
}
