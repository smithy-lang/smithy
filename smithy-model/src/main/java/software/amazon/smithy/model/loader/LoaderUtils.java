/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.util.Collection;
import java.util.List;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.ExpectationNotMetException;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.Validator;

final class LoaderUtils {

    private LoaderUtils() {}

    /**
     * Checks if additional properties are in an object, and if so, emits a warning event.
     *
     * @param node Node to check.
     * @param shape Shape to associate with the error.
     * @param properties Properties to allow.
     */
    static void checkForAdditionalProperties(
            ObjectNode node,
            ShapeId shape, Collection<String> properties,
            List<ValidationEvent> events
    ) {
        try {
            node.expectNoAdditionalProperties(properties);
        } catch (ExpectationNotMetException e) {
            ValidationEvent event = ValidationEvent.fromSourceException(e)
                    .toBuilder()
                    .shapeId(shape)
                    .severity(Severity.WARNING)
                    .build();
            events.add(event);
        }
    }

    /**
     * Checks if the given version string is supported.
     *
     * @param versionString Version string to check (e.g., 1, 1.0).
     * @return Returns true if this is a supported model version.
     */
    static boolean isVersionSupported(String versionString) {
        return versionString.equals("1")
               || versionString.equals("1.0")
               || versionString.equals("1.1");
    }

    /**
     * Create a {@link ValidationEvent} for a shape conflict.
     *
     * @param id Shape ID in conflict.
     * @param a The first location of this shape.
     * @param b The second location of this shape.
     * @return Returns the created validation event.
     */
    static ValidationEvent onShapeConflict(ShapeId id, SourceLocation a, SourceLocation b) {
        return ValidationEvent.builder()
                .id(Validator.MODEL_ERROR)
                .severity(Severity.ERROR)
                .sourceLocation(b)
                .shapeId(id)
                .message(String.format("Conflicting shape definition for `%s` found at `%s` and `%s`", id, a, b))
                .build();
    }

    /**
     * Checks if the given values are defined at the same source location,
     * and the source location is not {@link SourceLocation#NONE}.
     *
     * @param a First value to check.
     * @param b Second value to check.
     * @return Returns true if they are the same.
     */
    static boolean isSameLocation(FromSourceLocation a, FromSourceLocation b) {
        SourceLocation sa = a.getSourceLocation();
        SourceLocation sb = b.getSourceLocation();
        return sa != SourceLocation.NONE && sa.equals(sb);
    }

    /**
     * Checks if a list of validation events contains an ERROR severity.
     *
     * @param events Events to check.
     * @return Returns true if an ERROR event is present.
     */
    static boolean containsErrorEvents(List<ValidationEvent> events) {
        for (ValidationEvent event : events) {
            if (event.getSeverity() == Severity.ERROR) {
                return true;
            }
        }
        return false;
    }
}
