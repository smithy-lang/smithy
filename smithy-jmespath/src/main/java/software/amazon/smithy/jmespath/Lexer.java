/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import software.amazon.smithy.jmespath.ast.LiteralExpression;

final class Lexer {

    private static final int MAX_NESTING_LEVEL = 50;

    private final String expression;
    private final int length;
    private int position = 0;
    private int line = 1;
    private int column = 1;
    private int nestingLevel = 0;
    private final List<Token> tokens = new ArrayList<>();
    private boolean currentlyParsingLiteral;

    private Lexer(String expression) {
        this.expression = Objects.requireNonNull(expression, "expression must not be null");
        this.length = expression.length();
    }

    static TokenIterator tokenize(String expression) {
        return new Lexer(expression).doTokenize();
    }

    TokenIterator doTokenize() {
        while (!eof()) {
            char c = peek();

            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_') {
                tokens.add(parseIdentifier());
                continue;
            }

            if (c == '-' || (c >= '0' && c <= '9')) {
                tokens.add(parseNumber());
                continue;
            }

            switch (c) {
                case '.':
                    tokens.add(new Token(TokenType.DOT, null, line, column));
                    skip();
                    break;
                case '[':
                    tokens.add(parseLbracket());
                    break;
                case '*':
                    tokens.add(new Token(TokenType.STAR, null, line, column));
                    skip();
                    break;
                case '|':
                    tokens.add(parseAlternatives('|', TokenType.OR, TokenType.PIPE));
                    break;
                case '@':
                    tokens.add(new Token(TokenType.CURRENT, null, line, column));
                    skip();
                    break;
                case ']':
                    tokens.add(new Token(TokenType.RBRACKET, null, line, column));
                    skip();
                    break;
                case '{':
                    tokens.add(new Token(TokenType.LBRACE, null, line, column));
                    skip();
                    break;
                case '}':
                    tokens.add(new Token(TokenType.RBRACE, null, line, column));
                    skip();
                    break;
                case '&':
                    tokens.add(parseAlternatives('&', TokenType.AND, TokenType.EXPREF));
                    break;
                case '(':
                    tokens.add(new Token(TokenType.LPAREN, null, line, column));
                    skip();
                    break;
                case ')':
                    tokens.add(new Token(TokenType.RPAREN, null, line, column));
                    skip();
                    break;
                case ',':
                    tokens.add(new Token(TokenType.COMMA, null, line, column));
                    skip();
                    break;
                case ':':
                    tokens.add(new Token(TokenType.COLON, null, line, column));
                    skip();
                    break;
                case '"':
                    tokens.add(parseString());
                    break;
                case '\'':
                    tokens.add(parseRawStringLiteral());
                    break;
                case '`':
                    tokens.add(parseLiteral());
                    break;
                case '=':
                    tokens.add(parseEquals());
                    break;
                case '>':
                    tokens.add(parseAlternatives('=', TokenType.GREATER_THAN_EQUAL, TokenType.GREATER_THAN));
                    break;
                case '<':
                    tokens.add(parseAlternatives('=', TokenType.LESS_THAN_EQUAL, TokenType.LESS_THAN));
                    break;
                case '!':
                    tokens.add(parseAlternatives('=', TokenType.NOT_EQUAL, TokenType.NOT));
                    break;
                case ' ':
                case '\n':
                case '\r':
                case '\t':
                    skip();
                    break;
                default:
                    throw syntax("Unexpected syntax: " + peekSingleCharForMessage());
            }
        }

