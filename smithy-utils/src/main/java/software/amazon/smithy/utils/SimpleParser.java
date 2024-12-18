/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.utils;

import java.nio.CharBuffer;
import java.util.Objects;
import java.util.function.IntPredicate;
import java.util.function.Predicate;

/**
 * A simple expression parser that can be extended to implement parsers
 * for small domain-specific languages.
 *
 * <p>This parser internally consumes characters of a {@link CharSequence} while tracking
 * the current 0-based position, 1-based line, and 1-based column.
 * Expectations can be made on the parser to require specific characters,
 * and when those expectations are not met, a syntax exception is thrown.
 */
public class SimpleParser {

    public static final char EOF = Character.MIN_VALUE;

    private final CharSequence input;
    private final int maxNestingLevel;
    private int position = 0;
    private int line = 1;
    private int column = 1;
    private int nestingLevel = 0;

    /**
     * Creates a new SimpleParser and sets the expression to parse.
     *
     * @param expression Expression to parser.
     */
    public SimpleParser(CharSequence expression) {
        this(expression, 0);
    }

    /**
     * Creates a new SimpleParser and sets the expression to parse.
     *
     * <p>By default, no maximum parsing level is enforced. Setting the
     * {@code maxParsingLevel} to 0 disables the enforcement of a
     * maximum parsing level.
     *
     * @param input Input to parse that must not be null.
     * @param maxNestingLevel The maximum allowed nesting level of the parser.
     */
    public SimpleParser(CharSequence input, int maxNestingLevel) {
        this.input = Objects.requireNonNull(input, "expression must not be null");
        this.maxNestingLevel = maxNestingLevel;

        if (maxNestingLevel < 0) {
            throw new IllegalArgumentException("maxNestingLevel must be >= 0");
        }
    }

    /**
     * Gets the input being parsed as a CharSequence.
     *
     * @return Returns the input being parsed.
     */
    public final CharSequence input() {
        return input;
    }

    @Deprecated
    public final String expression() {
        return input.toString();
    }

    /**
     * Gets the current 0-based position of the parser.
     *
     * @return Returns the parser character position.
     */
    public final int position() {
        return position;
    }

    /**
     * Gets the current 1-based line number of the parser.
     *
     * @return Returns the current line number.
     */
    public final int line() {
        return line;
    }

    /**
     * Gets the current 1-based column number of the parser.
     *
     * @return Returns the current column number.
     */
    public final int column() {
        return column;
    }

    public final void rewind(int position, int line, int column) {
        this.column = column;
        this.line = line;
        this.position = position;
    }

    /**
     * Checks if the parser has reached the end of the expression.
     *
     * @return Returns true if the parser has reached the end.
     */
    public final boolean eof() {
        return position >= input.length();
    }

    /**
     * Returns the current character of the expression, but does not consume it.
     *
     * @return Returns the peeked character.
     */
    public final char peek() {
        return peek(0);
    }

    /**
     * Returns the current character of the expression + {@code offset}
     * characters, but does not consume it.
     *
     * <p>If the end of the expression is reached or if the peeked offset is
     * calculated to be less than 0, {@link Character#MIN_VALUE} is returned
     * (that is, '\0').
     *
     * @param offset The number of characters to peek ahead (positive or negative).
     * @return Returns the peeked character.
     */
    public final char peek(int offset) {
        int target = position + offset;
        if (target >= input.length() || target < 0) {
            return EOF;
        }

        return input.charAt(target);
    }

    /**
     * Expects that the next character is the given character and consumes it.
     *
     * @param token The character to expect.
     * @return Returns the expected character.
     */
    public final char expect(char token) {
        if (peek() == token) {
            skip();
            return token;
        }

        throw syntax(String.format("Expected: '%s', but found '%s'", token, peekSingleCharForMessage()));
    }

    /**
     * Peeks the next character and returns [EOF] if the next character is past
     * the end of the expression.
     *
     * @return Returns the peeked next character.
     */
    public final String peekSingleCharForMessage() {
        char peek = peek();
        return peek == EOF ? "[EOF]" : String.valueOf(peek);
    }

    /**
     * Expects that the next character is one of a fixed set of possible characters.
     *
     * @param tokens Characters to expect.
     * @return Returns the consumed character.
    
     */
    public final char expect(char... tokens) {
        for (char token : tokens) {
            if (peek() == token) {
                skip();
                return token;
            }
        }

        StringBuilder message = new StringBuilder("Found '")
                .append(peekSingleCharForMessage())
                .append("', but expected one of the following tokens:");
        for (char c : tokens) {
            message.append(' ').append('\'').append(c).append('\'');
        }

        throw syntax(message.toString());
    }

    /**
     * Creates a syntax error that adds some context to the given message.
     *
     * @param message Message for why the error occurred.
     * @return Returns the created syntax error.
     */
    public RuntimeException syntax(String message) {
        return new RuntimeException("Syntax error at line " + line() + ", column " + column() + ": " + message);
    }

