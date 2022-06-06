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

import java.util.Arrays;
import java.util.List;
import software.amazon.smithy.cli.Arguments;
import software.amazon.smithy.cli.CliError;
import software.amazon.smithy.cli.CliPrinter;
import software.amazon.smithy.cli.Command;
import software.amazon.smithy.cli.StandardOptions;
import software.amazon.smithy.cli.Style;
import software.amazon.smithy.utils.SmithyInternalApi;

@SmithyInternalApi
public class SmithyCommand implements Command {

    private final List<Command> commands = Arrays.asList(
            new ValidateCommand(),
            new BuildCommand(),
            new AstCommand(),
            new SelectCommand(),
            new DiffCommand());

    @Override
    public String getName() {
        return "smithy";
    }

    @Override
    public String getSummary() {
        return "";
    }

    @Override
    public void printHelp(CliPrinter printer) {
        printer.println(String.format("Usage: %s [-h|--help] <command> [<args>]",
                                      printer.style("smithy", Style.BRIGHT_WHITE)));
        printer.println("");
        printer.println("Available commands:");

        int longestName = 0;
        for (Command command : commands) {
            if (command.getName().length() + 12 > longestName) {
                longestName = command.getName().length() + 12;
            }
        }

        for (Command command : commands) {
            printer.println(String.format("    %-" + longestName + "s %s",
                                          printer.style(command.getName(), Style.YELLOW),
                                          command.getSummary()));
        }
    }

    @Override
    public int execute(Arguments arguments, Env env) {
        String command = arguments.shift();

        // If no command was given, then finish parsing to check if -h or --help was given.
        if (command == null) {
            arguments.finishParsing();
            if (arguments.getReceiver(StandardOptions.class).help()) {
                printHelp(env.stdout());
                return 0;
            } else {
                printHelp(env.stderr());
                return 1;
            }
        }

        for (Command c : commands) {
            if (c.getName().equals(command)) {
                return c.execute(arguments, env);
            }
        }

        throw new CliError("Unknown argument or command: " + command);
    }
}