        tokens.add(new Token(TokenType.EOF, null, line, column));
        return new TokenIterator(tokens);
    }

    private boolean eof() {
        return position >= length;
    }

    private char peek() {
        return peek(0);
    }

    private char peek(int offset) {
        int target = position + offset;
        if (target >= length || target < 0) {
            return Character.MIN_VALUE;
        }

        return expression.charAt(target);
    }

    private char expect(char token) {
        if (peek() == token) {
            skip();
            return token;
        }

        throw syntax(String.format("Expected: '%s', but found '%s'", token, peekSingleCharForMessage()));
    }

    private String peekSingleCharForMessage() {
        char peek = peek();
        return peek == Character.MIN_VALUE ? "[EOF]" : String.valueOf(peek);
    }

    private char expect(char... tokens) {
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

    private JmespathException syntax(String message) {
        return new JmespathException("Syntax error at line " + line + " column " + column + ": " + message);
    }

    private void skip() {
        if (eof()) {
            return;
        }

        switch (expression.charAt(position)) {
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
     * Gets a slice of the expression starting from the given 0-based
     * character position, read all the way through to the current
     * position of the parser.
     *
     * @param start Position to slice from, ending at the current position.
     * @return Returns the slice of the expression from {@code start} to {@link #position}.
     */
    private String sliceFrom(int start) {
        return expression.substring(start, position);
    }

    private int consumeUntilNoLongerMatches(Predicate<Character> predicate) {
        int startPosition = position;
        while (!eof()) {
            char peekedChar = peek();
            if (!predicate.test(peekedChar)) {
                break;
            }
            skip();
        }

        return position - startPosition;
    }

    private void increaseNestingLevel() {
        nestingLevel++;

        if (nestingLevel > MAX_NESTING_LEVEL) {
            throw syntax("Parser exceeded the maximum allowed depth of " + MAX_NESTING_LEVEL);
        }
    }

    private void decreaseNestingLevel() {
        nestingLevel--;
    }

    private Token parseAlternatives(char next, TokenType first, TokenType second) {
        int currentLine = line;
        int currentColumn = column;
        skip();
        if (peek() == next) {
            skip();
            return new Token(first, null, currentLine, currentColumn);
        } else {
            return new Token(second, null, currentLine, currentColumn);
        }
    }

    private Token parseEquals() {
        int currentLine = line;
        int currentColumn = column;
        skip();
        expect('=');
        return new Token(TokenType.EQUAL, null, currentLine, currentColumn);
    }

    private Token parseIdentifier() {
        int start = position;
        int currentLine = line;
        int currentColumn = column;
        consumeUntilNoLongerMatches(this::isIdentifierCharacter);
        LiteralExpression literalNode = new LiteralExpression(sliceFrom(start), currentLine, currentColumn);
        return new Token(TokenType.IDENTIFIER, literalNode, currentLine, currentColumn);
    }

    private boolean isIdentifierCharacter(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_' || (c >= '0' && c <= '9');
    }

    private Token parseString() {
        int currentLine = line;
        int currentColumn = column;
        expect('"');
        String value = consumeInsideString();
        return new Token(TokenType.IDENTIFIER,
                new LiteralExpression(value, currentLine, currentColumn),
                currentLine,
                currentColumn);
    }

    private String consumeInsideString() {
        StringBuilder builder = new StringBuilder();

        loop: while (!eof()) {
            switch (peek()) {
                case '"':
                    skip();
                    return builder.toString();
                case '\\':
                    skip();
                    switch (peek()) {
                        case '"':
                            builder.append('"');
                            skip();
                            break;
                        case 'n':
                            builder.append('\n');
                            skip();
                            break;
                        case 't':
                            builder.append('\t');
                            skip();
                            break;
                        case 'r':
                            builder.append('\r');
                            skip();
                            break;
                        case 'f':
                            builder.append('\f');
                            skip();
                            break;
                        case 'b':
                            builder.append('\b');
                            skip();
                            break;
                        case '/':
                            builder.append('/');
                            skip();
                            break;
                        case '\\':
                            builder.append('\\');
                            skip();
                            break;
                        case 'u':
                            // Read \ u XXXX
                            skip();
                            int unicode = 0;
                            for (int i = 0; i < 4; i++) {
                                char c = peek();
                                skip();
                                if (c >= '0' && c <= '9') {
                                    unicode = (unicode << 4) | (c - '0');
                                } else if (c >= 'a' && c <= 'f') {
                                    unicode = (unicode << 4) | (10 + c - 'a');
                                } else if (c >= 'A' && c <= 'F') {
                                    unicode = (unicode << 4) | (10 + c - 'A');
                                } else {
                                    throw syntax("Invalid unicode escape character: `" + c + "`");
                                }
                            }
                            builder.append((char) unicode);
                            break;
                        case '`':
                            // Ticks can be escaped when parsing literals.
                            if (currentlyParsingLiteral) {
                                builder.append('`');
                                skip();
                                break;
                            }
                            // fall-through.
                        default:
                            throw syntax("Invalid escape: " + peek());
                    }
                    break;
                case '`':
                    // If parsing a literal and an unescaped "`" is encountered,
                    // then the literal was erroneously closed while parsing a string.
                    if (currentlyParsingLiteral) {
                        skip();
                        break loop;
                    } // fall-through
                default:
                    builder.append(peek());
                    skip();
                    break;
            }
        }

        throw syntax("Unclosed quotes");
    }

    private Token parseRawStringLiteral() {
        int currentLine = line;
        int currentColumn = column;
        expect('\'');

        StringBuilder builder = new StringBuilder();
        while (!eof()) {
            if (peek() == '\\') {
                skip();
                if (peek() == '\'') {
                    skip();
                    builder.append('\'');
                } else {
                    if (peek() == '\\') {
                        skip();
                    }
                    builder.append('\\');
                }
            } else if (peek() == '\'') {
                skip();
                String result = builder.toString();
                return new Token(TokenType.LITERAL,
                        new LiteralExpression(result, currentLine, currentColumn),
                        currentLine,
                        currentColumn);
            } else {
                builder.append(peek());
                skip();
            }
        }

        throw syntax("Unclosed raw string: " + builder);
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private Token parseNumber() {
        int start = position;
        int currentLine = line;
        int currentColumn = column;

        int startPosition = position;
        char current = peek();

        if (current == '-') {
            skip();
            if (!isDigit(peek())) {
                throw syntax(createInvalidNumberString(startPosition, "'-' must be followed by a digit"));
            }
        }

        consumeUntilNoLongerMatches(Lexer::isDigit);

        // Consume decimals.
        char peek = peek();
        if (peek == '.') {
            skip();
            if (consumeUntilNoLongerMatches(Lexer::isDigit) == 0) {
                throw syntax(createInvalidNumberString(startPosition, "'.' must be followed by a digit"));
            }
        }

        // Consume scientific notation.
        peek = peek();
        if (peek == 'e' || peek == 'E') {
            skip();
            peek = peek();
            if (peek == '+' || peek == '-') {
                skip();
            }
            if (consumeUntilNoLongerMatches(Lexer::isDigit) == 0) {
                throw syntax(createInvalidNumberString(startPosition, "'e', '+', and '-' must be followed by a digit"));
            }
        }

        String lexeme = sliceFrom(start);

        try {
            double number = Double.parseDouble(lexeme);
            LiteralExpression node = new LiteralExpression(number, currentLine, currentColumn);
            return new Token(TokenType.NUMBER, node, currentLine, currentColumn);
        } catch (NumberFormatException e) {
            throw syntax("Invalid number syntax: " + lexeme);
        }
    }

    private String createInvalidNumberString(int startPosition, String message) {
        String lexeme = sliceFrom(startPosition);
        return String.format("Invalid number '%s': %s", lexeme, message);
    }

    private Token parseLbracket() {
        int currentLine = line;
        int currentColumn = column;
        skip();
        switch (peek()) {
            case ']':
                skip();
                return new Token(TokenType.FLATTEN, null, currentLine, currentColumn);
            case '?':
                skip();
                return new Token(TokenType.FILTER, null, currentLine, currentColumn);
            default:
                return new Token(TokenType.LBRACKET, null, currentLine, currentColumn);
        }
    }

    private Token parseLiteral() {
        int currentLine = line;
        int currentColumn = column;
        currentlyParsingLiteral = true;
        expect('`');
        ws();
        Object value = parseJsonValue();
        ws();
        expect('`');
        currentlyParsingLiteral = false;
        LiteralExpression expression = new LiteralExpression(value, currentLine, currentColumn);
        return new Token(TokenType.LITERAL, expression, currentLine, currentColumn);
    }

    private Object parseJsonValue() {
        ws();
        switch (expect('\"', '{', '[', 't', 'f', 'n', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-')) {
            case 't':
                expect('r');
                expect('u');
                expect('e');
                return true;
            case 'f':
                expect('a');
                expect('l');
                expect('s');
                expect('e');
                return false;
            case 'n':
                expect('u');
                expect('l');
                expect('l');
                return null;
            case '"':
                // Backtrack for positioning.
                position--;
                column--;
                return parseString().value.expectStringValue();
            case '{':
                return parseJsonObject();
            case '[':
                return parseJsonArray();
            default: // - | 0-9
                // Backtrack.
                position--;
                column--;
                return parseNumber().value.expectNumberValue();
        }
    }

    private Object parseJsonArray() {
        increaseNestingLevel();
        List<Object> values = new ArrayList<>();
        ws();

        if (peek() == ']') {
            skip();
            decreaseNestingLevel();
            return values;
        }

        while (!eof() && peek() != '`') {
            values.add(parseJsonValue());
            ws();
            if (expect(',', ']') == ',') {
                ws();
            } else {
                decreaseNestingLevel();
                return values;
            }
        }

        throw syntax("Unclosed JSON array");
    }

    private Object parseJsonObject() {
        increaseNestingLevel();
        Map<String, Object> values = new LinkedHashMap<>();
        ws();

        if (peek() == '}') {
            skip();
            decreaseNestingLevel();
            return values;
        }

        while (!eof() && peek() != '`') {
            String key = parseString().value.expectStringValue();
            ws();
            expect(':');
            ws();
            values.put(key, parseJsonValue());
            ws();
            if (expect(',', '}') == ',') {
                ws();
            } else {
                decreaseNestingLevel();
                return values;
            }
        }

        throw syntax("Unclosed JSON object");
    }

    private void ws() {
        while (!eof()) {
            switch (peek()) {
                case ' ':
                case '\t':
                case '\r':
                case '\n':
                    skip();
                    break;
                default:
                    return;
            }
        }
    }
}
