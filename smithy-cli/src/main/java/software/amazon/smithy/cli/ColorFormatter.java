/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
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
