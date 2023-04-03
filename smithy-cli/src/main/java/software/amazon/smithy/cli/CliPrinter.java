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

import java.io.Flushable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Handles text output of the CLI.
 */
public interface CliPrinter extends Appendable, Flushable {

    @Override
    CliPrinter append(char c);

    @Override
    default CliPrinter append(CharSequence csq) {
        return append(csq, 0, csq.length());
    }

    @Override
    CliPrinter append(CharSequence csq, int start, int end);

    /**
     * Prints text to the writer and appends a new line.
     *
     * @param text Text to print.
     */
    default CliPrinter println(String text) {
        return append(text + System.lineSeparator());
    }

    /**
     * Flushes any buffers in the printer.
     */
    default void flush() {}

    /**
     * Create a new CliPrinter from an OutputStream.
     *
     * @param stream OutputStream to write to.
     * @return Returns the created CliPrinter.
     */
    static CliPrinter fromOutputStream(OutputStream stream) {
        Charset charset = StandardCharsets.UTF_8;
        OutputStreamWriter writer = new OutputStreamWriter(stream, charset);

        return new CliPrinter() {
            @Override
            public CliPrinter append(char c) {
                try {
                    writer.append(c);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
                return this;
            }

            @Override
            public CliPrinter append(CharSequence csq) {
                try {
                    writer.append(csq);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
                return this;
            }

            @Override
            public CliPrinter append(CharSequence csq, int start, int end) {
                try {
                    writer.append(csq, start, end);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
                return this;
            }

            @Override
            public void flush() {
                try {
                    writer.flush();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        };
    }
}
