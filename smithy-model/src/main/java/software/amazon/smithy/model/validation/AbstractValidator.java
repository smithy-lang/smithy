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

package software.amazon.smithy.model.validation;

import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.shapes.Shape;

/**
 * Abstract Validator class that has helpful methods for emitting events.
 */
public abstract class AbstractValidator implements Validator {
    private final String defaultName = ValidatorService.determineValidatorName(getClass());

    public String getName() {
        return defaultName;
    }

    protected final ValidationEvent error(
            Shape shape,
            String message
    ) {
        return createEvent(Severity.ERROR, shape, shape.getSourceLocation(), message);
    }

    protected final ValidationEvent error(
            Shape shape,
            String message,
            String eventIdSubpart1
    ) {
        return createEvent(Severity.ERROR, shape, shape.getSourceLocation(), message, eventIdSubpart1);
    }

    protected final ValidationEvent error(
            Shape shape,
            String message,
            String eventIdSubpart1,
            String eventIdSubpart2
    ) {
        return createEvent(Severity.ERROR, shape, shape.getSourceLocation(), message, eventIdSubpart1, eventIdSubpart2);
    }

    protected final ValidationEvent error(
            Shape shape,
            String message,
            String eventIdSubpart1,
            String eventIdSubpart2,
            String eventIdSubpart3
    ) {
        return createEvent(Severity.ERROR, shape, shape.getSourceLocation(), message, eventIdSubpart1, eventIdSubpart2,
                eventIdSubpart3);
    }

    protected final ValidationEvent error(
            Shape shape,
            FromSourceLocation location,
            String message
    ) {
        return createEvent(Severity.ERROR, shape, location, message);
    }

    protected final ValidationEvent error(
            Shape shape,
            FromSourceLocation location,
            String message,
            String eventIdSubpart1
    ) {
        return createEvent(Severity.ERROR, shape, location, message, eventIdSubpart1);
    }

    protected final ValidationEvent error(
            Shape shape,
            FromSourceLocation location,
            String message,
            String eventIdSubpart1,
            String eventIdSubpart2
    ) {
        return createEvent(Severity.ERROR, shape, location, message, eventIdSubpart1, eventIdSubpart2);
    }

    protected final ValidationEvent error(
            Shape shape,
            FromSourceLocation location,
            String message,
            String eventIdSubpart1,
            String eventIdSubpart2,
            String eventIdSubpart3
    ) {
        return createEvent(Severity.ERROR, shape, location, message, eventIdSubpart1, eventIdSubpart2, eventIdSubpart3);
    }

    protected final ValidationEvent danger(
            Shape shape,
            String message
    ) {
        return createEvent(Severity.DANGER, shape, shape.getSourceLocation(), message);
    }

    protected final ValidationEvent danger(
            Shape shape,
            String message,
            String eventIdSubpart1
    ) {
        return createEvent(Severity.DANGER, shape, shape.getSourceLocation(), message, eventIdSubpart1);
    }

    protected final ValidationEvent danger(
            Shape shape,
            String message,
            String eventIdSubpart1,
            String eventIdSubpart2
    ) {
        return createEvent(Severity.DANGER, shape, shape.getSourceLocation(), message, eventIdSubpart1,
                            eventIdSubpart2);
    }

    protected final ValidationEvent danger(
            Shape shape,
            String message,
            String eventIdSubpart1,
            String eventIdSubpart2,
            String eventIdSubpart3
    ) {
        return createEvent(Severity.DANGER, shape, shape.getSourceLocation(), message, eventIdSubpart1, eventIdSubpart2,
                            eventIdSubpart3);
    }

    protected final ValidationEvent danger(
            Shape shape,
            FromSourceLocation location,
            String message
    ) {
        return createEvent(Severity.DANGER, shape, location, message);
    }

    protected final ValidationEvent danger(
            Shape shape,
            FromSourceLocation location,
            String message,
            String eventIdSubpart1
    ) {
        return createEvent(Severity.DANGER, shape, location, message, eventIdSubpart1);
    }

    protected final ValidationEvent danger(
            Shape shape,
            FromSourceLocation location,
            String message,
            String eventIdSubpart1,
            String eventIdSubpart2
    ) {
        return createEvent(Severity.DANGER, shape, location, message, eventIdSubpart1, eventIdSubpart2);
    }

    protected final ValidationEvent danger(
            Shape shape,
            FromSourceLocation location,
            String message,
            String eventIdSubpart1,
            String eventIdSubpart2,
            String eventIdSubpart3
    ) {
        return createEvent(Severity.DANGER, shape, location, message, eventIdSubpart1, eventIdSubpart2,
                            eventIdSubpart3);
    }

    protected final ValidationEvent warning(
            Shape shape,
            String message
    ) {
        return createEvent(Severity.WARNING, shape, shape.getSourceLocation(), message);
    }

    protected final ValidationEvent warning(
            Shape shape,
            String message,
            String eventIdSubpart1
    ) {
        return createEvent(Severity.WARNING, shape, shape.getSourceLocation(), message, eventIdSubpart1);
    }

    protected final ValidationEvent warning(
            Shape shape,
            String message,
            String eventIdSubpart1,
            String eventIdSubpart2
    ) {
        return createEvent(Severity.WARNING, shape, shape.getSourceLocation(), message, eventIdSubpart1,
                eventIdSubpart2);
    }

