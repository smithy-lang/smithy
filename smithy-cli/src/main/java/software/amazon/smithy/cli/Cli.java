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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.function.Supplier;

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

    private ColorFormatter colorFormatter;
    private CliPrinter stdoutPrinter;
    private CliPrinter stderrPrinter;
    private final ClassLoader classLoader;
    private final Command command;
    private final StandardOptions standardOptions = new StandardOptions();

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
        if (colorFormatter == null) {
            // CLI arguments haven't been parsed yet, so the CLI doesn't know if --force-color or --no-color
            // was passed. Defer the color setting implementation by asking StandardOptions before each write.
            colorFormatter(createDelegatedColorFormatter(standardOptions::colorSetting));
        }

        if (stdoutPrinter == null) {
            stdout(CliPrinter.fromOutputStream(System.out));
        }

        if (stderrPrinter == null) {
            stderr(CliPrinter.fromOutputStream(System.err));
        }


        LoggingArgumentsHandler arguments = new LoggingArgumentsHandler(colorFormatter,
                                                                        stderrPrinter,
                                                                        Arguments.of(args));
        arguments.addReceiver(standardOptions);

        try {
            try {
                Command.Env env = new Command.Env(colorFormatter, stdoutPrinter, stderrPrinter, classLoader);
                return command.execute(arguments, env);
            } catch (Exception e) {
                stdoutPrinter.flush();
                stderrPrinter.flush();
                printException(e, standardOptions.stackTrace());
                throw CliError.wrap(e);
            } finally {
                try {
                    arguments.restoreLogging();
                } catch (RuntimeException e) {
                    // Show the error, but don't fail the CLI since most invocations are one-time use.
                    printException(e, standardOptions.stackTrace());
                }
            }
        } finally {
            stdoutPrinter.flush();
            stderrPrinter.flush();
        }
    }

    public void colorFormatter(ColorFormatter colorFormatter) {
        this.colorFormatter = colorFormatter;
    }

    public void stdout(CliPrinter stdoutPrinter) {
        this.stdoutPrinter = stdoutPrinter;
    }

    public void stderr(CliPrinter stderrPrinter) {
        this.stderrPrinter = stderrPrinter;
    }

    private void printException(Throwable e, boolean stacktrace) {
        try (ColorBuffer buffer = ColorBuffer.of(colorFormatter, stderrPrinter)) {
            if (!stacktrace) {
                colorFormatter.println(stderrPrinter, e.getMessage(), ColorTheme.ERROR);
            } else {
                StringWriter writer = new StringWriter();
                e.printStackTrace(new PrintWriter(writer));
                String result = writer.toString();
                int positionOfName = result.indexOf(':');
                buffer.print(result.substring(0, positionOfName), ColorTheme.ERROR);
                buffer.println(result.substring(positionOfName));
            }
        }
    }

    private static ColorFormatter createDelegatedColorFormatter(Supplier<ColorFormatter> delegateSupplier) {
        return new ColorFormatter() {
            @Override
            public String style(String text, Style... styles) {
                return delegateSupplier.get().style(text, styles);
            }

            @Override
            public void style(Appendable appendable, String text, Style... styles) {
                delegateSupplier.get().style(appendable, text, styles);
            }

            @Override
            public boolean isColorEnabled() {
                return delegateSupplier.get().isColorEnabled();
            }

            @Override
            public void println(Appendable appendable, String text, Style... styles) {
                delegateSupplier.get().println(appendable, text, styles);
            }

            @Override
            public void startStyle(Appendable appendable, Style... style) {
                delegateSupplier.get().startStyle(appendable, style);
            }

            @Override
            public void endStyle(Appendable appendable) {
                delegateSupplier.get().endStyle(appendable);
            }
        };
    }
}
