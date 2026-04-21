/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Handles tagged string literals in the Smithy IDL.
 *
 * <p>A tagged string literal is a string prefixed with {@code #tag} that changes
 * how the string content is interpreted. The result can be a string or number token.
 */
final class TaggedStringLiteral {

    /**
     * The result of scanning a tagged string literal.
     */
    static final class Result {
        final IdlToken token;
        final CharSequence stringValue;
        final Number numberValue;

        private Result(IdlToken token, CharSequence stringValue, Number numberValue) {
            this.token = token;
            this.stringValue = stringValue;
            this.numberValue = numberValue;
        }

        static Result ofString(CharSequence value) {
            return new Result(IdlToken.STRING, value, null);
        }

        static Result ofNumber(Number value) {
            return new Result(IdlToken.NUMBER, null, value);
        }
    }

    private static final Map<String, Function<CharSequence, Result>> HANDLERS = new HashMap<>();

    static {
        HANDLERS.put("re", TaggedStringLiteral::scanRegexContents);
        HANDLERS.put("b", TaggedStringLiteral::scanBinaryContents);
        HANDLERS.put("timestamp", TaggedStringLiteral::scanTimestampContents);
        HANDLERS.put("hex", TaggedStringLiteral::scanHexContents);
    }

    private TaggedStringLiteral() {}

    static boolean hasHandler(String tag) {
        return HANDLERS.containsKey(tag);
    }

    static Result scan(String tag, CharSequence lexeme, boolean isTextBlock) {
        lexeme = IdlStringLexer.normalizeAndFormat(lexeme, isTextBlock);
        return HANDLERS.get(tag).apply(lexeme);
    }

    /**
     * Scans regex string contents. Backslash sequences are passed through literally
     * except for {@code \"} and {@code \\}.
     */
    private static Result scanRegexContents(CharSequence lexeme) {
        StringBuilder result = new StringBuilder(lexeme.length());
        for (int i = 0; i < lexeme.length(); i++) {
            char c = lexeme.charAt(i);
            if (c == '\\' && i + 1 < lexeme.length()) {
                char next = lexeme.charAt(i + 1);
                if (next == '"') {
                    result.append('"');
                    i++;
                } else if (next == '\\') {
                    result.append('\\');
                    i++;
                } else if (next == '\n') {
                    i++;
                } else {
                    result.append(c);
                    result.append(next);
                    i++;
                }
            } else {
                if (!IdlStringLexer.isValidNormalCharacter(c, true)) {
                    throw new RuntimeException("Invalid string character: `" + c + "`");
                }
                result.append(c);
            }
        }
        return Result.ofString(result);
    }

    /**
     * Scans binary string contents like Python's {@code b'...'} syntax.
     * The resulting bytes are base64-encoded.
     */
    private static Result scanBinaryContents(CharSequence lexeme) {
        byte[] buffer = new byte[lexeme.length()];
        int length = 0;
        for (int i = 0; i < lexeme.length(); i++) {
            char c = lexeme.charAt(i);
            if (c == '\\' && i + 1 < lexeme.length()) {
                char next = lexeme.charAt(i + 1);
                switch (next) {
                    case 'x':
                        if (i + 3 >= lexeme.length()) {
                            throw new RuntimeException("Incomplete \\x escape in binary string");
                        }
                        int hi = hexDigit(lexeme.charAt(i + 2));
                        int lo = hexDigit(lexeme.charAt(i + 3));
                        buffer[length++] = (byte) ((hi << 4) | lo);
                        i += 3;
                        break;
                    case '\\':
                        buffer[length++] = (byte) '\\';
                        i++;
                        break;
                    case '"':
                        buffer[length++] = (byte) '"';
                        i++;
                        break;
                    case 'a':
                        buffer[length++] = (byte) 0x07;
                        i++;
                        break;
                    case 'b':
                        buffer[length++] = (byte) 0x08;
                        i++;
                        break;
                    case 'f':
                        buffer[length++] = (byte) 0x0C;
                        i++;
                        break;
                    case 'n':
                        buffer[length++] = (byte) '\n';
                        i++;
                        break;
                    case 'r':
                        buffer[length++] = (byte) '\r';
                        i++;
                        break;
                    case 't':
                        buffer[length++] = (byte) '\t';
                        i++;
                        break;
                    case 'v':
                        buffer[length++] = (byte) 0x0B;
                        i++;
                        break;
                    case '0':
                    case '1':
                    case '2':
                    case '3':
                    case '4':
                    case '5':
                    case '6':
                    case '7':
                        // Octal escape: 1-3 octal digits.
                        int octal = next - '0';
                        int octalChars = 1;
                        while (octalChars < 3 && i + 1 + octalChars < lexeme.length()) {
                            char oc = lexeme.charAt(i + 1 + octalChars);
                            if (oc < '0' || oc > '7') {
                                break;
                            }
                            octal = (octal << 3) | (oc - '0');
                            octalChars++;
                        }
                        if (octal > 0xFF) {
                            throw new RuntimeException("Octal escape value \\" + Integer.toOctalString(octal)
                                    + " exceeds byte range");
                        }
                        buffer[length++] = (byte) octal;
                        i += octalChars;
                        break;
                    case '\n':
                        i++;
                        break;
                    default:
                        throw new RuntimeException("Invalid escape in binary string: `\\" + next + "`");
                }
            } else {
                if (c <= 0x7F) {
                    buffer[length++] = (byte) c;
                } else {
                    byte[] utf8 = String.valueOf(c).getBytes(StandardCharsets.UTF_8);
                    System.arraycopy(utf8, 0, buffer, length, utf8.length);
                    length += utf8.length;
                }
            }
        }
        byte[] result = new byte[length];
        System.arraycopy(buffer, 0, result, 0, length);
        return Result.ofString(Base64.getEncoder().encodeToString(result));
    }

    /**
     * Parses an ISO-8601 timestamp string and returns epoch seconds as a number.
     * E.g., {@code #timestamp "2026-04-14T01:40:23.657Z"} produces {@code 1776130823.657}.
     */
    private static Result scanTimestampContents(CharSequence lexeme) {
        try {
            Instant instant = Instant.parse(lexeme);
            long epochSecond = instant.getEpochSecond();
            int nanos = instant.getNano();
            if (nanos == 0) {
                return Result.ofNumber(epochSecond);
            }
            // Combine seconds and fractional part into a double.
            double value = epochSecond + nanos / 1_000_000_000.0;
            return Result.ofNumber(value);
        } catch (DateTimeParseException e) {
            throw new RuntimeException("Invalid ISO-8601 timestamp: " + lexeme);
        }
    }

    /**
     * Parses hex-encoded bytes: strips comments ({@code #} to end of line)
     * and whitespace, decodes the remaining hex digits, and returns base64.
     */
    private static Result scanHexContents(CharSequence lexeme) {
        // First pass: strip comments and collect hex chars.
        StringBuilder hex = new StringBuilder(lexeme.length());
        boolean inComment = false;
        for (int i = 0; i < lexeme.length(); i++) {
            char c = lexeme.charAt(i);
            if (inComment) {
                if (c == '\n') {
                    inComment = false;
                }
            } else if (c == '#') {
                inComment = true;
            } else if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F')) {
                hex.append(c);
            } else if (c != ' ' && c != '\t' && c != '\n' && c != '\r') {
                throw new RuntimeException("Invalid character in annotated CBOR: `" + c + "`");
            }
        }
        if (hex.length() % 2 != 0) {
            throw new RuntimeException("Odd number of hex digits in annotated CBOR");
        }
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) ((hexDigit(hex.charAt(i * 2)) << 4) | hexDigit(hex.charAt(i * 2 + 1)));
        }
        return Result.ofString(Base64.getEncoder().encodeToString(bytes));
    }

    private static int hexDigit(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        } else if (c >= 'a' && c <= 'f') {
            return 10 + c - 'a';
        } else if (c >= 'A' && c <= 'F') {
            return 10 + c - 'A';
        }
        throw new RuntimeException("Invalid hex digit in \\x escape: `" + c + "`");
    }
}
