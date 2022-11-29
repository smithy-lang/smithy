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
import java.util.function.Consumer;
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
    private static CliPrinter deprecatedStdOut;

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

        if (deprecatedStdOut != null) {
            stdout(deprecatedStdOut);
            stderr(deprecatedStdOut);
        }
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

        // Use or disable ANSI escapes in the printers. Note that determining the color setting is deferred
        // using a Supplier to allow the CLI parameters to be fully resolved.
        CliPrinter out = new CliPrinter.ColorPrinter(stdoutPrinter, standardOptions::colorSetting);
        CliPrinter err = new CliPrinter.ColorPrinter(stdErrPrinter, standardOptions::colorSetting);

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
        stdoutPrinter = printer;
    }

    public void stderr(CliPrinter printer) {
        stdErrPrinter = printer;
    }

    // This method exists to offer compatibility with older Smithy Gradle plugins and silence
    // their build warning messages. This method may be removed in the future. Use instance methods instead.
    @Deprecated
    public static void setStdout(Consumer<String> consumer) {
        deprecatedStdOut = new CliPrinter.ConsumerPrinter(text -> consumer.accept(text.toString()));
    }
}
