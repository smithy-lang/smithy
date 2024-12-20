/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath;

/**
 * Thrown when any JMESPath error occurs.
 */
public class JmespathException extends RuntimeException {
    public JmespathException(String message) {
        super(message);
    }

    public JmespathException(String message, Throwable previous) {
        super(message, previous);
    }
}
