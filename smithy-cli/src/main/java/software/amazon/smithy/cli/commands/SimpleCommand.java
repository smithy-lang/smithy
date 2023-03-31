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
import software.amazon.smithy.cli.CliError;
import software.amazon.smithy.cli.CliPrinter;
import software.amazon.smithy.cli.ColorFormatter;
import software.amazon.smithy.cli.Command;
import software.amazon.smithy.cli.HelpPrinter;
import software.amazon.smithy.cli.StandardOptions;
import software.amazon.smithy.utils.StringUtils;

/**
 * A command that has -h and --help options.
 *
 * <p>When -h or --help is found, the help for the command is printed
 * to stdout and exits with code 0.
 */
abstract class SimpleCommand implements Command {

    private static final Logger LOGGER = Logger.getLogger(SimpleCommand.class.getName());
    private final String parentCommandName;

    SimpleCommand(String parentCommandName) {
        this.parentCommandName = parentCommandName;
    }

    @Override
    public final int execute(Arguments arguments, Env env) {
        configureArgumentReceivers(arguments);
        List<String> positionalArguments = arguments.getPositional();

        StandardOptions options = arguments.getReceiver(StandardOptions.class);

        // Version is only supported on the root-level command, but the argument has
        // to be available to all commands to make that work.
        if (arguments.getReceiver(StandardOptions.class).version()) {
            throw new CliError("Unexpected CLI argument: --version");
        }

        if (options.help()) {
            printHelp(arguments, env.colors(), env.stdout());
            return 0;
        }

        LOGGER.fine(() -> "Invoking Command with positional arguments: " + positionalArguments);
        return run(arguments, env, positionalArguments);
    }

    @Override
    public void printHelp(Arguments arguments, ColorFormatter colors, CliPrinter printer) {
        String name = StringUtils.isEmpty(parentCommandName) ? getName() : parentCommandName + " " + getName();
        HelpPrinter.fromArguments(name, arguments)
                .summary(getSummary())
                .documentation(getDocumentation(colors))
                .print(colors, printer);
    }

    /**
     * Configure argument receivers for the command.
     *
     * @param arguments Arguments to add/remove argument receivers from.
     */
    protected abstract void configureArgumentReceivers(Arguments arguments);

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
