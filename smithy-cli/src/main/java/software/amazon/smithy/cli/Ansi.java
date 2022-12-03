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
import java.util.Objects;

/**
 * Styles text using ANSI color codes.
 */
public enum Ansi {

    /**
     * Writes using ANSI colors if it detects that the environment supports color.
     */
    AUTO {
        private final Ansi delegate = Ansi.detect();

        @Override
        public String style(String text, Style... styles) {
            return delegate.style(text, styles);
        }

        @Override
        public void style(Appendable appendable, String text, Style... styles) {
            delegate.style(appendable, text, styles);
        }
    },

    /**
     * Does not write any color.
     */
    NO_COLOR {
        @Override
        public String style(String text, Style... styles) {
            return text;
        }

        @Override
        public void style(Appendable appendable, String text, Style... styles) {
            try {
                appendable.append(text);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    },

    /**
     * Writes with ANSI colors.
     */
    FORCE_COLOR {
        @Override
        public String style(String text, Style... styles) {
            StringBuilder builder = new StringBuilder();
            style(builder, text, styles);
            return builder.toString();
        }

        @Override
        public void style(Appendable appendable, String text, Style... styles) {
            try {
                appendable.append("\033[");
                boolean isAfterFirst = false;
                for (Style style : styles) {
                    if (isAfterFirst) {
                        appendable.append(';');
                    }
                    appendable.append(style.toString());
                    isAfterFirst = true;
                }
                appendable.append('m');
                appendable.append(text);
                appendable.append("\033[0m");
            } catch (IOException e) {
                throw new CliError("Error writing output", 2, e);
            }
        }
    };

    /**
     * Detects if ANSI colors are supported and returns the appropriate Ansi enum variant.
     *
     * <p>This method differs from using the {@link Ansi#AUTO} variant directly because it will detect any changes
     * to the environment that might enable or disable colors.
     *
     * @return Returns the detected ANSI color enum variant.
     */
    public static Ansi detect() {
        return isAnsiEnabled() ? FORCE_COLOR : NO_COLOR;
    }

    /**
     * Styles text using ANSI color codes.
     *
     * @param text Text to style.
     * @param styles Styles to apply.
     * @return Returns the styled text.
     */
    public abstract String style(String text, Style... styles);

    /**
     * Styles text using ANSI color codes and writes it to an Appendable.
     *
     * @param appendable Where to write styled text.
     * @param text Text to write.
     * @param styles Styles to apply.
     */
    public abstract void style(Appendable appendable, String text, Style... styles);

    private static boolean isAnsiEnabled() {
        if (EnvironmentVariable.FORCE_COLOR.isSet()) {
            return true;
        }

        // Disable colors if NO_COLOR is set to anything.
        if (EnvironmentVariable.NO_COLOR.isSet()) {
            return false;
        }

        String term = EnvironmentVariable.TERM.get();

        // If term is set to "dumb", then don't use colors.
        if (Objects.equals(term, "dumb")) {
            return false;
        }

        // If TERM isn't set at all and Windows is detected, then don't use colors.
        if (term == null && System.getProperty("os.name").contains("win")) {
            return false;
        }

        // Disable colors if no console is associated.
        return System.console() != null;
    }
}
