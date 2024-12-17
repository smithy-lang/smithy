/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.language.error;

import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Exception thrown when a rule-set is invalid.
 */
@SmithyUnstableApi
public final class InvalidRulesException extends RuntimeException implements FromSourceLocation {
    private final transient SourceLocation sourceLocation;

    /**
     * Constructs a new invalid rule exception with the given message and source location.
     *
     * @param message the detail message.
     * @param location the location of the invalid rule.
     */
    public InvalidRulesException(String message, FromSourceLocation location) {
        super(createMessage(message, location.getSourceLocation()));
        sourceLocation = location.getSourceLocation();
    }

    @Override
    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }

    private static String createMessage(String message, SourceLocation sourceLocation) {
        if (sourceLocation == SourceLocation.NONE) {
            return message;
        } else {
            String prettyLocation = stackTraceForm(sourceLocation);
            return message.contains(prettyLocation) ? message : message + " (" + prettyLocation + ")";
        }
    }

    private static String stackTraceForm(SourceLocation sourceLocation) {
        if (sourceLocation == SourceLocation.NONE) {
            return "N/A";
        }

        StringBuilder sb = new StringBuilder();
        if (sourceLocation.getFilename() != null) {
            sb.append(sourceLocation.getFilename());
        }
        if (sourceLocation.getLine() != 0) {
            sb.append(":").append(sourceLocation.getLine());
        }
        // column is ignored in stack trace form
        return sb.toString();
    }
}
