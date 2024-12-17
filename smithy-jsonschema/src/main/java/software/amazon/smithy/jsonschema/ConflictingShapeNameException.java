/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jsonschema;

/**
 * Thrown when two shapes generate the same JSON schema pointer.
 */
public class ConflictingShapeNameException extends SmithyJsonSchemaException {
    ConflictingShapeNameException(String message) {
        super(message);
    }
}
