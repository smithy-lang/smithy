/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.build;

/**
 * Base exception thrown for any error that occurs while building.
 */
public class SmithyBuildException extends RuntimeException {
    public SmithyBuildException(String message) {
        super(message);
    }

    public SmithyBuildException(Throwable cause) {
        super(cause);
    }

    public SmithyBuildException(String message, Throwable previous) {
        super(message, previous);
    }
}
