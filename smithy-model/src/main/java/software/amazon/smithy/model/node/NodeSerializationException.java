/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.node;

/**
 * Exception encountered during serialization.
 */
public class NodeSerializationException extends RuntimeException {
    public NodeSerializationException(String message) {
        super(message);
    }

    public NodeSerializationException(String message, Throwable previous) {
        super(message, previous);
    }
}
