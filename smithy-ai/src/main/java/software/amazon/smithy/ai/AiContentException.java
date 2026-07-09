/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.ai;

/**
 * Thrown when the Smithy AI content index is missing, malformed, or a listed resource cannot be
 * loaded. Distinct from {@code IOException} so callers can surface an actionable message rather
 * than a wrapped IO error.
 */
public class AiContentException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public AiContentException(String message) {
        super(message);
    }

    public AiContentException(String message, Throwable cause) {
        super(message, cause);
    }
}
