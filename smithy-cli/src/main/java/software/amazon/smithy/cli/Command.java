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

import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Represents a CLI command.
 */
@SmithyUnstableApi
public interface Command {
    /**
     * Gets the name of the command.
     *
     * <p>The returned name should contain no spaces or special characters.
     *
     * @return Returns the command name.
     */
    String getName();

    /**
     * Gets a short summary of the command that's shown in the main help.
     *
     * @return Returns the short help description.
     */
    String getSummary();

    /**
     * Gets details help for the command.
     *
     * <p>Returning an empty string omits detailed help.
     *
     * @return Returns detailed help information.
     */
    default String getHelp() {
        return "";
    }

    /**
     * Gets the parser of the command.
     *
     * @return Returns the argument parser.
     */
    Parser getParser();

    /**
     * Executes the command using the provided arguments.
     *
     * @param arguments CLI arguments.
     * @param classLoader ClassLoader to use in the command.
     */
    void execute(Arguments arguments, ClassLoader classLoader);
}
