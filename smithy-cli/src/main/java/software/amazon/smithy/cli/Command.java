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
     * Gets the long description of the command.
     *
     * @param printer Printer used to style strings.
     * @return Returns the long description.
     */
    default String getDocumentation(CliPrinter printer) {
        return "";
    }

    /**
     * Prints help output.
     *
     * @param arguments Arguments that have been parsed so far.
     * @param printer Where to write help.
     */
    void printHelp(Arguments arguments, CliPrinter printer);

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
        private final ClassLoader classLoader;

        public Env(CliPrinter stdout, CliPrinter stderr, ClassLoader classLoader) {
            this.stdout = stdout;
            this.stderr = stderr;
            this.classLoader = classLoader;
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

        public Env withClassLoader(ClassLoader classLoader) {
            return classLoader == this.classLoader ? this : new Env(stdout, stderr, classLoader);
        }
    }
}
