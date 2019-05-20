/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tokenizes a .smithy formatted string into a list of tokens.
 *
 * <p>This lexer uses a regular expression to tokenize. Each token is defined
 * using a {@code TokenType} that contains partial regular expression used to
 * match the token. The order in which these tokens are defined is
 * significant. All of the tokens and their expressions are combined into a
 * single regular expressions that uses named captures. The name of each
 * capture is the name of the enum. Any unrecognized token is tokenized as
 * {@code ERROR}.
 *
 * <p>\r\n and \n within strings are normalized into just "\n". This removes
 * any element of surprise when using Smithy models with different operating
 * systems. \r and \r\n can be added to models within quoted strings using
 * unicode escapes or by escaping \r and \n.
 */
final class SmithyModelLexer implements Iterator<SmithyModelLexer.Token> {

    private static final Pattern COMPILED;
    private Token peeked;
    private int line = 1;
    private int lastLineOffset;
    private final Matcher matcher;

    SmithyModelLexer(String input) {
        // Normalize all new lines into \n.
        if (input.indexOf('\r') > -1) {
            input = input.replaceAll("\r\n?", "\n");
        }

        matcher = COMPILED.matcher(input);
    }

    /** Represents a parsed token type, in order of precedence. */
    enum TokenType {
        NEWLINE("\n"),
        WS("\\s+"),
        COMMENT("//[^\\n]*"),
        RETURN(Pattern.quote("->")),
        VERSION(Pattern.quote("$version")),
        UNQUOTED("[A-Za-z_][A-Za-z0-9_#$.-]*"),
        LPAREN(Pattern.quote("(")),
        RPAREN(Pattern.quote(")")),
        LBRACE(Pattern.quote("{")),
        RBRACE(Pattern.quote("}")),
        LBRACKET(Pattern.quote("[")),
        RBRACKET(Pattern.quote("]")),
        EQUAL("="),
        COLON(":"),
        COMMA(","),
        ANNOTATION("@[A-Za-z0-9.$#]+"),
        // Quoted strings support escaped quotes, escaped escapes, and escaped newlines.
        QUOTED("(\"\"\"(((?!\"\"\")|[^\\\\])*(?:\\\\(.|\n)((?!\"\"\")|[^\\\\])*)*)\"\"\")"
               + "|(\"([^\"\\\\]*(?:\\\\(.|\n)[^\"\\\\]*)*)\")"
               + "|('([^'\\\\]*(?:\\\\(.|\n)[^'\\\\]*)*)')"),
        NUMBER("-?(?:0|[1-9]\\d*)(?:\\.\\d+)?(?:[eE][+-]?\\d+)?"),
        ERROR(".");

        private final String pattern;

        TokenType(String pattern) {
            this.pattern = pattern;
        }

        @Override
        public String toString() {
            return super.toString() + "(" + pattern.replace("\\Q", "").replace("\\E", "") + ")";
        }
    }

    /** Represents a parsed token. */
    static final class Token {
        final TokenType type;
        final String lexeme;
        final String errorMessage;
        final int line;
        final int column;
        final int span;

        Token(TokenType type, String lexeme, int line, int column, int span, String errorMessage) {
            this.type = type;
            this.lexeme = lexeme;
            this.line = line;
            this.column = column;
            this.span = span;
            this.errorMessage = errorMessage;
        }

        @Override
        public String toString() {
            return errorMessage != null
                   ? String.format("ERROR(%s, %d:%d)", errorMessage, line, column)
                   : String.format("%s(%s, %d:%d)", type.name(), lexeme, line, column);
        }
    }

    static {
        // Compile the lexer tokens into a regular expression with capture
        // groups matching the enum value name.
        StringBuilder builder = new StringBuilder();
        for (TokenType token : TokenType.values()) {
            builder.append(String.format("|(?<%s>%s)", token.name(), token.pattern));
        }
        COMPILED = Pattern.compile(builder.substring(1));
    }

    /**
     * Peeks at the next available token without consuming it.
     *
     * @return Returns the next token or null if at the end.
     */
    Token peek() {
        if (peeked == null) {
            peeked = maybeGetNext();
        }

        return peeked;
    }

    @Override
    public boolean hasNext() {
        return peek() != null;
    }

    @Override
    public Token next() {
        Token next = maybeGetNext();
        if (next == null) {
            throw new NoSuchElementException();
        }
        return next;
    }

