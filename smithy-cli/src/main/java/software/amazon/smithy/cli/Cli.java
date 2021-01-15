/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import software.amazon.smithy.utils.SmithyUnstableApi;
import software.amazon.smithy.utils.StringUtils;

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
 *     <li>--force-color: Explicitly enables ANSI colors.</li>
 *     <li>--stacktrace: Prints the stacktrace of any CLI exception that is thrown.</li>
 *     <li>--logging: Sets the log level to one of OFF, SEVERE, WARNING, INFO, FINE, ALL.</li>
 * </ul>
 *
 * <p>Why are we not using a library for this? Because parsing command line
 * options isn't difficult, we don't need to take a dependency, this code
 * uses no reflection to improve startup time. We can control exactly what
 * CLI features are supported in case we want to migrate to a library or
 * event a different language.
 */
@SmithyUnstableApi
public final class Cli {
    public static final String HELP = "--help";
    public static final String NO_COLOR = "--no-color";
    public static final String FORCE_COLOR = "--force-color";
    public static final String DEBUG = "--debug";
    public static final String STACKTRACE = "--stacktrace";
    public static final String LOGGING = "--logging";

    /** Configures whether or not to use ANSI colors. */
    static boolean useAnsiColors = isAnsiColorSupported();

    private static final SimpleDateFormat FORMAT = new SimpleDateFormat("HH:mm:ss.SSS");

    // Note that we don't use a method reference here in case System.out or System.err are changed.
    private static Consumer<String> stdout = s -> System.out.println(s);
    private static Consumer<String> stderr = s -> System.err.println(s);

    private final String applicationName;
    private final ClassLoader classLoader;
    private Map<String, Command> commands = new TreeMap<>();

    /**
     * Creates a new CLI with the given name.
     *
     * @param applicationName Name of the CLI application.
     */
    public Cli(String applicationName) {
        this(applicationName, Cli.class.getClassLoader());
    }

    /**
     * Creates a new CLI with the given name.
     *
     * @param applicationName Name of the CLI application.
     * @param classLoader ClassLoader to use when invoking commands.
     */
    public Cli(String applicationName, ClassLoader classLoader) {
        this.applicationName = applicationName;
        this.classLoader = classLoader;
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
     */
    public void run(String[] args) {
        try {
            // No args is not a valid use of the command, so
            // print help and exit with an error code.
            if (args.length == 0) {
                printMainHelp();
                return;
            }

            String argument = args[0];
            if (argument.equals("-h") || argument.equals(HELP)) {
                printMainHelp();
            } else if (commands.containsKey(argument)) {
                Command command = commands.get(argument);
                Parser parser = command.getParser();
                Arguments parsedArguments = parser.parse(args, 1);

                // Use the --no-color argument to globally disable ANSI colors.
                if (parsedArguments.has(NO_COLOR)) {
                    setUseAnsiColors(false);
                } else if (parsedArguments.has(FORCE_COLOR)) {
                    setUseAnsiColors(true);
                }

                // Automatically handle --help output for subcommands.
                if (parsedArguments.has(HELP)) {
                    printHelp(command, parser);
                } else {
                    configureLogging(parsedArguments);
                    command.execute(parsedArguments, classLoader);
                }
            } else {
                throw new CliError("Unknown command or argument: '" + argument + "'", 1);
            }
        } catch (Exception e) {
            printException(args, e);
            throw e;
        }
    }

    /**
     * Configures a custom STDOUT printer.
     *
     * @param printer Consumer responsible for writing to STDOUT.
     */
    public static void setStdout(Consumer<String> printer) {
        stdout = printer;
    }

    /**
     * Configures a custom STDERR printer.
     *
     * @param printer Consumer responsible for writing to STDERR.
     */
    public static void setStderr(Consumer<String> printer) {
        stderr = printer;
    }

    /**
     * Gets the stdout consumer.
     *
     * @return Returns the stdout consumer.
     */
    public static Consumer<String> getStdout() {
        return stdout;
    }

    /**
     * Gets the stderr consumer.
     *
     * @return Returns the stderr consumer.
     */
    public static Consumer<String> getStderr() {
        return stderr;
    }

    /**
     * Write a line of text to the configured STDOUT.
     *
     * @param message Message to write.
     */
    public static void stdout(Object message) {
        stdout.accept(String.valueOf(message));
    }

    /**
     * Write a line of text to the configured STDERR.
     *
     * @param message Message to write.
     */
    public static void stderr(Object message) {
        stderr.accept(String.valueOf(message));
    }

    /**
     * Explicitly configures whether or not to use ANSI colors.
     *
     * @param useAnsiColors Set to true or false to enable/disable.
     */
    public static void setUseAnsiColors(boolean useAnsiColors) {
        Cli.useAnsiColors = useAnsiColors;
    }

    /**
     * Does a really simple check to see if ANSI colors are supported.
     *
     * @return Returns true if ANSI probably works.
     */
    private static boolean isAnsiColorSupported() {
        return System.console() != null && System.getenv().get("TERM") != null;
    }

    private void configureLogging(Arguments parsedArgs) {
        boolean configureLogging = parsedArgs.has(DEBUG) || parsedArgs.has(LOGGING);

        if (!configureLogging) {
            return;
        }

        Level level = Level.parse(parsedArgs.parameter(LOGGING, Level.ALL.getName()));

        // Remove any currently present console loggers.
        Logger rootLogger = Logger.getLogger("");
        removeConsoleHandler(rootLogger);

        if (parsedArgs.has(DEBUG)) {
            // Debug ignores the given logging level and just logs everything.
            CliLogHandler handler = new CliLogHandler(new DebugFormatter());
            handler.setLevel(Level.ALL);
            rootLogger.addHandler(handler);
            rootLogger.setLevel(Level.ALL);
            for (Handler h : rootLogger.getHandlers()) {
                h.setLevel(Level.ALL);
            }
        } else if (level != Level.OFF) {
            CliLogHandler handler = new CliLogHandler(new BasicFormatter());
            handler.setLevel(level);
            rootLogger.addHandler(handler);
        }
    }

    private static void removeConsoleHandler(Logger rootLogger) {
        for (Handler handler : rootLogger.getHandlers()) {
            if (handler instanceof ConsoleHandler) {
                // Remove any console log handlers.
                rootLogger.removeHandler(handler);
            }
        }
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
        if (hasArgument(args, NO_COLOR)) {
            setUseAnsiColors(false);
        }

        Colors.BOLD_RED.out(throwable.getMessage());
        if (hasArgument(args, STACKTRACE)) {
            StringWriter sw = new StringWriter();
            throwable.printStackTrace(new PrintWriter(sw));
            String trace = sw.toString();
            Colors.RED.out(trace);
        }
    }

    private void printMainHelp() {
        Colors.BRIGHT_WHITE.out(String.format("Usage: %s [-h | --help] <command> [<args>]%n", applicationName));
        stdout("commands:");
        Map<String, String> table = new LinkedHashMap<>();
        for (Map.Entry<String, Command> entry : commands.entrySet()) {
            table.put("  " + entry.getKey(), entry.getValue().getSummary());
        }
        stdout(StringUtils.stripEnd(createTable(table), " \t\r\n"));
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
            if (!arg.getLongName().filter(name -> name.equals(HELP)).isPresent()) {
                example.append(" [");
                writeArgHelp(arg, example);
                example.append("]");
            }
        });

