/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.pattern;

/**
 * Exception thrown for invalid HTTP URI patterns.
 */
public final class InvalidUriPatternException extends InvalidPatternException {

    InvalidUriPatternException(String message) {
        super(message);
    }
}
