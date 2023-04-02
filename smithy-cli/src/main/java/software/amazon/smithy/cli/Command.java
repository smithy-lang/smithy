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
