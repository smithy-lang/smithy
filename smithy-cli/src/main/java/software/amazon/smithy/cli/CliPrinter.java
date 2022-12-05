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

import java.io.BufferedWriter;
import java.io.Flushable;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Handles text output of the CLI.
 */
@FunctionalInterface
public interface CliPrinter extends Flushable {

    /**
     * Create a new CliPrinter from a PrintWriter.
     *
     * @param ansi Ansi color settings to use.
     * @param printWriter PrintWriter to write to.
     * @return Returns the created CliPrinter.
     */
    static CliPrinter fromPrintWriter(Ansi ansi, PrintWriter printWriter) {
        return new CliPrinter() {
            @Override
            public void println(String text) {
                printWriter.println(text);
            }

            @Override
            public Ansi ansi() {
                return ansi;
            }

            @Override
            public void flush() {
                printWriter.flush();
            }
        };
    }

    /**
     * Create a new CliPrinter from an OutputStream.
     *
     * @param ansi Ansi color settings to use.
     * @param stream OutputStream to write to.
     * @return Returns the created CliPrinter.
     */
    static CliPrinter fromOutputStream(Ansi ansi, OutputStream stream) {
        Charset charset = StandardCharsets.UTF_8;
        PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(stream, charset)), false);
        return fromPrintWriter(ansi, writer);
    }

    /**
     * Prints text to the writer and appends a new line.
     *
     * @param text Text to print.
     */
    void println(String text);

    /**
     * Prints a styled line of text using ANSI colors.
     *
     * @param text Text to style and write.
     * @param styles Styles to apply.
     */
    default void println(String text, Style... styles) {
        println(ansi().style(text, styles));
    }

    /**
     * Flushes any buffers in the printer.
     */
    default void flush() {}

    /**
     * Gets the ANSI color style setting used by the printer.
     *
     * @return Returns the ANSI color style.
     */
    default Ansi ansi() {
        return Ansi.AUTO;
    }

    /**
     * Creates a {@link Buffer} used to build up a long string of text.
     *
     * @return Returns the buffer. Call {@link Buffer#close()} or use try-with-resources to write to the printer.
     */
    default Buffer buffer() {
        return new Buffer(this);
    }

    /**
     * A buffer associated with a {@link CliPrinter} used to build up a string of ANSI color stylized text.
     *
     * <p>This class is not thread safe; it's a convenient way to build up a long string of text that uses ANSI styles
     * before ultimately calling {@link #close()}, which writes it to the attached printer.
     */
    final class Buffer implements Appendable, AutoCloseable {
        private final CliPrinter printer;
        private final StringBuilder builder = new StringBuilder();

        private Buffer(CliPrinter printer) {
            this.printer = printer;
        }

        @Override
        public String toString() {
            return builder.toString();
        }

        @Override
        public Buffer append(CharSequence csq) {
            builder.append(csq);
            return this;
        }

        @Override
        public Buffer append(CharSequence csq, int start, int end) {
            builder.append(csq, start, end);
            return this;
        }

        @Override
        public Buffer append(char c) {
            builder.append(c);
            return this;
        }

        /**
         * Writes styled text to the builder using the CliPrinter's Ansi settings.
         *
         * @param text Text to write.
         * @param styles Styles to apply to the text.
         * @return Returns self.
         */
        public Buffer print(String text, Style... styles) {
            printer.ansi().style(this, text, styles);
            return this;
        }

        /**
         * Prints a line of styled text to the buffer.
         *
         * @param text Text to print.
         * @param styles Styles to apply.
         * @return Returns self.
         */
        public Buffer println(String text, Style... styles) {
            return print(text, styles).println();
        }

        /**
         * Writes a system-dependent new line.
         *
         * @return Returns the buffer.
         */
        public Buffer println() {
            return append(System.lineSeparator());
        }

        @Override
        public void close() {
            printer.println(builder.toString());
            builder.setLength(0);
        }
    }
}
