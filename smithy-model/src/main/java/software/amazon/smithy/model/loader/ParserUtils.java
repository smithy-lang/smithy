/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader;

import software.amazon.smithy.utils.SimpleParser;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Utility methods that act on a {@link SimpleParser} and parse
 * Smithy grammar productions.
 */
@SmithyUnstableApi
public final class ParserUtils {

    private ParserUtils() {}

    /**
     * Parses a Smithy number production into a string.
     *
     * <pre>-?(?:0|[1-9]\d*)(?:\.\d+)?(?:[eE][+-]?\d+)?</pre>
     *
     * @param parser Parser to consume tokens from.
     * @return Returns the parsed number lexeme.
     */
    public static String parseNumber(SimpleParser parser) {
        int startPosition = parser.position();
        char current = parser.peek();

        if (current == '-') {
            parser.skip();
            if (!isDigit(parser.peek())) {
                throw parser.syntax(createInvalidString(parser, startPosition, "'-' must be followed by a digit"));
            }
        }

        parser.consumeWhile(ParserUtils::isDigit);

        // Consume decimals.
        char peek = parser.peek();
        if (peek == '.') {
            parser.skip();
            if (parser.consumeWhile(ParserUtils::isDigit) == 0) {
                throw parser.syntax(createInvalidString(parser, startPosition, "'.' must be followed by a digit"));
            }
        }

        // Consume scientific notation.
        peek = parser.peek();
        if (peek == 'e' || peek == 'E') {
            parser.skip();
            peek = parser.peek();
            if (peek == '+' || peek == '-') {
                parser.skip();
            }
            if (parser.consumeWhile(ParserUtils::isDigit) == 0) {
                throw parser.syntax(
                        createInvalidString(parser, startPosition, "'e', '+', and '-' must be followed by a digit"));
            }
        }

        return parser.sliceFrom(startPosition);
    }

    private static String createInvalidString(SimpleParser parser, int startPosition, String message) {
        String lexeme = parser.sliceFrom(startPosition);
        return String.format("Invalid number '%s': %s", lexeme, message);
    }

    /**
     * Expects and returns a parsed Smithy identifier production.
     *
     * @param parser Parser to consume tokens from.
     * @return Returns the parsed identifier.
     */
    public static String parseIdentifier(SimpleParser parser) {
        int start = parser.position();
        consumeIdentifier(parser);
        return parser.sliceFrom(start);
    }

    /**
     * Expects and returns a parsed absolute Smithy Shape ID that
     * does not include a member.
     *
     * @param parser Parser to consume tokens from.
     * @return Returns the parsed Shape ID as a string.
     */
    public static String parseRootShapeId(SimpleParser parser) {
        int start = parser.position();
        consumeShapeId(parser, false);
        return parser.sliceFrom(start);
    }

    /**
     * Expects and returns a parsed relative or absolute Smithy Shape ID.
     *
     * @param parser Parser to consume tokens from.
     * @return Returns the parsed Shape ID as a string.
     */
    public static String parseShapeId(SimpleParser parser) {
        int start = parser.position();
        consumeShapeId(parser, true);
        return parser.sliceFrom(start);
    }

    private static void consumeShapeId(SimpleParser parser, boolean parseMember) {
        consumeNamespace(parser);

        if (parser.peek() == '#') {
            parser.skip();
            consumeIdentifier(parser);
        }

        if (parseMember && parser.peek() == '$') {
            parser.skip();
            consumeIdentifier(parser);
        }
    }

    /**
     * Expects and consumes a valid Smithy shape ID namespace.
     *
     * @param parser Parser to consume tokens from.
     */
    public static void consumeNamespace(SimpleParser parser) {
        consumeIdentifier(parser);
        while (parser.peek() == '.') {
            parser.skip();
            consumeIdentifier(parser);
        }
    }

    /**
     * Expects and skips over a Smithy identifier production.
     *
     * <pre>
     *     identifier       = identifier_start *identifier_chars
     *     identifier_start = *"_" ALPHA
     *     identifier_chars = ALPHA / DIGIT / "_"
     * </pre>
     *
     * @param parser Parser to consume tokens from.
     */
    public static void consumeIdentifier(SimpleParser parser) {
        // Parse identifier_start
        char c = parser.peek();
        if (c == '_') {
            parser.consumeWhile(next -> next == '_');
            if (!ParserUtils.isValidIdentifierCharacter(parser.peek())) {
                throw invalidIdentifier(parser);
            }
        } else if (!isAlphabetic(c)) {
            throw invalidIdentifier(parser);
        }

        // Skip the first character since it's known to be valid.
        parser.skip();

        // Parse identifier_chars
        parser.consumeWhile(ParserUtils::isValidIdentifierCharacter);
    }

    private static RuntimeException invalidIdentifier(SimpleParser parser) {
        throw parser.syntax("Expected a valid identifier character, but found '"
                + parser.peekSingleCharForMessage() + '\'');
    }

    /**
     * Returns true if the given character is allowed in an identifier.
     *
     * @param c Character to check.
     * @return Returns true if the character is allowed in an identifier.
     */
    public static boolean isValidIdentifierCharacter(char c) {
        return isValidIdentifierCharacter((int) c);
    }

    /**
     * Returns true if the given character is allowed in an identifier.
     *
     * @param c Character to check.
     * @return Returns true if the character is allowed in an identifier.
     */
    public static boolean isValidIdentifierCharacter(int c) {
        return isIdentifierStart(c) || isDigit(c);
    }

    /**
     * Returns true if the given character is allowed to start an identifier.
     *
     * @param c Character to check.
     * @return Returns true if the character can start an identifier.
     */
    public static boolean isIdentifierStart(char c) {
        return isIdentifierStart((int) c);
    }

    /**
     * Returns true if the given character is allowed to start an identifier.
     *
     * @param c Character to check.
     * @return Returns true if the character can start an identifier.
     */
    public static boolean isIdentifierStart(int c) {
        return c == '_' || isAlphabetic(c);
    }

    /**
     * Returns true if the given value is a digit 0-9.
     *
     * @param c Character to check.
     * @return Returns true if the character is a digit.
     */
    public static boolean isDigit(char c) {
        return isDigit((int) c);
    }

    /**
     * Returns true if the given value is a digit 0-9.
     *
     * @param c Character to check.
     * @return Returns true if the character is a digit.
     */
    public static boolean isDigit(int c) {
        return c >= '0' && c <= '9';
    }

    /**
     * Returns true if the given character is an alphabetic character
     * A-Z, a-z. This is a stricter version of {@link Character#isAlphabetic}.
     *
     * @param c Character to check.
     * @return Returns true if the character is an alphabetic character.
     */
    public static boolean isAlphabetic(char c) {
        return isAlphabetic((int) c);
    }

    /**
     * Returns true if the given character is an alphabetic character
     * A-Z, a-z. This is a stricter version of {@link Character#isAlphabetic}.
     *
     * @param c Character to check.
     * @return Returns true if the character is an alphabetic character.
     */
    public static boolean isAlphabetic(int c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
    }
}
