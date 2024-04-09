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
