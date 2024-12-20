/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.build;

/**
 * Thrown when a mapper cannot be found.
 */
public class UnknownTransformException extends SmithyBuildException {
    public UnknownTransformException(String message) {
        super(message);
    }

    public UnknownTransformException(String message, Throwable previous) {
        super(message, previous);
    }
}
