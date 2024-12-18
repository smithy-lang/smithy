/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.shapes;

/**
 * Represents a shape ID syntax error.
 */
public class ShapeIdSyntaxException extends IllegalArgumentException {

    public ShapeIdSyntaxException(String message) {
        super(message);
    }
}
