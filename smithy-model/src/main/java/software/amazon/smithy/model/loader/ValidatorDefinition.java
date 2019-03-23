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
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
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
    private final String id;

    ValidatorDefinition(String name, String id) {
        this.name = name;
        this.id = id;
    }

    ValidationEvent map(ValidationEvent event) {
        if (!filterEvent(event) || (id == null && severity == null && message == null)) {
            return event;
        } else {
            ValidationEvent.Builder builder = event.toBuilder();
            builder.eventId(id != null ? id : event.getEventId());
            builder.severity(severity != null ? severity : event.getSeverity());
            if (message != null) {
                builder.message(message.replace("{super}", event.getMessage()));
            }
            return builder.build();
        }
    }

    private boolean filterEvent(ValidationEvent event) {
        return namespaces.isEmpty()
               || event.getShapeId().map(ShapeId::getNamespace).filter(namespaces::contains).isPresent();
    }
}
