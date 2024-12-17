/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.cli.commands;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import software.amazon.smithy.cli.Arguments;
import software.amazon.smithy.cli.CliError;
import software.amazon.smithy.cli.CliPrinter;
import software.amazon.smithy.cli.ColorFormatter;
import software.amazon.smithy.cli.ColorTheme;
import software.amazon.smithy.cli.Command;
import software.amazon.smithy.cli.EnvironmentVariable;
import software.amazon.smithy.cli.SmithyCli;
import software.amazon.smithy.cli.StandardOptions;
import software.amazon.smithy.cli.dependencies.DependencyResolver;

public final class SmithyCommand implements Command {

    private final List<Command> commands;

    public SmithyCommand(DependencyResolver.Factory dependencyResolverFactory) {
        Objects.requireNonNull(dependencyResolverFactory);

        Command migrateCommand = new MigrateCommand(getName());
        Command deprecated1To2Command = MigrateCommand.createDeprecatedAlias(migrateCommand);

        commands = Arrays.asList(
                new VersionCommand(),
                new ValidateCommand(getName(), dependencyResolverFactory),
                new BuildCommand(getName(), dependencyResolverFactory),
                new DiffCommand(getName(), dependencyResolverFactory),
                new AstCommand(getName(), dependencyResolverFactory),
                new SelectCommand(getName(), dependencyResolverFactory),
                new FormatCommand(getName()),
                new CleanCommand(getName()),
                migrateCommand,
                deprecated1To2Command,
                new WarmupCommand(getName()),
                new InitCommand(getName()),
                new LockCommand(getName(), dependencyResolverFactory));
    }

    @Override
    public String getName() {
        return "smithy";
    }

    @Override
    public String getSummary() {
        return "";
    }

    private void printHelp(ColorFormatter colors, CliPrinter printer) {
        printer.println(String.format("Usage: %s [-h | --help] [--version] <command> [<args>]",
                colors.style("smithy", ColorTheme.EM_UNDERLINE)));
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
                        colors.style(command.getName(), ColorTheme.LITERAL),
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
