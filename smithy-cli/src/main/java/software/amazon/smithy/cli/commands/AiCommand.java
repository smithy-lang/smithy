/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.cli.commands;

import java.util.Arrays;
import java.util.List;
import software.amazon.smithy.cli.Arguments;
import software.amazon.smithy.cli.CliError;
import software.amazon.smithy.cli.CliPrinter;
import software.amazon.smithy.cli.ColorFormatter;
import software.amazon.smithy.cli.ColorTheme;
import software.amazon.smithy.cli.Command;
import software.amazon.smithy.cli.StandardOptions;

/**
 * Parent command for AI-agent affordances, dispatching to subcommands like {@code smithy ai install}.
 */
final class AiCommand implements Command {

    private final String parentCommandName;
    private final List<Command> commands;

    AiCommand(String parentCommandName) {
        this.parentCommandName = parentCommandName;
        this.commands = Arrays.asList(
                new AiInstallCommand(getName()),
                new AiListCommand(getName()));
    }

    @Override
    public String getName() {
        return "ai";
    }

    @Override
    public String getSummary() {
        return "Install Smithy knowledge skills into an AI agent harness.";
    }

    private void printHelp(ColorFormatter colors, CliPrinter printer) {
        printer.println(String.format("Usage: %s %s <subcommand> [<args>]",
                colors.style(parentCommandName, ColorTheme.EM_UNDERLINE),
                colors.style(getName(), ColorTheme.EM_UNDERLINE)));
        printer.println("");
        printer.println("Available subcommands:");

        int longestName = 0;
        for (Command command : commands) {
            if (!command.isHidden()) {
                longestName = Math.max(longestName, command.getName().length() + 12);
            }
        }

        for (Command command : commands) {
            if (!command.isHidden()) {
                printer.println(String.format("    %-" + longestName + "s %s",
                        colors.style(command.getName(), ColorTheme.LITERAL),
                        command.getSummary()));
            }
        }

        printer.println("");
    }

    @Override
    public int execute(Arguments arguments, Env env) {
        String command = arguments.shift();

        if (command == null) {
            arguments.getPositional();
            if (arguments.getReceiver(StandardOptions.class).help()) {
                printHelp(env.colors(), env.stdout());
                return 0;
            } else {
                printHelp(env.colors(), env.stderr());
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