        // Print the options name if present.
        parser.getPositionalName().ifPresent(name -> example.append(" ").append(name));
        Colors.BRIGHT_WHITE.out(example.append("\n").toString());

        // Print the summary of the command.
        StringBuilder body = new StringBuilder();
        body.append(command.getSummary()).append("\n\n");

        // Print each argument. Pad to ensure they are all aligned.
        Map<String, String> table = new LinkedHashMap<>();
        parser.getArgs().forEach(arg -> {
            StringBuilder key = new StringBuilder("  ");
            writeArgHelp(arg, key);
            table.put(key.toString(), arg.getHelp());
        });

        // Add the options description to the table.
        parser.getPositionalName().ifPresent(name -> {
            table.put("  " + name + "  ", parser.getPositionalHelp().orElse(""));
        });

        body.append("  ").append(createTable(table).trim());

        String help = command.getHelp();
        if (!help.isEmpty()) {
            body.append("\n\n").append(command.getHelp().trim());
        }

        stdout(body.toString());
    }

    private void writeArgHelp(Parser.Argument arg, StringBuilder sink) {
        arg.getShortName().ifPresent(sink::append);
        if (arg.getShortName().isPresent() && arg.getLongName().isPresent()) {
            sink.append(" | ");
        }
        arg.getLongName().ifPresent(sink::append);
        if (arg.getArity() == Parser.Arity.MANY) {
            sink.append(" ...");
        }
    }

    private static final class BasicFormatter extends SimpleFormatter {
        @Override
        public synchronized String format(LogRecord r) {
            return FORMAT.format(new Date(r.getMillis()))
                   + " [" + r.getLevel().getLocalizedName() + "] "
                   + r.getMessage() + System.lineSeparator();
        }
    }

    private static final class DebugFormatter extends SimpleFormatter {
        @Override
        public synchronized String format(LogRecord r) {
            return FORMAT.format(new Date(r.getMillis()))
                   + " [" + r.getLevel().getLocalizedName() + "] ["
                   + r.getLoggerName() + "] "
                   + r.getMessage() + System.lineSeparator();
        }
    }

    /**
     * Logs messages to the CLI's redirect stderr.
     */
    private static final class CliLogHandler extends Handler {
        private final Formatter formatter;

        CliLogHandler(Formatter formatter) {
            this.formatter = formatter;
        }

        @Override
        public void publish(LogRecord record) {
            if (isLoggable(record)) {
                Cli.stderr(formatter.format(record));
            }
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    }
}
