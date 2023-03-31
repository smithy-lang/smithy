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
import java.util.Objects;
import software.amazon.smithy.cli.Arguments;
import software.amazon.smithy.cli.CliError;
import software.amazon.smithy.cli.CliPrinter;
import software.amazon.smithy.cli.ColorFormatter;
import software.amazon.smithy.cli.Command;
import software.amazon.smithy.cli.EnvironmentVariable;
import software.amazon.smithy.cli.SmithyCli;
import software.amazon.smithy.cli.StandardOptions;
import software.amazon.smithy.cli.Style;
import software.amazon.smithy.cli.dependencies.DependencyResolver;

public final class SmithyCommand implements Command {

    private final List<Command> commands;

    public SmithyCommand(DependencyResolver.Factory dependencyResolverFactory) {
        Objects.requireNonNull(dependencyResolverFactory);
        commands = Arrays.asList(
            new ValidateCommand(getName(), dependencyResolverFactory),
            new BuildCommand(getName(), dependencyResolverFactory),
            new DiffCommand(getName(), dependencyResolverFactory),
            new AstCommand(getName(), dependencyResolverFactory),
            new SelectCommand(getName(), dependencyResolverFactory),
            new CleanCommand(getName()),
            new Upgrade1to2Command(getName()),
            new WarmupCommand(getName())
        );
    }

    @Override
    public String getName() {
        return "smithy";
    }

    @Override
    public String getSummary() {
        return "";
    }

    @Override
    public void printHelp(Arguments arguments, ColorFormatter colors, CliPrinter printer) {
        printer.println(String.format("Usage: %s [-h | --help] [--version] <command> [<args>]",
                                      colors.style("smithy", Style.BRIGHT_WHITE, Style.UNDERLINE)));
        printer.println("");
        printer.println("Available commands:");

        int longestName = 0;
        for (Command command : commands) {
            if (!command.isHidden()) {
                if (command.getName().length() + 12 > longestName) {
                    longestName = command.getName().length() + 12;
                }
            }
        }

        for (Command command : commands) {
            if (!command.isHidden()) {
                printer.println(String.format("    %-" + longestName + "s %s",
                                              colors.style(command.getName(), Style.YELLOW),
                                              command.getSummary()));
            }
        }

        printer.println("");
    }

    @Override
    public int execute(Arguments arguments, Env env) {
        // Set the current CLI version as a system property, so it can be used in config files.
        EnvironmentVariable.SMITHY_VERSION.set(SmithyCli.getVersion());

        String command = arguments.shift();

        // If no command was given, then finish parsing to check if -h, --help, or --version was given.
        if (command == null) {
            arguments.getPositional();

            StandardOptions standardOptions = arguments.getReceiver(StandardOptions.class);

            if (standardOptions.help()) {
                printHelp(arguments, env.colors(), env.stdout());
                return 0;
            } else if (standardOptions.version()) {
                env.stdout().println(SmithyCli.getVersion());
                return 0;
            } else {
                printHelp(arguments, env.colors(), env.stderr());
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
