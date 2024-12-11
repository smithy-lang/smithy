/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader;

public class ModelManifestException extends RuntimeException {

    public ModelManifestException(String message, Throwable parent) {
        super(message, parent);
    }

    public ModelManifestException(String message) {
        super(message);
    }
}
