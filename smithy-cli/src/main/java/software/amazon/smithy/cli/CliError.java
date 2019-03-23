/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
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
}
