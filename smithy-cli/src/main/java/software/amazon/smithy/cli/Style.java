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

import java.util.function.IntConsumer;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Parameters used to change the ANSI public style of text.
 */
@SmithyUnstableApi
@FunctionalInterface
public interface Style {

    Style BOLD = new SingularCode(1);
    Style FAINT = new SingularCode(2);
    Style ITALIC = new SingularCode(3);
    Style UNDERLINE = new SingularCode(4);

    Style BLACK = new SingularCode(30);
    Style RED = new SingularCode(31);
    Style GREEN = new SingularCode(32);
    Style YELLOW = new SingularCode(33);
    Style BLUE = new SingularCode(34);
    Style MAGENTA = new SingularCode(35);
    Style CYAN = new SingularCode(36);
    Style WHITE = new SingularCode(37);

    Style BRIGHT_BLACK = new SingularCode(90);
    Style BRIGHT_RED = new SingularCode(91);
    Style BRIGHT_GREEN = new SingularCode(92);
    Style BRIGHT_YELLOW = new SingularCode(93);
    Style BRIGHT_BLUE = new SingularCode(94);
    Style BRIGHT_MAGENTA = new SingularCode(95);
    Style BRIGHT_CYAN = new SingularCode(96);
    Style BRIGHT_WHITE = new SingularCode(97);

    Style BG_BLACK = new SingularCode(40);
    Style BG_RED = new SingularCode(41);
    Style BG_GREEN = new SingularCode(42);
    Style BG_YELLOW = new SingularCode(43);
    Style BG_BLUE = new SingularCode(44);
    Style BG_MAGENTA = new SingularCode(45);
    Style BG_CYAN = new SingularCode(46);
    Style BG_WHITE = new SingularCode(47);

    Style BG_BRIGHT_BLACK = new SingularCode(100);
    Style BG_BRIGHT_RED = new SingularCode(101);
    Style BG_BRIGHT_GREEN = new SingularCode(102);
    Style BG_BRIGHT_YELLOW = new SingularCode(103);
    Style BG_BRIGHT_BLUE = new SingularCode(104);
    Style BG_BRIGHT_MAGENTA = new SingularCode(105);
    Style BG_BRIGHT_CYAN = new SingularCode(106);
    Style BG_BRIGHT_WHITE = new SingularCode(107);

    /**
     * Pushes one or more ANSI color codes to the consumer.
     *
     * <p>Most implementations will push a single code, but multiple
     * codes are needed to do things like use 8-bit colors
     * (e.g., 38+5+206 to make pink foreground text).
     *
     * @param codeConsumer Consumer to push integers to.
     */
    void pushCodes(IntConsumer codeConsumer);

    /**
     * Formats the given text with ANSI escapes.
     *
     * <p>Each {@code styles} is one or more ANSI escape codes in the format of
     * "1", "38;5;206" to create an 8-bit color, etc.
     *
     * @param text Text to format.
     * @param styles Styles to apply.
     * @return Returns the formatted text, and then resets the formatting.
     * @see <a href="https://man7.org/linux/man-pages/man4/console_codes.4.html">ANSI console codes</a>
     */
    static String format(String text, Style... styles) {
        StringBuilder result = new StringBuilder("\033[");
        IntConsumer consumer = result::append;
        boolean isAfterFirst = false;

        for (Style style : styles) {
            if (isAfterFirst) {
                result.append(';');
            }
            style.pushCodes(consumer);
            isAfterFirst = true;
        }

        result.append('m');
        result.append(text);
        result.append("\033[0m");
        return result.toString();
    }

    /**
     * A simple implementation of {@code Style} that pushes a single code.
     */
    final class SingularCode implements Style {
        private final int code;

        public SingularCode(int code) {
            this.code = code;
        }

        @Override
        public void pushCodes(IntConsumer codeConsumer) {
            codeConsumer.accept(code);
        }
    }
}
