/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core;

import software.amazon.smithy.build.SmithyBuildException;

/**
 * Thrown when an error occurs during code generation.
 */
public class CodegenException extends SmithyBuildException {
    public CodegenException(String message) {
        super(message);
    }

    public CodegenException(Throwable cause) {
        super(cause);
    }

    public CodegenException(String message, Throwable cause) {
        super(message, cause);
    }
}
