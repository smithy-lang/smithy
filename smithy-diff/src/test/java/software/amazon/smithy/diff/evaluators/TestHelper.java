/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.diff.evaluators;

import java.util.List;
import java.util.stream.Collectors;
import software.amazon.smithy.model.shapes.ToShapeId;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;

public final class TestHelper {
    public static List<ValidationEvent> findEvents(List<ValidationEvent> events, String eventId) {
        return events.stream()
                .filter(event -> event.containsId(eventId))
                .collect(Collectors.toList());
    }

    public static List<ValidationEvent> findEvents(List<ValidationEvent> events, Severity severity) {
        return events.stream()
                .filter(event -> event.getSeverity() == severity)
                .collect(Collectors.toList());
    }

    public static List<ValidationEvent> findEvents(List<ValidationEvent> events, ToShapeId shapeId) {
        return events.stream()
                .filter(event -> event.getShapeId().filter(id -> id.toShapeId().equals(shapeId)).isPresent())
                .collect(Collectors.toList());
    }
}
