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

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Predicate;
import java.util.function.Supplier;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.utils.IoUtils;
import software.amazon.smithy.utils.StringUtils;

/**
 * Tokenizes a .smithy formatted string into a list of tokens.
 *
 * <p>This lexer does not use a regular expression based lexer because:
 *
 * <ul>
 *     <li>A regex lexer would require many nested captures with
 *     alternation which can lead to stack overflows while evaluating
 *     large inputs with the compiled Pattern.</li>
 *     <li>This style of scanner is easier to reason about, particularly
 *     when it comes to deciding the location of tokens and the handling
 *     of quoted strings.</li>
 * </ul>
 *
 * <p>{@code \r\n} and {@code \n} within strings are normalized into just
 * "\n". This removes any element of surprise when using Smithy models with
 * different operating systems. {@code \r} and {@code \r\n} can be added to
 * models within quoted strings using unicode escapes or by escaping
 * {@code \r} and {@code \n}.
 */
final class SmithyModelLexer implements Iterator<SmithyModelLexer.Token>, AutoCloseable {

    private final String filename;
    private final String input;
    private Token peeked;
    private int line = 1;
    private int column = 1;

    // The position is incremented to 0 in the first call to consume()
    private int position = -1;

    SmithyModelLexer(String filename, String input) {
        this.filename = filename;

        // Normalize all new lines into \n.
        if (input.indexOf('\r') > -1) {
            input = input.replaceAll("\r\n?", "\n");
        }

        this.input = input;
    }

    SmithyModelLexer(String filename, InputStream input) {
        // TODO: convert this lexer to just using a buffered reader.
        this(filename, IoUtils.toUtf8String(input));
    }

    enum TokenType {
        DOC,
        RETURN,
        CONTROL,
        UNQUOTED,
        LPAREN,
        RPAREN,
        LBRACE,
        RBRACE,
        LBRACKET,
        RBRACKET,
        EQUAL,
        COLON,
        COMMA,
        ANNOTATION,
        QUOTED,
        NUMBER,
        ERROR
    }

    /** Represents a parsed token. */
    static final class Token implements FromSourceLocation {
        final String filename;
        final TokenType type;
        final String lexeme;
        final String errorMessage;
        final int line;
        final int column;
        final int span;

        Token(String filename, TokenType type, String lexeme, int line, int column, int span, String errorMessage) {
            this.filename = filename;
            this.type = type;
            this.lexeme = lexeme;
            this.line = line;
            this.column = column;
            this.span = span;
            this.errorMessage = errorMessage;
        }

        @Override
        public String toString() {
            return !StringUtils.isBlank(errorMessage)
                   ? String.format("%s(%s, %d:%d)", type.name(), errorMessage, line, column)
                   : String.format("%s(%s, %d:%d)", type.name(), lexeme, line, column);
        }

        @Override
        public SourceLocation getSourceLocation() {
            return new SourceLocation(filename, line, column);
        }

        public String getDocContents() {
            if (type != TokenType.DOC) {
                throw new IllegalStateException("Not a doc comment token");
            }

            // Strip "///" and a leading space if present.
            return lexeme.startsWith("/// ")
                   ? lexeme.substring(4)
                   : lexeme.substring(3);
        }
    }

    @Override
    public void close() throws IOException {
        // TODO: convert to using the buffered reader directly.
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

        while (position < input.length() - 1) {
            Token token = parseCharacter(consume());
            if (token != null) {
                return token;
            }
        }

        return null;
    }

