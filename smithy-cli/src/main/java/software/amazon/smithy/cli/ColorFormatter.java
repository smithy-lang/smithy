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

import java.util.function.Consumer;

/**
 * Styles text using color codes.
 *
 * @see AnsiColorFormatter for the ANSI implementation.
 */
public interface ColorFormatter {
    /**
     * Styles text using the given styles.
     *
     * @param text Text to style.
     * @param styles Styles to apply.
     * @return Returns the styled text.
     */
    String style(String text, Style... styles);

    /**
     * Styles text using the given styles and writes it to an Appendable.
     *
     * @param appendable Where to write styled text.
     * @param text Text to write.
     * @param styles Styles to apply.
     */
    void style(Appendable appendable, String text, Style... styles);

    /**
     * Adds styles around text written to an appendable by a consumer.
     *
     * @param appendable Appendable to write to.
     * @param consumer   Consumer to write to the appendable using style.
     * @param styles     Styles to apply to the text written by the consumer.
     * @param <T>        Appendable type.
     */
    <T extends Appendable> void style(T appendable, Consumer<T> consumer, Style... styles);

    /**
     * @return Returns true if this formatter supports color output.
     */
    boolean isColorEnabled();

    /**
     * Print a styled line of text to the given {@code printer}.
     *
     * @param printer Printer to write to.
     * @param text Text to write.
     * @param styles Styles to apply.
     */
    default void println(CliPrinter printer, String text, Style... styles) {
        printer.println(style(text, styles));
    }

    /**
     * Creates a {@link PrinterBuffer} used to build up a long string of styled text.
     *
     * <p>Call {@link PrinterBuffer#close()} or use try-with-resources to write to the printer.
     *
     * @return Returns the buffer.
     */
    default PrinterBuffer printerBuffer(CliPrinter printer) {
        return new PrinterBuffer(this, printer);
    }

    /**
     * A buffer associated with a {@link CliPrinter} used to build up a string of colored text.
     *
     * <p>Use {@link #close()} to write to the associated {@link CliPrinter}, or wrap the buffer in a
     * try-with-resources block.
     */
    final class PrinterBuffer implements Appendable, AutoCloseable {
        private final ColorFormatter colors;
        private final CliPrinter printer;
        private final StringBuilder builder = new StringBuilder();
        private boolean pendingNewline;
        private final String newline = System.lineSeparator();

        private PrinterBuffer(ColorFormatter colors, CliPrinter printer) {
            this.colors = colors;
            this.printer = printer;
        }

        @Override
        public String toString() {
            String result = builder.toString();
            if (pendingNewline) {
                result += newline;
            }
            return result;
        }

        @Override
        public PrinterBuffer append(CharSequence csq) {
            handleNewline();
            builder.append(csq);
            return this;
        }

        private void handleNewline() {
            if (pendingNewline) {
                builder.append(newline);
                pendingNewline = false;
            }
        }

        @Override
        public PrinterBuffer append(CharSequence csq, int start, int end) {
            handleNewline();
            builder.append(csq, start, end);
            return this;
        }

        @Override
        public PrinterBuffer append(char c) {
            handleNewline();
            builder.append(c);
            return this;
        }

        /**
         * Writes styled text to the builder using the CliPrinter's color settings.
         *
         * @param text Text to write.
         * @param styles Styles to apply to the text.
         * @return Returns self.
         */
        public PrinterBuffer print(String text, Style... styles) {
            handleNewline();
            colors.style(this, text, styles);
            return this;
        }

        /**
         * Prints a line of styled text to the buffer.
         *
         * @param text Text to print.
         * @param styles Styles to apply.
         * @return Returns self.
         */
        public PrinterBuffer println(String text, Style... styles) {
            print(text, styles);
            return println();
        }

        /**
         * Writes a system-dependent new line.
         *
         * @return Returns the buffer.
         */
        public PrinterBuffer println() {
            handleNewline();
            pendingNewline = true;
            return this;
        }

        @Override
        public void close() {
            printer.println(builder.toString());
            builder.setLength(0);
            pendingNewline = false;
        }
    }
}
