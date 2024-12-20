/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.cli;

/**
 * Throw this exception to cause the CLI to exit with a message and code.
 */
public final class CliError extends RuntimeException {
    public final int code;

    /**
     * Exits the CLI with a message and an error code of 1.
     *
     * @param message Message to output.
     */
    public CliError(String message) {
        this(message, 1);
    }

    /**
     * @param message Message to use in the exception.
     * @param code Exit code to set.
     */
    public CliError(String message, int code) {
        super(message);
        this.code = code;
    }

    /**
     * @param message Message to use in the exception.
     * @param code Exit code to set.
     * @param previous Previous exception.
     */
    public CliError(String message, int code, Throwable previous) {
        super(message, previous);
        this.code = code;
    }

    /**
     * Wraps the given exception in a CliError (no wrapping is performed
     * if the given exception is an instance of CliError).
     *
     * @param e Exception to wrap.
     * @return Returns the wrapped exception.
     */
    public static CliError wrap(Throwable e) {
        if (e instanceof CliError) {
            return (CliError) e;
        } else {
            return new CliError(e.getMessage(), 1, e);
        }
    }
}
