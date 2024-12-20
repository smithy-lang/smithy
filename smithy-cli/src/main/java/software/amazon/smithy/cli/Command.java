/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.cli;

/**
 * Represents a CLI command.
 */
public interface Command {
    /**
     * Gets the name of the command.
     *
     * <p>The returned name should contain no spaces or special characters.
     *
     * @return Returns the command name.
     */
    String getName();

    /**
     * Return true to hide this command from help output.
     *
     * @return Return true if this is a hidden command.
     */
    default boolean isHidden() {
        return false;
    }

    /**
     * Gets a short summary of the command that's shown in the main help.
     *
     * @return Returns the short help description.
     */
    String getSummary();

    /**
     * Executes the command using the provided arguments.
     *
     * @param arguments CLI arguments.
     * @param env CLI environment settings like stdout, stderr, etc.
     * @return Returns the exit code.
     */
    int execute(Arguments arguments, Env env);

    /**
     * Environment settings for the command.
     */
    final class Env {

        private final CliPrinter stdout;
        private final CliPrinter stderr;
        private final ColorFormatter colors;
        private final ClassLoader classLoader;

        public Env(ColorFormatter colors, CliPrinter stdout, CliPrinter stderr, ClassLoader classLoader) {
            this.colors = colors;
            this.stdout = stdout;
            this.stderr = stderr;
            this.classLoader = classLoader;
        }

        public ColorFormatter colors() {
            return colors;
        }

        public CliPrinter stdout() {
            return stdout;
        }

        public CliPrinter stderr() {
            return stderr;
        }

        public ClassLoader classLoader() {
            return classLoader == null ? getClass().getClassLoader() : classLoader;
        }

        public void flush() {
            stderr.flush();
            stdout.flush();
        }

        public Env withClassLoader(ClassLoader classLoader) {
            return classLoader == this.classLoader ? this : new Env(colors, stdout, stderr, classLoader);
        }
    }
}