    /**
     * Gets the next token or returns null if there are no more tokens.
     *
     * <p>Drains and returns a previously peeked value if present.
     *
     * @return Returns the nullable next token.
     */
    private Token maybeGetNext() {
        if (peeked != null) {
            Token value = peeked;
            peeked = null;
            return value;
        }

        next_token:
        while (matcher.find()) {
            for (TokenType token : TokenType.values()) {
                if (matcher.group(token.name()) != null) {
                    String lexeme = matcher.group(token.name());
                    String errorMessage = null;
                    final int startingLineNumber = line;
                    final int column = 1 + (matcher.start() - lastLineOffset);
                    final int span = matcher.end() - matcher.start();

                    switch (token) {
                        case NEWLINE:
                            line++;
                            lastLineOffset = matcher.end();
                            // fallthru
                        case WS: // fallthru
                        case COMMENT:
                            // Try to grab the next token before returning.
                            continue next_token;
                        case QUOTED:
                            try {
                                // Quoted text can contain newlines, so track the offsets.
                                int lastNewline = lexeme.lastIndexOf('\n');
                                if (lastNewline != -1) {
                                    // Offset from the text position from start + the last new line (starting at 1).
                                    lastLineOffset = matcher.start() + lastNewline + 1;
                                }
                                lexeme = parseStringContents(lexeme);
                            } catch (IllegalArgumentException e) {
                                token = TokenType.ERROR;
                                errorMessage = e.getMessage();
                            }
                            break;
                        case ANNOTATION:
                            // Strip leading "@".
                            lexeme = lexeme.substring(1);
                            break;
                        default:
                            break;
                    }

                    return new Token(token, lexeme, startingLineNumber, column, span, errorMessage);
                }
            }
        }

        return null;
    }

    enum LexerState { NORMAL, AFTER_ESCAPE, UNICODE }

    /**
     * This method adds support for JSON escapes found in strings.
     *
     * @param lexeme Lexeme to parse escapes from.
     * @return Returns the raw, unescaped lexeme.
     * @throws IllegalArgumentException when escapes are invalid.
     */
    private String parseStringContents(String lexeme) {
        int offset = 1;

        // Format the text block and remove incidental whitespace.
        if (lexeme.startsWith("\"\"\"")) {
            lexeme = formatTextBlock(lexeme);
            offset = 0;
        }

        StringBuilder result = new StringBuilder(lexeme.length() - offset * 2);
        LexerState state = LexerState.NORMAL;
        int hexCount = 0;
        int unicode = 0;

        // Skip quotes from the start and end.
        for (int i = offset; i < lexeme.length() - offset; i++) {
            char c = lexeme.charAt(i);

            if (c == '\n') {
                line++;
            }

            switch (state) {
                case NORMAL:
                    if (c == '\\') {
                        state = LexerState.AFTER_ESCAPE;
                    } else {
                        result.append(c);
                    }
                    break;
                case AFTER_ESCAPE:
                    state = LexerState.NORMAL;
                    switch (c) {
                        case '"':
                            result.append('"');
                            continue;
                        case '\'':
                            result.append('\'');
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
                            state = LexerState.UNICODE;
                            break;
                        case '\n':
                            // Skip writing the escaped new line.
                            break;
                        default:
                            throw new IllegalArgumentException("Invalid escape found in string: `\\" + c + "`");
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
                        throw new IllegalArgumentException("Invalid unicode escape character: `" + c + "`");
                    }

                    if (++hexCount == 4) {
                        result.append((char) unicode);
                        hexCount = 0;
                        state = LexerState.NORMAL;
                    }
                    break;
                default:
                    throw new IllegalStateException("Unreachable");
            }
        }

        if (state == LexerState.UNICODE) {
            throw new IllegalArgumentException("Invalid unclosed unicode escape found in string");
        }

        return result.toString();
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
    private String formatTextBlock(String lexeme) {
        // Strip the leading and trailing delimiter.
        lexeme = lexeme.substring(3, lexeme.length() - 3);

        if (lexeme.isEmpty()) {
            throw new IllegalArgumentException("Invalid text block: text block is empty");
        } else if (lexeme.charAt(0) != '\n') {
            throw new IllegalArgumentException("Invalid text block: text block must start with a new line (LF)");
        }

        line++;
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
    private int computeLeadingWhitespace(String line, boolean isLastLine) {
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

        return endPosition > startPosition ? line.substring(startPosition, endPosition + 1) : null;
    }
}
