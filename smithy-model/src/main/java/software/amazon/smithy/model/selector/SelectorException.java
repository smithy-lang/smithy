/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.selector;

/**
 * Exception thrown when a selector evaluation is invalid.
 */
public class SelectorException extends RuntimeException {
    public SelectorException(String message) {
        super(message);
    }

    public SelectorException(String message, Throwable previous) {
        super(message, previous);
    }
}
