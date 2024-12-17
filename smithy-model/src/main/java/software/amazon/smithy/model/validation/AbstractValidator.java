/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
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
        return createEvent(Severity.ERROR,
                shape,
                shape.getSourceLocation(),
                message,
                eventIdSubpart1,
                eventIdSubpart2,
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
        return createEvent(Severity.DANGER,
                shape,
                shape.getSourceLocation(),
                message,
                eventIdSubpart1,
                eventIdSubpart2);
    }

    protected final ValidationEvent danger(
            Shape shape,
            String message,
            String eventIdSubpart1,
            String eventIdSubpart2,
            String eventIdSubpart3
    ) {
        return createEvent(Severity.DANGER,
                shape,
                shape.getSourceLocation(),
                message,
                eventIdSubpart1,
                eventIdSubpart2,
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
        return createEvent(Severity.DANGER,
                shape,
                location,
                message,
                eventIdSubpart1,
                eventIdSubpart2,
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
        return createEvent(Severity.WARNING,
                shape,
                shape.getSourceLocation(),
                message,
                eventIdSubpart1,
                eventIdSubpart2);
    }

    protected final ValidationEvent warning(
            Shape shape,
            String message,
            String eventIdSubpart1,
            String eventIdSubpart2,
            String eventIdSubpart3
    ) {
        return createEvent(Severity.WARNING,
                shape,
                shape.getSourceLocation(),
                message,
                eventIdSubpart1,
                eventIdSubpart2,
                eventIdSubpart3);
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
        return createEvent(Severity.WARNING,
                shape,
                location,
                message,
                eventIdSubpart1,
                eventIdSubpart2,
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
        return createEvent(Severity.NOTE,
                shape,
                shape.getSourceLocation(),
                message,
                eventIdSubpart1,
                eventIdSubpart2,
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
