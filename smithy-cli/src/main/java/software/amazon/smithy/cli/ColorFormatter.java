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
import java.io.UncheckedIOException;

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
    default String style(String text, Style... styles) {
        if (!isColorEnabled()) {
            return text;
        } else {
            StringBuilder builder = new StringBuilder();
            style(builder, text, styles);
            return builder.toString();
        }
    }

    /**
     * Styles text using the given styles and writes it to an Appendable.
     *
     * @param appendable Where to write styled text.
     * @param text Text to write.
     * @param styles Styles to apply.
     */
    default void style(Appendable appendable, String text, Style... styles) {
        try {
            startStyle(appendable, styles);
            appendable.append(text);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            if (styles.length > 0) {
                endStyle(appendable);
            }
        }
    }

    /**
     * Print a styled line of text to the given {@code appendable}.
     *
     * @param appendable Where to write.
     * @param text Text to write.
     * @param styles Styles to apply.
     */
    default void println(Appendable appendable, String text, Style... styles) {
        style(appendable, text + System.lineSeparator(), styles);
    }

    /**
     * @return Returns true if this formatter supports color output.
     */
    boolean isColorEnabled();

    void startStyle(Appendable appendable, Style... style);

    void endStyle(Appendable appendable);
}