    private Token parseCharacter(char c) {
        switch (c) {
            case ' ':
            case '\t':
            case '\n':
                break;
            case '(':
                return new Token(filename, TokenType.LPAREN, "(", line, column - 1, 1, null);
            case ')':
                return new Token(filename, TokenType.RPAREN, ")", line, column - 1, 1, null);
            case '{':
                return new Token(filename, TokenType.LBRACE, "{", line, column - 1, 1, null);
            case '}':
                return new Token(filename, TokenType.RBRACE, "}", line, column - 1, 1, null);
            case '[':
                return new Token(filename, TokenType.LBRACKET, "[", line, column - 1, 1, null);
            case ']':
                return new Token(filename, TokenType.RBRACKET, "]", line, column - 1, 1, null);
            case '=':
                return new Token(filename, TokenType.EQUAL, "=", line, column - 1, 1, null);
            case ':':
                return new Token(filename, TokenType.COLON, ":", line, column - 1, 1, null);
            case ',':
                return new Token(filename, TokenType.COMMA, ",", line, column - 1, 1, null);
            case '/':
                // Parse double and triple comments.
                Token parsedToken = parseToken(TokenType.DOC, this::parseComment);
                if (parsedToken != null) {
                    return parsedToken;
                }
                break;
            case '$':
                // Parse control statements.
                return parseToken(TokenType.CONTROL, this::parseControl);
            case '@':
                // Parse traits.
                return parseToken(TokenType.ANNOTATION, this::parseTrait);
            case '"':
                // Parse double quoted strings.
                return parseToken(TokenType.QUOTED, this::parseQuotes);
            case '-':
                // Return "->" or negative number.
                if (peekChar() == '>') {
                    consume();
                    return new Token(filename, TokenType.RETURN, "->", line, column - 1, 2, null);
                }
                return parseToken(TokenType.NUMBER, this::parseNumber);
            default:
                if (isDigit(c)) {
                    return parseToken(TokenType.NUMBER, this::parseNumber);
                } else if (isIdentifierStart(c)) {
                    // Offset the position to not consume the current character before parsing the identifier.
                    return parseToken(TokenType.UNQUOTED, this::parseIdentifier);
                } else {
                    return new Token(filename, TokenType.ERROR, String.valueOf(c), line, column - 1, 1, null);
                }
        }

        return null;
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isIdentifierStart(char c) {
        return c == '_' || (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
    }

    private char consume() {
        if (position >= input.length() - 1) {
            throw new RuntimeException("Unexpected EOF");
        }

        char c = input.charAt(++position);

        if (c == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }

        return c;
    }

    private char expect(char... expectedChars) {
        char c = consume();
        for (char expected : expectedChars) {
            if (c == expected) {
                return c;
            }
        }

        throw new RuntimeException("Expected one of the following characters: " + Arrays.toString(expectedChars));
    }

    private char peekChar() {
        return peekChar(1);
    }

    private char peekChar(int offset) {
        return position + offset < input.length() ? input.charAt(position + offset) : '\0';
    }

    private Token parseToken(TokenType type, Supplier<String> parser) {
        int currentLine = line;
        int currentColumn = column - 1;
        int startPosition = position;

        try {
            String lexeme = parser.get();
            return lexeme == null
                   ? null
                   : new Token(filename, type, lexeme, currentLine, currentColumn, 1 + position - startPosition, null);
        } catch (RuntimeException e) {
            return new Token(filename, TokenType.ERROR, input.substring(startPosition, position),
                             currentLine, currentColumn, 1 + position - startPosition, e.getMessage());
        }
    }

    private String parseComment() {
        int startPosition = position;
        expect('/');
        boolean isDocComment = peekChar() == '/';
        consumeUntilNoLongerMatches(c -> c != '\n');
        return !isDocComment ? null : input.substring(startPosition, position + 1);
    }

    private int consumeUntilNoLongerMatches(Predicate<Character> predicate) {
        int startPosition = position;
        while (true) {
            char peekedChar = peekChar();
            if (peekedChar == '\0' || !predicate.test(peekedChar)) {
                break;
            }
            consume();
        }

        return position - startPosition;
    }

    private String parseControl() {
        consume(); // Skip '$'
        String identifier = parseIdentifier();
        consumeUntilNoLongerMatches(Character::isWhitespace);
        expect(':');
        return identifier;
    }

    private String parseTrait() {
        consume(); // Skip '@'
        return parseIdentifier();
    }

    private String parseIdentifier() {
        // Identifier: [A-Za-z_][A-Za-z0-9_#$.]*
        int startPosition = position;
        char start = input.charAt(position);

        if (!isIdentifierStart(start)) {
            throw new IllegalArgumentException("Expected [A-Za-z_]");
        }

        consumeUntilNoLongerMatches(c -> isIdentifierStart(c) || isDigit(c) || c == '#' || c == '$' || c == '.');

        return input.substring(startPosition, position + 1);
    }

    private String parseQuotes() {
        int startPosition = position;
        boolean triple = peekChar() == '"' && peekChar(2) == '"';

        if (triple) {
            consume();
            consume();
        }

        while (true) {
            char next = consume();
            if (next == '"' && (!triple || (peekChar() == '"' && peekChar(2) == '"'))) {
                if (triple) {
                    consume();
                    consume();
                }
                break;
            } else if (next == '\\') {
                consume();
            }
        }

        return parseStringContents(input.substring(startPosition, position + 1));
    }

    // -?(?:0|[1-9]\d*)(?:\.\d+)?(?:[eE][+-]?\d+)?
    private String parseNumber() {
        int startPosition = position;

        char current = input.charAt(position);

        // 0 must not be followed by a number.
        if (current == '0' && isDigit(peekChar())) {
            throw new IllegalArgumentException("Invalid number");
        }

        consumeUntilNoLongerMatches(this::isDigit);

        // Consume decimals.
        char peek = peekChar();
        if (peek == '.') {
            consume();
            if (consumeUntilNoLongerMatches(this::isDigit) == 0) {
                throw new IllegalArgumentException("Invalid number");
            }
        }

        // Consume scientific notation.
        peek = peekChar();
        if (peek == 'e' || peek == 'E') {
            consume();
            peek = peekChar();
            if (peek == '+' || peek == '-') {
                consume();
            }
            if (consumeUntilNoLongerMatches(this::isDigit) == 0) {
                throw new IllegalArgumentException("Invalid number");
            }
        }

        return input.substring(startPosition, position + 1);
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
