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
import java.util.Objects;
import java.util.logging.Logger;

/**
 * This class provides a basic CLI abstraction.
 *
 * <p>Why are we not using a library for this? Because parsing command line
 * options isn't difficult, we don't need to take a dependency, this code
 * uses no reflection to improve startup time. We can control exactly what
 * CLI features are supported in case we want to migrate to a library or
 * event a different language.
 */
public final class Cli {

    private static final Logger LOGGER = Logger.getLogger(Cli.class.getName());
    private CliPrinter stdoutPrinter;
    private CliPrinter stdErrPrinter;

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
     * @throws CliError on error.
     */
    public int run(String[] args) {
        Arguments arguments = new Arguments(args);
        StandardOptions standardOptions = new StandardOptions();
        arguments.addReceiver(standardOptions);

        if (stdoutPrinter == null) {
            stdoutPrinter = System.out::println;
        }

        if (stdErrPrinter == null) {
            stdErrPrinter = System.err::println;
        }

        // Use or disable ANSI escapes in the printers.
        CliPrinter out = new ColorPrinter(stdoutPrinter);
        CliPrinter err = new ColorPrinter(stdErrPrinter);

        // Setup logging after parsing all arguments.
        arguments.onComplete((opts, positional) -> {
            LoggingUtil.configureLogging(opts.getReceiver(StandardOptions.class), err);
            LOGGER.fine(() -> "Running CLI command: " + Arrays.toString(args));
        });

        try {
            return command.execute(arguments, new Command.Env(out, err, classLoader));
        } catch (Exception e) {
            err.printException(e, standardOptions.stackTrace());
            throw CliError.wrap(e);
        } finally {
            try {
                LoggingUtil.restoreLogging();
            } catch (RuntimeException e) {
                // Show the error, but don't fail the CLI since most invocations are one-time use.
                err.println(err.style("Unable to restore logging to previous settings", Style.RED));
                err.printException(e, standardOptions.stackTrace());
            }
        }
    }

    public void stdout(CliPrinter printer) {
        stdoutPrinter = synchronizedPrinter(printer);
    }

    public void stderr(CliPrinter printer) {
        stdErrPrinter = synchronizedPrinter(printer);
    }

    private CliPrinter synchronizedPrinter(CliPrinter printer) {
        return new CliPrinter() {
            @Override
            public void println(String text) {
                synchronized (printer) {
                    printer.println(text);
                }
            }

            @Override
            public String style(String text, Style... styles) {
                return printer.style(text, styles);
            }
        };
    }

    /**
     * A CliPrinter that prints ANSI colors if able and allowed.
     */
    private static final class ColorPrinter implements CliPrinter {
        private final CliPrinter delegate;
        private final boolean ansiSupported = isAnsiColorSupported();

        ColorPrinter(CliPrinter delegate) {
            this.delegate = delegate;
        }

        private static boolean isAnsiColorSupported() {
            if (EnvironmentVariable.FORCE_COLOR.isSet()) {
                return true;
            }

            // Disable colors if NO_COLOR is set to anything.
            if (EnvironmentVariable.NO_COLOR.isSet()) {
                return false;
            }

            String term = EnvironmentVariable.TERM.get();

            // If term is set to "dumb", then don't use colors.
            if (Objects.equals(term, "dumb")) {
                return false;
            }

            // If TERM isn't set at all and Windows is detected, then don't use colors.
            if (term == null && System.getProperty("os.name").contains("win")) {
                return false;
            }

            // Disable colors if no console is associated.
            return System.console() != null;
        }

        @Override
        public void println(String text) {
            delegate.println(text);
        }

        @Override
        public String style(String text, Style... styles) {
            return ansiSupported ? delegate.style(text, styles) : text;
        }
    }
}
