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

import java.util.function.Consumer;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Provides an abstraction for printing with ANSI colors if it is supported.
 */
@SmithyUnstableApi
public enum Colors {
    BLACK(30),
    RED(31),
    BOLD_RED(31, true),
    GREEN(32),
    BOLD_GREEN(32, true),
    YELLOW(33),
    BLUE(34),
    MAGENTA(35),
    CYAN(36),
    WHITE(37),
    BRIGHT_BLACK(90),
    BRIGHT_RED(91),
    BRIGHT_BOLD_RED(91, true),
    BRIGHT_GREEN(92),
    BRIGHT_BOLD_GREEN(92, true),
    BRIGHT_YELLOW(93),
    BRIGHT_BOLD_YELLOW(93, true),
    BRIGHT_BLUE(94),
    BRIGHT_MAGENTA(95),
    BRIGHT_CYAN(96),
    BRIGHT_WHITE(97);

    private int escape;
    private boolean bold;

    Colors(int escape) {
        this(escape, false);
    }

    Colors(int escape, boolean bold) {
        this.escape = escape;
        this.bold = bold;
    }

    /**
     * Prints to stdout using the Color if ANSI colors are enabled.
     *
     * @param message Message to print.
     */
    public void out(String message) {
        write(Cli.getStdout(), message);
    }

    /**
     * Prints to stderr using the Color if ANSI colors are enabled.
     *
     * @param message Message to print.
     */
    public void err(String message) {
        write(Cli.getStderr(), message);
    }

    /**
     * Writes the color output to the given consumer.
     *
     * @param consumer Consume to invoke.
     * @param message Message to write.
     */
    public void write(Consumer<String> consumer, String message) {
        if (Cli.useAnsiColors) {
            consumer.accept(format(message));
        } else {
            consumer.accept(message);
        }
    }

    private String format(String message) {
        String colored = String.format("\u001b[%dm%s\u001b[0m", escape, message);
        return bold ? String.format("\033[1m%s\033[0m", colored) : colored;
    }
}
