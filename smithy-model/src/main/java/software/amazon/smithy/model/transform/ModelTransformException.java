/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.transform;

/**
 * Exception thrown when a model transformation error occurs.
 */
public final class ModelTransformException extends RuntimeException {
    public ModelTransformException(String message) {
        super(message);
    }

    public ModelTransformException(String message, Throwable previous) {
        super(message, previous);
    }
}
