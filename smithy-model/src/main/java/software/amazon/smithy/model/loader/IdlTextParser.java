/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

/**
 * Parses IDL text and text blocks.
 */
final class IdlTextParser {

    private IdlTextParser() {}

    // quoted_text = DQUOTE *quoted_char DQUOTE
    static String parseQuotedString(IdlModelParser parser) {
        parser.expect('"');
        if (parser.peek() == '"') { // open and closed string.
            parser.skip();
            return ""; // ""
        } else { // "
            return parseQuotedTextAndTextBlock(parser, false);
        }
    }

    // Parses both quoted_text and text_block
    static String parseQuotedTextAndTextBlock(IdlModelParser parser, boolean triple) {
        int start = parser.position();

        while (!parser.eof()) {
            char next = parser.peek();
            if (next == '"' && (!triple || (parser.peek(1) == '"' && parser.peek(2) == '"'))) {
                // Found closing quotes of quoted_text and/or text_block
                break;
            }
            parser.skip();
            if (next == '\\') {
                parser.skip();
            }
        }

        // Strip the ending '"'.
        String result = parser.sliceFrom(start);
        parser.expect('"');

        if (triple) {
            parser.expect('"');
            parser.expect('"');
        }

        return parseStringContents(parser, result, triple);
    }

    enum State { NORMAL, AFTER_ESCAPE, UNICODE }

    private static String parseStringContents(IdlModelParser parser, String lexeme, boolean triple) {
        lexeme = normalizeLineEndings(lexeme);

        // Format the text block and remove incidental whitespace.
        if (triple) {
            lexeme = formatTextBlock(parser, lexeme);
        }

        StringBuilder result = new StringBuilder(lexeme.length());
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
                    } else if (isValidNormalCharacter(c, triple)) {
                        result.append(c);
                    } else {
                        throw parser.syntax("Invalid character: `" + c + "`");
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
                            throw parser.syntax("Invalid escape found in string: `\\" + c + "`");
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
                        throw parser.syntax("Invalid unicode escape character: `" + c + "`");
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
            throw parser.syntax("Invalid unclosed unicode escape found in string");
        }

        return result.toString();
    }

    // New lines in strings are normalized from CR (u000D) and CRLF (u000Du000A) to
    // LF (u000A). This ensures that strings defined in a Smithy model are equivalent
    // across platforms. If a literal \r is desired, it can be added a string value
    // using the Unicode escape \\u000d.
    private static String normalizeLineEndings(String lexeme) {
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

        return builder.toString();
    }

    // A slightly more efficient way than String#contains to check if a string
    // contains a carriage return.
    private static boolean containsCarriageReturn(String lexeme) {
        for (int i = 0; i < lexeme.length(); i++) {
            if (lexeme.charAt(i) == '\r') {
                return true;
            }
        }
        return false;
    }

    /**
     * Formats a text block by removing leading incidental whitespace.
     *
     * <p>Text block formatting occurs before expanding escapes.
     *
     * @param lexeme Lexeme to format.
     * @return Returns the formatted textblock.
     * @throws IllegalArgumentException if the block does not start with \n or is empty.
     */
    private static String formatTextBlock(IdlModelParser parser, String lexeme) {
        if (lexeme.isEmpty()) {
            throw parser.syntax("Invalid text block: text block is empty");
        } else if (lexeme.charAt(0) != '\n') {
            throw parser.syntax("Invalid text block: text block must start with a new line (LF)");
        }

        StringBuilder buffer = new StringBuilder();
        int longestPadding = Integer.MAX_VALUE;
        String[] lines = lexeme.split("\n", -1);

        for (int i = 1; i < lines.length; i++) {
            int padding = computeLeadingWhitespace(lines[i], i == lines.length - 1);
            if (padding > -1 && padding < longestPadding) {
                longestPadding = padding;
            }
        }

        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];

            if (!line.isEmpty()) {
                String formattedLine = createTextBlockLine(line, longestPadding);
                if (formattedLine != null) {
                    buffer.append(formattedLine);
                }
            }

            if (i < lines.length - 1) {
                buffer.append('\n');
            }
        }

        return buffer.toString();
    }

    /**
     * Computes the number of leading whitespace characters in a string.
     *
     * <p>This method returns -1 if the string is empty or if the string
     * contains only whitespace. When determining the whitespace of the
     * last line, the length of the line is returned if the entire
     * line consists of only spaces. This ensures that the whitespace
     * of the last line can be used to influence the indentation of the
     * content as a whole (a significant trailing line policy).
     *
     * @param line Line to search for whitespace.
     * @param isLastLine Whether or not this is the last line.
     * @return Returns the last whitespace index starting at 0 or -1.
     */
    private static int computeLeadingWhitespace(String line, boolean isLastLine) {
        if (line.isEmpty()) {
            return -1;
        }

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
    private static String createTextBlockLine(String line, int longestPadding) {
        int startPosition = Math.min(longestPadding, line.length());
        int endPosition = line.length() - 1;

        // Strip off trailing whitespace from each line.
        while (endPosition > 0 && line.charAt(endPosition) == ' ') {
            endPosition--;
        }

        return endPosition >= startPosition ? line.substring(startPosition, endPosition + 1) : null;
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
