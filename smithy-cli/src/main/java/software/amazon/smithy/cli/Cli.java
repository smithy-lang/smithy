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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * This class provides a very basic CLI abstraction.
 *
 * <p>Note: The argument parser does not support setting an argument using
 * foo=bar.
 *
 * <p>The following options are automatically added to each command:
 *
 * <ul>
 *     <li>--help | -h: Prints subcommand help text.</li>
 *     <li>--debug: Prints debug information, including exception stack traces.</li>
 *     <li>--no-color: Explicitly disables ANSI colors.</li>
 * </ul>
 *
 * <p>Why are we not using a library for this? Because parsing command line
 * options isn't difficult, we don't need to take a dependency, this code
 * uses no reflection to improve startup time. We can control exactly what
 * CLI features are supported in case we want to migrate to a library or
 * event a different language.
 */
public final class Cli {
    private static final String LOG_FORMAT = "[%1$tF %1$tT] [%2$s] %3$s%n";
    private final String applicationName;
    private Map<String, Command> commands = new TreeMap<>();

    /**
     * Creates a new CLI with the given name.
     *
     * @param applicationName Name of the CLI application.
     */
    public Cli(String applicationName) {
        this.applicationName = applicationName;
    }

    /**
     * Adds a subcommand to the CLI.
     *
     * @param command Command to add.
     */
    public void addCommand(Command command) {
        commands.put(command.getName(), command);
    }

    /**
     * Execute the command line using the given arguments.
     *
     * @param args Arguments to parse.
     * @return Returns the exit code.
     */
    public int run(String[] args) {
        try {
            // No args is not a valid use of the command, so
            // print help and exit with an error code.
            if (args.length == 0) {
                printMainHelp();
                return 0;
            }

            String argument = args[0];
            if (argument.equals("-h") || argument.equals("--help")) {
                printMainHelp();
            } else if (commands.containsKey(argument)) {
                Command command = commands.get(argument);
                Parser parser = command.getParser();
                Arguments parsedArguments = parser.parse(args, 1);
                // Use the --no-color argument to globally disable ANSI colors.
                if (parsedArguments.has("--no-color")) {
                    Colors.setUseAnsiColors(false);
                }
                // Automatically handle --help output for subcommands.
                if (parsedArguments.has("--help")) {
                    printHelp(command, parser);
                } else {
                    configureLogging(args);
                    command.execute(parsedArguments);
                }
            } else {
                throw new CliError("Unknown command or argument: '" + argument + "'", 1);
            }

            return 0;
        } catch (CliError e) {
            printException(args, e);
            return e.code;
        } catch (Exception e) {
            printException(args, e);
            return 1;
        }
    }

    private void configureLogging(String[] args) {
        Handler handler = getConsoleHandler();
        handler.setFormatter(new SimpleFormatter() {
            @Override
            public synchronized String format(LogRecord r) {
                return String.format(
                        LOG_FORMAT, new Date(r.getMillis()), r.getLevel().getLocalizedName(), r.getMessage());
            }
        });

        if (hasArgument(args, "--debug")) {
            handler.setLevel(Level.FINEST);
        }
    }

    private static Handler getConsoleHandler() {
        Logger rootLogger = Logger.getLogger("");

        for (Handler handler : rootLogger.getHandlers()) {
            if (handler instanceof ConsoleHandler) {
                return handler;
            }
        }

        Handler consoleHandler = new ConsoleHandler();
        rootLogger.addHandler(consoleHandler);
        return consoleHandler;
    }

    private boolean hasArgument(String[] args, String name) {
        for (String arg : args) {
            if (arg.equals(name)) {
                return true;
            }
        }

        return false;
    }

    private void printException(String[] args, Throwable throwable) {
        if (hasArgument(args, "--no-color")) {
            Colors.setUseAnsiColors(false);
        }

        Colors.out(Colors.RED, throwable.getMessage());
        if (hasArgument(args, "--debug")) {
            StringWriter sw = new StringWriter();
            throwable.printStackTrace(new PrintWriter(sw));
            String trace = sw.toString();
            Colors.out(Colors.RED, trace);
        }
    }

    private void printMainHelp() {
        Colors.out(Colors.BRIGHT_WHITE,
                   String.format("Usage: %s [-h | --help] <command> [<args>]%n", applicationName));
        System.out.println("commands:");
        Map<String, String> table = new LinkedHashMap<>();
        for (Map.Entry<String, Command> entry : commands.entrySet()) {
            table.put("  " + entry.getKey(), entry.getValue().getSummary());
        }
        System.out.println(createTable(table).trim());
    }

    private String createTable(Map<String, String> table) {
        StringBuilder builder = new StringBuilder();
        int longestLen = table.keySet().stream().map(String::length).mapToInt(v -> v).max().orElse(0) + 4;
        table.forEach((k, v) -> {
            int padding = longestLen - k.length();
            builder.append(k);
            for (int i = 0; i < padding; i++) {
                builder.append(" ");
            }
            builder.append(v).append("\n");
        });

        return builder.toString();
    }

    private void printHelp(Command command, Parser parser) {
        // First print the start/example line.
        StringBuilder example = new StringBuilder();
        example.append(applicationName).append(" ").append(command.getName());

        // In the example line, print each argument.
        parser.getArgs().forEach(arg -> {
            // Omit the built-in --help arguments.
            if (!arg.getLongName().filter(name -> name.equals("--help")).isPresent()) {
                example.append(" [");
                arg.getShortName().ifPresent(example::append);
                if (arg.getShortName().isPresent() && arg.getLongName().isPresent()) {
                    example.append(" | ");
                }
                arg.getLongName().ifPresent(example::append);
                if (arg.getArity() == Parser.Arity.MANY) {
                    example.append(" ...");
                }
                example.append("]");
            }
        });

        // Print the options name if present.
        parser.getPositionalName().ifPresent(name -> example.append(" ").append(name));
        Colors.out(Colors.BRIGHT_WHITE, example.append("\n").toString());

        // Print the summary of the command.
        StringBuilder body = new StringBuilder();
        body.append(command.getSummary()).append("\n\n");

        // Print each argument. Pad to ensure they are all aligned.
        Map<String, String> table = new LinkedHashMap<>();
        parser.getArgs().forEach(arg -> {
            StringBuilder key = new StringBuilder("  ");
            arg.getShortName().ifPresent(key::append);
            if (arg.getShortName().isPresent() && arg.getLongName().isPresent()) {
                key.append(" | ");
            }
            arg.getLongName().ifPresent(key::append);
            if (arg.getArity() == Parser.Arity.MANY) {
                key.append(" ...");
            }
            table.put(key.toString(), arg.getHelp());
        });

        // Add the options description to the table.
        parser.getPositionalName().ifPresent(name -> {
            table.put("  " + name + "  ", parser.getPositionalHelp().orElse(""));
        });

        body.append(createTable(table).trim());

        String help = command.getHelp();
        if (!help.isEmpty()) {
            body.append("\n\n").append(command.getHelp().trim());
        }

        System.out.println(body);
    }
}
