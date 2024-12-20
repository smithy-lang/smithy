/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jsonschema;

public class SmithyJsonSchemaException extends RuntimeException {
    public SmithyJsonSchemaException(String message) {
        super(message);
    }

    public SmithyJsonSchemaException(String message, Throwable cause) {
        super(message, cause);
    }
}
