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

import java.io.IOException;
import java.util.function.Consumer;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Handles text output of the CLI.
 */
@SmithyUnstableApi
public interface CliPrinter extends Appendable {

    /**
     * Prints text to the writer.
     *
     * @param text Text to print.
     */
    default void print(String text) {
        try {
            append(text);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Prints text to the writer using a specific color.
     *
     * @param text Text to print.
     */
    default void print(Color color, String text) {
        print(color.format(text));
    }

    /**
     * Prints text to the writer and appends a new line.
     *
     * @param text Text to print.
     */
    default void println(String text) {
        print(text + System.lineSeparator());
    }

    /**
     * Prints text to the writer using a specific color followed by a new line.
     *
     * @param text Text to print.
     */
    default void println(Color color, String text) {
        print(color, text + System.lineSeparator());
    }

    /**
     * Creates a CliPrinter from a Consumer that accepts a CharSequence.
     *
     * @param consumer Consumer to wrap.
     * @return Returns the created CliPrinter.
     */
    static CliPrinter fromConsumer(Consumer<CharSequence> consumer) {
        return new CliPrinter() {
            @Override
            public Appendable append(CharSequence csq) {
                consumer.accept(csq);
                return this;
            }

            @Override
            public Appendable append(CharSequence csq, int start, int end) {
                consumer.accept(csq.subSequence(start, end));
                return this;
            }

            @Override
            public Appendable append(char c) {
                return append(Character.toString(c));
            }
        };
    }
}
