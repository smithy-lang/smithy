/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.diff.evaluators;

import software.amazon.smithy.diff.DiffEvaluator;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Provides a default diff evaluator implementation with utility methods
 * for emitting events of different severities.
 */
public abstract class AbstractDiffEvaluator implements DiffEvaluator {
    /**
     * Gets the event ID of the evaluator.
     *
     * <p>This can be overridden in subclasses to use a different ID.
     *
     * @return Returns the computed event ID.
     */
    protected String getEventId() {
        return getClass().getSimpleName();
    }

    protected final ValidationEvent error(Shape shape, String message) {
        return createEvent(Severity.ERROR, shape, shape.getSourceLocation(), message);
    }

    protected final ValidationEvent error(Shape shape, FromSourceLocation location, String message) {
        return createEvent(Severity.ERROR, shape, location, message);
    }

    protected final ValidationEvent danger(Shape shape, String message) {
        return createEvent(Severity.DANGER, shape, shape.getSourceLocation(), message);
    }

    protected final ValidationEvent danger(Shape shape, FromSourceLocation location, String message) {
        return createEvent(Severity.DANGER, shape, location, message);
    }

    protected final ValidationEvent warning(Shape shape, String message) {
        return createEvent(Severity.WARNING, shape, shape.getSourceLocation(), message);
    }

    protected final ValidationEvent warning(Shape shape, FromSourceLocation location, String message) {
        return createEvent(Severity.WARNING, shape, location, message);
    }

    protected final ValidationEvent note(Shape shape, String message) {
        return createEvent(Severity.NOTE, shape, shape.getSourceLocation(), message);
    }

    protected final ValidationEvent note(Shape shape, FromSourceLocation location, String message) {
        return createEvent(Severity.NOTE, shape, location, message);
    }

    private ValidationEvent createEvent(Severity severity, Shape shape, FromSourceLocation location, String message) {
        return createEvent(ValidationEvent.builder()
                .severity(severity)
                .message(message)
                .shapeId(shape.getId())
                .sourceLocation(location.getSourceLocation()));
    }

    private ValidationEvent createEvent(ValidationEvent.Builder builder) {
        return builder.id(getEventId()).build();
    }
}
