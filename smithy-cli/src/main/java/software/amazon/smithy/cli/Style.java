/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.cli;

import java.util.StringJoiner;

/**
 * Colors and styles for use with {@link AnsiColorFormatter} or {@link CliPrinter}.
 *
 * <p>ANSI codes returned by this interface should not include the opening {@code \033[}, the final {@code ;m} when
 * opening styles, or the closing {@code [0m}. These escapes are added automatically when multiple styles are
 * applied.
 */
public interface Style {

    Style BOLD = new Constant("1");
    Style FAINT = new Constant("2");
    Style ITALIC = new Constant("3");
    Style UNDERLINE = new Constant("4");

    Style BLACK = new Constant("30");
    Style RED = new Constant("31");
    Style GREEN = new Constant("32");
    Style YELLOW = new Constant("33");
    Style BLUE = new Constant("34");
    Style MAGENTA = new Constant("35");
    Style CYAN = new Constant("36");
    Style WHITE = new Constant("37");

    Style BRIGHT_BLACK = new Constant("90");
    Style BRIGHT_RED = new Constant("91");
    Style BRIGHT_GREEN = new Constant("92");
    Style BRIGHT_YELLOW = new Constant("93");
    Style BRIGHT_BLUE = new Constant("94");
    Style BRIGHT_MAGENTA = new Constant("95");
    Style BRIGHT_CYAN = new Constant("96");
    Style BRIGHT_WHITE = new Constant("97");

    Style BG_BLACK = new Constant("40");
    Style BG_RED = new Constant("41");
    Style BG_GREEN = new Constant("42");
    Style BG_YELLOW = new Constant("43");
    Style BG_BLUE = new Constant("44");
    Style BG_MAGENTA = new Constant("45");
    Style BG_CYAN = new Constant("46");
    Style BG_WHITE = new Constant("47");

    Style BG_BRIGHT_BLACK = new Constant("100");
    Style BG_BRIGHT_RED = new Constant("101");
    Style BG_BRIGHT_GREEN = new Constant("102");
    Style BG_BRIGHT_YELLOW = new Constant("103");
    Style BG_BRIGHT_BLUE = new Constant("104");
    Style BG_BRIGHT_MAGENTA = new Constant("105");
    Style BG_BRIGHT_CYAN = new Constant("106");
    Style BG_BRIGHT_WHITE = new Constant("107");

    String getAnsiColorCode();

    static Style of(Style... styles) {
        if (styles.length == 1) {
            return styles[0];
        } else {
            return new Multi(styles);
        }
    }

    final class Constant implements Style {
        private final String ansiColorCode;

        public Constant(String ansiColorCode) {
            this.ansiColorCode = ansiColorCode;
        }

        @Override
        public String getAnsiColorCode() {
            return ansiColorCode;
        }
    }

    final class Multi implements Style {
        private final String ansiStyle;

        public Multi(Style... styles) {
            if (styles.length == 1) {
                ansiStyle = styles[0].getAnsiColorCode();
            } else {
                StringJoiner joiner = new StringJoiner(";");
                for (Style style : styles) {
                    joiner.add(style.getAnsiColorCode());
                }
                ansiStyle = joiner.toString();
            }
        }

        @Override
        public String getAnsiColorCode() {
            return ansiStyle;
        }
    }
}
