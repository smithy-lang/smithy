/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.model.loader;

import java.util.ArrayList;
import java.util.List;

final class IdlStringLexer {

    private IdlStringLexer() { }

    private enum State { NORMAL, AFTER_ESCAPE, UNICODE }

    // Use the original lexeme of a string when possible, but creates a new string when escapes are used.
    private static final class StringBuilderProxy {
        private final CharSequence lexeme;
        private StringBuilder builder;
        private int position = 0;

        StringBuilderProxy(CharSequence lexeme) {
            this.lexeme = lexeme;
        }

        // Called when a string uses escapes, and begins buffering to a newly created string.
        void capture() {
            if (builder == null) {
                builder = new StringBuilder(lexeme.length());
                builder.append(lexeme, 0, position);
            }
        }

        void append(char c) {
            if (builder != null) {
                builder.append(c);
            } else {
                position++;
            }
        }

        CharSequence getResult() {
            return builder == null ? lexeme : builder;
        }
    }

    static CharSequence scanStringContents(CharSequence lexeme, boolean scanningTextBlock) {
        lexeme = normalizeLineEndings(lexeme);

        // Format the text block and remove incidental whitespace.
        if (scanningTextBlock) {
            lexeme = formatTextBlock(lexeme);
        }

        //StringBuilder result = new StringBuilder(lexeme.length());
        StringBuilderProxy result = new StringBuilderProxy(lexeme);

        State state = State.NORMAL;
        int hexCount = 0;
        int unicode = 0;

        // Skip quotes from the start and end.
        for (int i = 0; i < lexeme.length(); i++) {
            char c = lexeme.charAt(i);
            switch (state) {
                case NORMAL:
                    if (c == '\\') {
                        state = State.AFTER_ESCAPE;
                        result.capture();
                    } else if (isValidNormalCharacter(c, scanningTextBlock)) {
                        result.append(c);
                    } else {
                        throw new RuntimeException("Invalid string character: `" + c + "`");
                    }
                    break;
                case AFTER_ESCAPE:
                    state = State.NORMAL;
                    switch (c) {
                        case '"':
                            result.append('"');
                            continue;
                        case '\\':
                            result.append('\\');
                            break;
                        case '/':
                            result.append('/');
                            break;
                        case 'b':
                            result.append('\b');
                            break;
                        case 'f':
                            result.append('\f');
                            break;
                        case 'n':
                            result.append('\n');
                            break;
                        case 'r':
                            result.append('\r');
                            break;
                        case 't':
                            result.append('\t');
                            break;
                        case 'u':
                            state = State.UNICODE;
                            break;
                        case '\n':
                            // Skip writing the escaped new line.
                            break;
                        default:
                            throw new RuntimeException("Invalid escape found in string: `\\" + c + "`");
                    }
                    break;
                case UNICODE:
                    if (c >= '0' && c <= '9') {
                        unicode = (unicode << 4) | (c - '0');
                    } else if (c >= 'a' && c <= 'f') {
                        unicode = (unicode << 4) | (10 + c - 'a');
                    } else if (c >= 'A' && c <= 'F') {
                        unicode = (unicode << 4) | (10 + c - 'A');
                    } else {
                        throw new RuntimeException("Invalid unicode escape character: `" + c + "`");
                    }

                    if (++hexCount == 4) {
                        result.append((char) unicode);
                        hexCount = 0;
                        state = State.NORMAL;
                    }
                    break;
                default:
                    throw new IllegalStateException("Unreachable");
            }
        }

        if (state == State.UNICODE) {
            throw new RuntimeException("Invalid unclosed unicode escape found in string");
        }

        return result.getResult();
    }

    // New lines in strings are normalized from CR (u000D) and CRLF (u000Du000A) to
    // LF (u000A). This ensures that strings defined in a Smithy model are equivalent
    // across platforms. If a literal \r is desired, it can be added a string value
    // using the Unicode escape (\)u000d.
    private static CharSequence normalizeLineEndings(CharSequence lexeme) {
        if (!containsCarriageReturn(lexeme)) {
            return lexeme;
        }

        StringBuilder builder = new StringBuilder(lexeme.length());
        for (int i = 0; i < lexeme.length(); i++) {
            char c = lexeme.charAt(i);
            if (c != '\r') {
                builder.append(c);
            } else {
                // Convert "\r\n" to "\n".
                if (i < lexeme.length() - 1 && lexeme.charAt(i + 1) == '\n') {
                    i++;
                }
                builder.append('\n');
            }
        }

        return builder;
    }