    /**
     * Skip 0 or more whitespace characters (that is, ' ', '\t', '\r', and '\n').
     */
    public void ws() {
        while (isWhitespace(peek())) {
            skip();
        }
    }

    private boolean isWhitespace(char c) {
        return c == ' ' || c == '\t' || c == '\r' || c == '\n';
    }

    /**
     * Skip 0 or more spaces (that is, ' ' and '\t').
     */
    public void sp() {
        while (isSpace(peek())) {
            skip();
        }
    }

    private boolean isSpace(char c) {
        return c == ' ' || c == '\t';
    }

    /**
     * Skips spaces then expects that the next character is either the end of
     * the expression or a line break (\n, \r\n, or \r).
     *
     * @throws RuntimeException if the next non-space character is not EOF or a line break.
     */
    public void br() {
        sp();

        // EOF can also be considered a line break to end a file.
        if (eof()) {
            return;
        }

        char c = peek();
        if (c == '\n' || c == '\r') {
            skip();
        } else {
            throw syntax("Expected a line break, but found '" + c + "'");
        }
    }

    /**
     * Skips a single character while tracking lines and columns.
     */
    public void skip() {
        if (eof()) {
            return;
        }

        switch (input.charAt(position)) {
            case '\r':
                if (peek(1) == '\n') {
                    position++;
                }
                line++;
                column = 1;
                break;
            case '\n':
                line++;
                column = 1;
                break;
            default:
                column++;
        }

        position++;
    }

    /**
     * Skips over the remaining characters on a line but does not consume
     * the newline character (\n or \r\n).
     *
     * <p>This method will also terminate when the end of the expression
     * is encountered.
     *
     * <p>This method is useful, for example, for skipping the text of a
     * commented out line in an expression. If the contents of the skipped
     * line are required, then store the current position before invoking
     * this method using {@link #position()}, then call this method, then get
     * the contents of the skipped characters using {@link #sliceFrom(int)}.
     */
    public void consumeRemainingCharactersOnLine() {
        char ch = peek();
        while (ch != EOF && ch != '\n' && ch != '\r') {
            skip();
            ch = peek();
        }
    }

    /**
     * Copies a slice of the expression into a string, starting from the given 0-based character position,
     * read all the way through to the current position of the parser.
     *
     * @param start Position to slice from, ending at the current position.
     * @return Returns the copied slice of the expression from {@code start} to {@link #position}.
     */
    public final String sliceFrom(int start) {
        return input.subSequence(start, position).toString();
    }

    /**
     * Gets a zero-copy slice of the expression starting from the given 0-based character position, read all the
     * way through to the current position of the parser.
     *
     * @param start Position to slice from, ending at the current position.
     * @return Returns the zero-copy slice of the expression from {@code start} to {@link #position}.
     */
    public final CharSequence borrowSliceFrom(int start) {
        return CharBuffer.wrap(input, start, position);
    }

    /**
     * Gets a zero-copy slice of the expression starting from the given 0-based character position, read all the
     * way through to the current position of the parser minus {@code removeRight}.
     *
     * @param start       Position to slice from, ending at the current position.
     * @param removeRight Number of characters to omit before the current position.
     * @return Returns the zero-copy slice of the expression from {@code start} to {@link #position}.
     */
    public final CharSequence borrowSliceFrom(int start, int removeRight) {
        return CharBuffer.wrap(input, start, position - removeRight);
    }

    @Deprecated
    public final int consumeUntilNoLongerMatches(Predicate<Character> predicate) {
        int startPosition = position;
        char ch = peek();
        while (ch != EOF && predicate.test(ch)) {
            skip();
            ch = peek();
        }
        return position - startPosition;
    }

    /**
     * Reads a lexeme from the expression while the given {@code predicate} matches each peeked character.
     *
     * @param predicate Predicate that filters characters.
     * @return Returns the consumed lexeme (or an empty string on no matches).
     */
    public final int consumeWhile(IntPredicate predicate) {
        int startPosition = position;
        char ch = peek();
        while (ch != EOF && predicate.test(ch)) {
            skip();
            ch = peek();
        }
        return position - startPosition;
    }

    /**
     * Increases the current nesting level of the parser.
     *
     * <p>This method can be manually invoked when parsing in order to
     * prevent parsing too deeply using recursive descent parsers.
     *
     * @throws RuntimeException if the nesting level is deeper than the max allowed nesting.
     */
    public final void increaseNestingLevel() {
        nestingLevel++;

        if (maxNestingLevel > 0 && nestingLevel > maxNestingLevel) {
            throw syntax("Parser exceeded the maximum allowed depth of " + maxNestingLevel);
        }
    }

    /**
     * Decreases the current nesting level of the parser.
     */
    public final void decreaseNestingLevel() {
        nestingLevel--;

        if (nestingLevel < 0) {
            throw syntax("Invalid parser state. Nesting level set to -1");
        }
    }

    /**
     * Gets the current 0-based nesting level of the parser.
     *
     * @return Returns the current nesting level.
     */
    public int nestingLevel() {
        return nestingLevel;
    }
}
