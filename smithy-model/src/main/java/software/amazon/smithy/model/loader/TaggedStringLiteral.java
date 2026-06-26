/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader;

import java.math.BigDecimal;
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
 * <p>A tagged string literal is a string prefixed with a tag (e.g. {@code #b}) that changes
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
     * Scans regex string contents. All characters are passed through literally
     * except newlines which are stripped, allowing multiline patterns in text blocks.
     */
    private static Result scanRegexContents(CharSequence lexeme) {
        StringBuilder result = new StringBuilder(lexeme.length());
        for (int i = 0; i < lexeme.length(); i++) {
            char c = lexeme.charAt(i);
            if (c != '\n') {
                result.append(c);
            }
        }
        return Result.ofString(result);
    }

    /**
     * Scans binary string contents like Python's {@code b'...'} syntax.
     * Supports hex escapes ({@code \xHH}), octal escapes, and named escapes.
     * The result is base64-encoded.
     */
    private static Result scanBinaryContents(CharSequence lexeme) {
        // Allocate buffer for worst case (each char = 4 UTF-8 bytes).
        byte[] buffer = new byte[lexeme.length() * 4];
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
                        buffer[length++] = (byte) ((hexDigit(lexeme.charAt(i + 2)) << 4)
                                | hexDigit(lexeme.charAt(i + 3)));
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
                        buffer[length++] = 0x07;
                        i++;
                        break;
                    case 'b':
                        buffer[length++] = 0x08;
                        i++;
                        break;
                    case 'f':
                        buffer[length++] = 0x0C;
                        i++;
                        break;
                    case 'n':
                        buffer[length++] = 0x0A;
                        i++;
                        break;
                    case 'r':
                        buffer[length++] = 0x0D;
                        i++;
                        break;
                    case 't':
                        buffer[length++] = (byte) '\t';
                        i++;
                        break;
                    case 'v':
                        buffer[length++] = 0x0B;
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
                            throw new RuntimeException("Octal escape value \\"
                                    + Integer.toOctalString(octal) + " exceeds byte range");
                        }
                        buffer[length++] = (byte) octal;
                        i += octalChars;
                        break;
                    case '\n':
                        // Escaped newline: skip both.
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
     * Parses an RFC 3339 timestamp string and returns epoch seconds as a number.
     */
    private static Result scanTimestampContents(CharSequence lexeme) {
        try {
            Instant instant = Instant.parse(lexeme);
            long epochSecond = instant.getEpochSecond();
            int millis = instant.getNano() / 1_000_000;
            if (millis == 0) {
                return Result.ofNumber(epochSecond);
            }
            BigDecimal epochSecondWithMillis = BigDecimal.valueOf(epochSecond).add(BigDecimal.valueOf(millis, 3));
            return Result.ofNumber(epochSecondWithMillis);
        } catch (DateTimeParseException e) {
            throw new RuntimeException("Invalid RFC 3339 timestamp: " + lexeme);
        }
    }

    /**
     * Parses hex-encoded bytes with optional comments ({@code #} to end of line)
     * and whitespace. Returns base64-encoded result.
     */
    private static Result scanHexContents(CharSequence lexeme) {
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
                throw new RuntimeException("Invalid character in hex string: `" + c + "`");
            }
        }
        if (hex.length() % 2 != 0) {
            throw new RuntimeException("Odd number of hex digits in hex string");
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
        throw new RuntimeException("Invalid hex digit: `" + c + "`");
    }
}
