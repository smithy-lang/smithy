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

package software.amazon.smithy.cli;

import java.util.Arrays;
import java.util.logging.Logger;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * This class provides a very basic CLI abstraction.
 *
 * <p>Why are we not using a library for this? Because parsing command line
 * options isn't difficult, we don't need to take a dependency, this code
 * uses no reflection to improve startup time. We can control exactly what
 * CLI features are supported in case we want to migrate to a library or
 * event a different language.
 */
@SmithyUnstableApi
public final class Cli {

    private static final Logger LOGGER = Logger.getLogger(Cli.class.getName());
    private static final boolean ANSI_SUPPORTED = isAnsiColorSupported();

    // Delegate to the stdout consumer by default since this can change.
    private CliPrinter stdoutPrinter = new CliPrinter.ConsumerPrinter(str -> System.out.print(str));

    // Don't use a method reference in case System.err is changed after initialization.
    private CliPrinter stdErrPrinter = new CliPrinter.ConsumerPrinter(str -> System.err.print(str));

    private final ClassLoader classLoader;
    private final Command command;

    /**
     * Creates a new CLI with the given name.
     *
     * @param command CLI command to run.
     * @param classLoader ClassLoader to use when invoking commands.
     */
    public Cli(Command command, ClassLoader classLoader) {
        this.command = command;
        this.classLoader = classLoader;
    }

    /**
     * Execute the command line using the given arguments.
     *
     * @param args Arguments to parse.
     * @return Returns the exit code.
     */
    public int run(String[] args) {
        Arguments arguments = new Arguments(args);
        StandardOptions standardOptions = new StandardOptions();
        arguments.addReceiver(standardOptions);

        // Use or disable ANSI escapes in the printers.
        CliPrinter out = ansiPrinter(stdoutPrinter, standardOptions);
        CliPrinter err = ansiPrinter(stdErrPrinter, standardOptions);

        // Setup logging after parsing all arguments.
        arguments.onComplete((opts, positional) -> {
            LoggingUtil.configureLogging(opts.getReceiver(StandardOptions.class), err);
            LOGGER.fine(() -> "Running CLI command: " + Arrays.toString(args));
        });

        try {
            return command.execute(arguments, new Command.Env(out, err, classLoader));
        } catch (Exception e) {
            printException(standardOptions, err, e);
            throw e;
        } finally {
            // Attempt to restore log settings as they were originally before running the CLI.
            LoggingUtil.restoreLogging();
        }
    }

    public void stdout(CliPrinter printer) {
        stdoutPrinter = printer;
    }

    public void stderr(CliPrinter printer) {
        stdErrPrinter = printer;
    }

    /**
     * Does a really simple check to see if ANSI colors are supported.
     *
     * @return Returns true if ANSI probably works.
     */
    private static boolean isAnsiColorSupported() {
        return System.console() != null && System.getenv().get("TERM") != null;
    }

    private void printException(StandardOptions options, CliPrinter printer, Throwable throwable) {
        if (throwable instanceof NullPointerException) {
            printer.println(stdErrPrinter.style(
                    "A null pointer exception occurred while running the Smithy CLI. The --stacktrace argument can be "
                    + "used to get more information. Please open an issue with the Smithy team on GitHub so this can "
                    + "be investigated: https://github.com/awslabs/smithy/issues", Style.RED));
        }

        printer.println(printer.style(throwable.getMessage(), Style.RED, Style.BOLD));

        if (options.stackTrace()) {
            printer.println(printer.style(throwable.getClass().getCanonicalName() + ":", Style.RED, Style.UNDERLINE));
            for (StackTraceElement element : throwable.getStackTrace()) {
                printer.println("\tat " + element.toString());
            }
        }
    }

    /**
     * Creates a CliPrinter that inspects provided options to determine whether
     * to use ANSI.
     *
     * @param delegate Printer to delegate write and formatting to.
     * @param options Options to query when it's parsed.
     * @return Returns the created printer.
     */
    private static CliPrinter ansiPrinter(CliPrinter delegate, StandardOptions options) {
        return new CliPrinter() {
            @Override
            public void println(String text) {
                delegate.println(text);
            }

            @Override
            public String style(String text, Style... styles) {
                if (options.forceColor() || (!options.noColor() && ANSI_SUPPORTED)) {
                    return delegate.style(text, styles);
                } else {
                    return text;
                }
            }
        };
    }
}
