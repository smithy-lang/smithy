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

package software.amazon.smithy.model.loader;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.selector.Selector;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Validator defined in a Smithy document.
 */
final class ValidatorDefinition {

    final String name;
    final List<String> namespaces = new ArrayList<>();
    ObjectNode configuration = Node.objectNode();
    SourceLocation sourceLocation = SourceLocation.none();
    Severity severity;
    String message;
    Selector selector;
    private final String id;

    ValidatorDefinition(String name, String id) {
        this.name = name;
        this.id = id;
    }

    List<ValidationEvent> map(Model model, List<ValidationEvent> events) {
        List<ValidationEvent> filtered = new ArrayList<>(events.size());
        Set<ShapeId> candidates = null;

        // If there's a selector, create a list of candidate shape IDs that can be emitted.
        if (selector != null) {
            candidates = selector
                    .shapes(model)
                    .map(Shape::getId)
                    .collect(Collectors.toSet());
        }

        for (ValidationEvent event : events) {
            // Skip events that are not eligible.
            if (!filterEvent(event, candidates)) {
                continue;
            }

            if (name.equals(id) && severity == null && message == null) {
                // Use the event as-is without modifying it.
                filtered.add(event);
            } else {
                // Modify the event by changing the id, severity, or message.
                ValidationEvent.Builder builder = event.toBuilder();
                builder.id(replaceId(event.getId(), id));
                builder.severity(severity != null ? severity : event.getSeverity());
                if (message != null) {
                    builder.message(message.replace("{super}", event.getMessage()));
                }
                filtered.add(builder.build());
            }
        }

        return filtered;
    }

    private boolean filterEvent(ValidationEvent event, Set<ShapeId> candidates) {
        ShapeId target = event.getShapeId().orElse(null);
        if (target != null) {
            if (!namespaces.isEmpty() && !namespaces.contains(target.getNamespace())) {
                return false;
            }
            if (candidates != null && !candidates.contains(target)) {
                return false;
            }
        }

        return true;
    }

    private String replaceId(String eventId, String validatorId) {
        int index = eventId.indexOf(".");
        if (index == -1) {
            return validatorId;
        } else {
            return validatorId + eventId.substring(index);
        }
    }
}
