/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath;

/**
 * Thrown when any JMESPath error occurs.
 */
public class JmespathException extends RuntimeException {

    private final JmespathExceptionType errorType;

    public JmespathException(String message) {
        this(JmespathExceptionType.OTHER, message);
    }

    public JmespathException(JmespathExceptionType errorType, String message) {
        super(message);
        this.errorType = errorType;
    }

    public JmespathException(String message, Throwable previous) {
        this(JmespathExceptionType.OTHER, message, previous);
    }

    public JmespathException(JmespathExceptionType errorType, String message, Throwable previous) {
        super(message, previous);
        this.errorType = errorType;
    }

    public JmespathExceptionType getType() {
        return errorType;
    }
}
