/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation;

import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Writes {@code ValidationEvent} objects as a single line string.
 */
public final class LineValidationEventFormatter implements ValidationEventFormatter {
    @Override
    public String format(ValidationEvent event) {
        String message = event.getMessage();

        String reason = event.getSuppressionReason().orElse(null);
        if (reason != null) {
            message += " (" + reason + ")";
        }
        String hint = event.getHint().orElse(null);
        if (hint != null) {
            message += " [" + hint + "]";
        }

        return String.format(
                "[%s] %s: %s | %s %s:%s:%s",
                event.getSeverity(),
                event.getShapeId().map(ShapeId::toString).orElse("-"),
                message,
                event.getId(),
                event.getSourceLocation().getFilename(),
                event.getSourceLocation().getLine(),
                event.getSourceLocation().getColumn());
    }
}
