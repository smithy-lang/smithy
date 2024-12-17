/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.build;

/**
 * Thrown when a projection extends from an unknown projection.
 */
public class UnknownProjectionException extends SmithyBuildException {
    public UnknownProjectionException(String message) {
        super(message);
    }

    public UnknownProjectionException(String message, Throwable previous) {
        super(message, previous);
    }
}
