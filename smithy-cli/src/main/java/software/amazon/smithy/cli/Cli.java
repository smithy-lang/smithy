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
import java.util.Arrays;
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
    private CliPrinter stdout;
    private CliPrinter stderr;
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

        if (stdout == null || stderr == null) {
            Ansi ansi = Ansi.detect();
            if (stdout == null) {
                stdout(CliPrinter.fromOutputStream(ansi, System.out));
            }
            if (stderr == null) {
                stderr(CliPrinter.fromOutputStream(ansi, System.err));
            }
        }

        // Setup logging after parsing all arguments.
        arguments.onComplete((opts, positional) -> {
            LoggingUtil.configureLogging(opts.getReceiver(StandardOptions.class), stderr);
            LOGGER.fine(() -> "Running CLI command: " + Arrays.toString(args));
        });

        try {
            try {
                Command.Env env = new Command.Env(stdout, stderr, classLoader);
                return command.execute(arguments, env);
            } catch (Exception e) {
                printException(e, standardOptions.stackTrace());
                throw CliError.wrap(e);
            } finally {
                try {
                    LoggingUtil.restoreLogging();
                } catch (RuntimeException e) {
                    // Show the error, but don't fail the CLI since most invocations are one-time use.
                    printException(e, standardOptions.stackTrace());
                }
            }
        } finally {
            stdout.flush();
            stderr.flush();
        }
    }

    public void stdout(CliPrinter stdout) {
        this.stdout = stdout;
    }

    public void stderr(CliPrinter stderr) {
        this.stderr = stderr;
    }

    private void printException(Throwable e, boolean stacktrace) {
        if (!stacktrace) {
            stderr.println(e.getMessage(), Style.RED);
        } else {
            try (CliPrinter.Buffer buffer = stderr.buffer()) {
                StringWriter writer = new StringWriter();
                e.printStackTrace(new PrintWriter(writer));
                String result = writer.toString();
                int positionOfName = result.indexOf(':');
                buffer.print(result.substring(0, positionOfName), Style.RED, Style.UNDERLINE);
                buffer.println(result.substring(positionOfName));
            }
        }
    }
}
