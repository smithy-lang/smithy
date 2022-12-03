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

/**
 * Handles text output of the CLI.
 */
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
}