    private static boolean containsCarriageReturn(CharSequence lexeme) {
        for (int i = 0; i < lexeme.length(); i++) {
            if (lexeme.charAt(i) == '\r') {
                return true;
            }
        }
        return false;
    }

    private static CharSequence formatTextBlock(CharSequence lexeme) {
        if (lexeme.length() == 0) {
            throw new RuntimeException("Text block is empty");
        } else if (lexeme.charAt(0) != '\n') {
            throw new RuntimeException("Text block must start with a new line");
        }

        StringBuilder buffer = new StringBuilder();
        int longestPadding = Integer.MAX_VALUE;
        List<CharSequence> lines = lines(lexeme);

        for (int i = 1; i < lines.size(); i++) {
            int padding = computeLeadingWhitespace(lines.get(i), i == lines.size() - 1);
            if (padding > -1 && padding < longestPadding) {
                longestPadding = padding;
            }
        }

        for (int i = 1; i < lines.size(); i++) {
            CharSequence line = lines.get(i);

            if (line.length() > 0) {
                CharSequence formattedLine = createTextBlockLine(line, longestPadding);
                if (formattedLine != null) {
                    buffer.append(formattedLine);
                }
            }

            if (i < lines.size() - 1) {
                buffer.append('\n');
            }
        }

        return buffer;
    }

    private static List<CharSequence> lines(final CharSequence text) {
        List<CharSequence> lines = new ArrayList<>();
        int mark = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                lines.add(text.subSequence(mark, i));
                mark = i + 1;
            }
        }
        lines.add(text.subSequence(mark, text.length()));
        return lines;
    }

    /**
     * Computes the number of leading whitespace characters in a string.
     *
     * <p>This method returns -1 if the string contains only whitespace. When determining the whitespace of the
     * last line, the length of the line is returned if the entire line consists of only spaces. This ensures that
     * the whitespace of the last line can be used to influence the indentation of the content as a whole
     * (a significant trailing line policy).
     *
     * @param line Line to search for whitespace.
     * @param isLastLine Whether or not this is the last line.
     * @return Returns the last whitespace index starting at 0 or -1.
     */
    private static int computeLeadingWhitespace(CharSequence line, boolean isLastLine) {
        for (int offset = 0; offset < line.length(); offset++) {
            if (line.charAt(offset) != ' ') {
                return offset;
            }
        }

        return isLastLine ? line.length() : -1;
    }

    /**
     * Creates a line for a text block.
     *
     * <p>Leading padding and trailing whitespace are removed from each line.
     * This method will not fail if a line is not longer than the leading
     * whitespace lines to remove (for example, a mixed number of blank lines
     * consisting of only whitespace).
     *
     * @param line The line to format.
     * @param longestPadding The leading whitespace to remove.
     * @return Returns the line or null if no line is emitted.
     */
    private static CharSequence createTextBlockLine(CharSequence line, int longestPadding) {
        int startPosition = Math.min(longestPadding, line.length());
        int endPosition = line.length() - 1;

        // Strip off trailing whitespace from each line.
        while (endPosition > 0 && line.charAt(endPosition) == ' ') {
            endPosition--;
        }

        return endPosition >= startPosition ? line.subSequence(startPosition, endPosition + 1) : null;
    }

    private static boolean isValidNormalCharacter(char c, boolean isTextBlock) {
        // Valid normal characters are the unescaped characters defined in the
        // QuotedChar grammar:
        // https://smithy.io/2.0/spec/idl.html#grammar-token-smithy-QuotedChar
        return c == '\t'
               || c == '\n'
               || c == '\r'
               || (c >= 0x20 && c <= 0x21) // space - "!"
               || (isTextBlock && c == 0x22) // DQUOTE is allowed in text_block
               || (c >= 0x23 && c <= 0x5b) // "#" - "["
               || c >= 0x5d; // "]"+
    }
}
