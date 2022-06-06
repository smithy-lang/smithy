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
}
