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
