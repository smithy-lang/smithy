/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.pattern;

/**
 * Exception thrown for invalid patterns.
 */
public class InvalidPatternException extends IllegalArgumentException {

    public InvalidPatternException(String message) {
        super(message);
    }
}
