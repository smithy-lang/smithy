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
     * Prints text to the writer.
     *
     * @param text Text to write.
     */
    void print(String text);

    /**
     * Prints text to the writer and appends a new line.
     *
     * @param text Text to print.
     */
    default void println(String text) {
        print(text + System.lineSeparator());
    }

    /**
     * Flushes any buffers in the printer.
     */
    default void flush() {}

    /**
     * Create a new CliPrinter from a PrintWriter.
     *
     * @param printWriter PrintWriter to write to.
     * @return Returns the created CliPrinter.
     */
    static CliPrinter fromPrintWriter(PrintWriter printWriter) {
        return new CliPrinter() {
            @Override
            public void println(String text) {
                printWriter.println(text);
            }

            @Override
            public void print(String text) {
                printWriter.print(text);
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
     * @param stream OutputStream to write to.
     * @return Returns the created CliPrinter.
     */
    static CliPrinter fromOutputStream(OutputStream stream) {
        Charset charset = StandardCharsets.UTF_8;
        PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(stream, charset)), false);
        return fromPrintWriter(writer);
    }
}