    protected final ValidationEvent warning(
            Shape shape,
            String message,
            String eventIdSubpart1,
            String eventIdSubpart2,
            String eventIdSubpart3
    ) {
        return createEvent(Severity.WARNING, shape, shape.getSourceLocation(), message, eventIdSubpart1,
                eventIdSubpart2, eventIdSubpart3);
    }

    protected final ValidationEvent warning(
            Shape shape,
            FromSourceLocation location,
            String message
    ) {
        return createEvent(Severity.WARNING, shape, location, message);
    }

    protected final ValidationEvent warning(
            Shape shape,
            FromSourceLocation location,
            String message,
            String eventIdSubpart1
    ) {
        return createEvent(Severity.WARNING, shape, location, message, eventIdSubpart1);
    }

    protected final ValidationEvent warning(
            Shape shape,
            FromSourceLocation location,
            String message,
            String eventIdSubpart1,
            String eventIdSubpart2
    ) {
        return createEvent(Severity.WARNING, shape, location, message, eventIdSubpart1, eventIdSubpart2);
    }

    protected final ValidationEvent warning(
            Shape shape,
            FromSourceLocation location,
            String message,
            String eventIdSubpart1,
            String eventIdSubpart2,
            String eventIdSubpart3
    ) {
        return createEvent(Severity.WARNING, shape, location, message, eventIdSubpart1, eventIdSubpart2,
                            eventIdSubpart3);
    }

    protected final ValidationEvent note(
            Shape shape,
            String message
    ) {
        return createEvent(Severity.NOTE, shape, shape.getSourceLocation(), message);
    }

    protected final ValidationEvent note(
            Shape shape,
            String message,
            String eventIdSubpart1
    ) {
        return createEvent(Severity.NOTE, shape, shape.getSourceLocation(), message, eventIdSubpart1);
    }

    protected final ValidationEvent note(
            Shape shape,
            String message,
            String eventIdSubpart1,
            String eventIdSubpart2
    ) {
        return createEvent(Severity.NOTE, shape, shape.getSourceLocation(), message, eventIdSubpart1, eventIdSubpart2);
    }

    protected final ValidationEvent note(
            Shape shape,
            String message,
            String eventIdSubpart1,
            String eventIdSubpart2,
            String eventIdSubpart3
    ) {
        return createEvent(Severity.NOTE, shape, shape.getSourceLocation(), message, eventIdSubpart1, eventIdSubpart2,
                eventIdSubpart3);
    }

    protected final ValidationEvent note(
            Shape shape,
            FromSourceLocation location,
            String message
    ) {
        return createEvent(Severity.NOTE, shape, location, message);
    }

    protected final ValidationEvent note(
            Shape shape,
            FromSourceLocation location,
            String message,
            String eventIdSubpart1
    ) {
        return createEvent(Severity.NOTE, shape, location, message, eventIdSubpart1);
    }

    protected final ValidationEvent note(
            Shape shape,
            FromSourceLocation location,
            String message,
            String eventIdSubpart1,
            String eventIdSubpart2
    ) {
        return createEvent(Severity.NOTE, shape, location, message, eventIdSubpart1, eventIdSubpart2);
    }

    protected final ValidationEvent note(
            Shape shape,
            FromSourceLocation location,
            String message,
            String eventIdSubpart1,
            String eventIdSubpart2,
            String eventIdSubpart3
    ) {
        return createEvent(Severity.NOTE, shape, location, message, eventIdSubpart1, eventIdSubpart2, eventIdSubpart3);
    }

    protected final ValidationEvent createEvent(
            Severity severity,
            Shape shape,
            FromSourceLocation loc,
            String message
    ) {
        return ValidationEvent.builder()
                .severity(severity)
                .message(message)
                .shapeId(shape.getId())
                .sourceLocation(loc.getSourceLocation())
                .id(getName())
                .build();
    }

    protected final ValidationEvent createEvent(
            Severity severity,
            Shape shape,
            FromSourceLocation loc,
            String message,
            String eventIdSubpart1
    ) {
        return ValidationEvent.builder()
                .severity(severity)
                .message(message)
                .shapeId(shape.getId())
                .sourceLocation(loc.getSourceLocation())
                .id(getName() + "." + eventIdSubpart1)
                .build();
    }

    protected final ValidationEvent createEvent(
            Severity severity,
            Shape shape,
            FromSourceLocation loc,
            String message,
            String eventIdSubpart1,
            String eventIdSubpart2
    ) {
        return ValidationEvent.builder()
                .severity(severity)
                .message(message)
                .shapeId(shape.getId())
                .sourceLocation(loc.getSourceLocation())
                .id(getName() + "." + eventIdSubpart1 + "." + eventIdSubpart2)
                .build();
    }

    protected final ValidationEvent createEvent(
            Severity severity,
            Shape shape,
            FromSourceLocation loc,
            String message,
            String eventIdSubpart1,
            String eventIdSubpart2,
            String eventIdSubpart3
    ) {
        return ValidationEvent.builder()
                .severity(severity)
                .message(message)
                .shapeId(shape.getId())
                .sourceLocation(loc.getSourceLocation())
                .id(getName() + "." + eventIdSubpart1 + "." + eventIdSubpart2 + "." + eventIdSubpart3)
                .build();
    }
}
