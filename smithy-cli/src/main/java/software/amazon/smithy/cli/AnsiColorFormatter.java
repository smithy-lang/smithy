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
import java.util.function.Consumer;

/**
 * Styles text using ANSI color codes.
 */
public enum AnsiColorFormatter implements ColorFormatter {

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

        @Override
        public boolean isColorEnabled() {
            return false;
        }

        @Override
        public <T extends Appendable> void style(T appendable, Consumer<T> consumer, Style... styles) {
            consumer.accept(appendable);
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
                startStyle(appendable, styles);
                appendable.append(text);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } finally {
                closeStyle(appendable, styles);
            }
        }

        @Override
        public boolean isColorEnabled() {
            return true;
        }

        @Override
        public <T extends Appendable> void style(T appendable, Consumer<T> consumer, Style... styles) {
            try {
                startStyle(appendable, styles);
                consumer.accept(appendable);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } finally {
                closeStyle(appendable, styles);
            }
        }

        private void startStyle(Appendable appendable, Style... styles) throws IOException {
            if (styles.length > 0) {
                appendable.append("\033[");
                boolean isAfterFirst = false;
                for (Style style : styles) {
                    if (isAfterFirst) {
                        appendable.append(';');
                    }
                    appendable.append(style.getAnsiColorCode());
                    isAfterFirst = true;
                }
                appendable.append('m');
            }
        }

        private void closeStyle(Appendable appendable, Style... styles) {
            try {
                if (styles.length > 0) {
                    appendable.append("\033[0m");
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    },

    /**
     * Writes using ANSI colors if it detects that the environment supports color.
     */
    AUTO {
        private final AnsiColorFormatter delegate = AnsiColorFormatter.detect();

        @Override
        public String style(String text, Style... styles) {
            return delegate.style(text, styles);
        }

        @Override
        public void style(Appendable appendable, String text, Style... styles) {
            delegate.style(appendable, text, styles);
        }

        @Override
        public boolean isColorEnabled() {
            return delegate.isColorEnabled();
        }

        @Override
        public <T extends Appendable> void style(T appendable, Consumer<T> consumer, Style... styles) {
            delegate.style(appendable, consumer, styles);
        }
    };

    /**
     * Detects if ANSI colors are supported and returns the appropriate Ansi enum variant.
     *
     * <p>This method differs from using the {@link AnsiColorFormatter#AUTO} variant directly because it will
     * detect any changes to the environment that might enable or disable colors.
     *
     * @return Returns the detected ANSI color enum variant.
     */
    public static AnsiColorFormatter detect() {
        return isAnsiEnabled() ? FORCE_COLOR : NO_COLOR;
    }

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
