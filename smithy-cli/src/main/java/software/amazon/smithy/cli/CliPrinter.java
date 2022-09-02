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
import java.util.function.Consumer;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Handles text output of the CLI.
 */
@SmithyUnstableApi
public interface CliPrinter {
    /**
     * Prints text to the writer and appends a new line.
     *
     * @param text Text to print.
     */
    void println(String text);

    /**
     * Styles the given text using ANSI escape sequences.
     *
     * <p>It is strongly recommended to use the constants defined in
     * {@link Style} to provide valid combinations of ANSI color escape
     * codes.
     *
     * @param text Text to style.
     * @param styles ANSI escape codes.
     * @return Returns the styled text.
     */
    default String style(String text, Style... styles) {
        return Style.format(text, styles);
    }

    /**
     * Print an exception to the printer.
     *
     * @param e Exception to print.
     * @param stacktrace Whether to include a stack trace.
     */
    default void printException(Throwable e, boolean stacktrace) {
        if (!stacktrace) {
            println(style(e.getMessage(), Style.RED));
        } else {
            StringWriter writer = new StringWriter();
            e.printStackTrace(new PrintWriter(writer));
            String result = writer.toString();
            int positionOfName = result.indexOf(':');
            result = style(result.substring(0, positionOfName), Style.RED, Style.UNDERLINE)
                     + result.substring(positionOfName);
            println(result);
        }
    }

    /**
     * CliPrinter that calls a Consumer that accepts a CharSequence.
     */
    final class ConsumerPrinter implements CliPrinter {
        private final Consumer<CharSequence> consumer;

        public ConsumerPrinter(Consumer<CharSequence> consumer) {
            this.consumer = consumer;
        }

        @Override
        public void println(String text) {
            consumer.accept(text + System.lineSeparator());
        }
    }

    /**
     * A CliPrinter that prints ANSI colors if able and allowed.
     */
    final class ColorPrinter implements CliPrinter {
        private final CliPrinter delegate;
        private final StandardOptions options;
        private final boolean ansiSupported;

        public ColorPrinter(CliPrinter delegate, StandardOptions options) {
            this.delegate = delegate;
            this.options = options;
            this.ansiSupported = isAnsiColorSupported();
        }

        private static boolean isAnsiColorSupported() {
            return System.console() != null && System.getenv().get("TERM") != null;
        }

        @Override
        public void println(String text) {
            delegate.println(text);
        }

        @Override
        public String style(String text, Style... styles) {
            if (options.forceColor() || (!options.noColor() && ansiSupported)) {
                return delegate.style(text, styles);
            } else {
                return text;
            }
        }
    }
}
