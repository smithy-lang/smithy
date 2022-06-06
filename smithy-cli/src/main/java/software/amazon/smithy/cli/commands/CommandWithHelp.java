/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.cli.commands;

import java.util.List;
import java.util.logging.Logger;
import software.amazon.smithy.cli.Arguments;
import software.amazon.smithy.cli.Command;
import software.amazon.smithy.cli.StandardOptions;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * A command that has -h and --help options.
 *
 * <p>When -h or --help is found, the help for the command is printed
 * to stdout and exits with code 0.
 */
@SmithyInternalApi
abstract class CommandWithHelp implements Command {

    private static final Logger LOGGER = Logger.getLogger(CommandWithHelp.class.getName());

    @Override
    public final int execute(Arguments arguments, Env env) {
        List<String> positionalArguments = parseArguments(arguments, env);

        if (arguments.getReceiver(StandardOptions.class).help()) {
            printHelp(env.stdout());
            return 0;
        }

        LOGGER.fine(() -> "Invoking Command with positional arguments: " + positionalArguments);
        return run(arguments, env, positionalArguments);
    }

    /**
     * Registers all required receivers, and returns the result of calling
     * {@link Arguments#finishParsing()} on the parser.
     *
     * @param arguments Arguments to parse.
     * @param env Environment being evaluated.
     * @return Returns the parsed positional arguments.
     */
    protected abstract List<String> parseArguments(Arguments arguments, Env env);

    /**
     * Run the non-help command after all arguments have been parsed.
     *
     * @param arguments Arguments to evaluate.
     * @param env CLI environment settings.
     * @param positional Parsed positional arguments.
     * @return Returns the exit code.
     */
    protected abstract int run(Arguments arguments, Env env, List<String> positional);
}
