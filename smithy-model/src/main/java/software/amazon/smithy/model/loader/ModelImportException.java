/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader;

/**
 * Exception thrown when a model fails to be imported.
 */
public class ModelImportException extends RuntimeException {
    public ModelImportException(String message) {
        super(message);
    }

    public ModelImportException(String message, Throwable cause) {
        super(message, cause);
    }
}
