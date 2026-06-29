/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class TaggedStringLiteralTest {

    // --- #re tag tests ---

    public static Stream<Arguments> reTagTests() {
        return Stream.of(
                Arguments.of("^\\d{5}$", "^\\d{5}$"),
                Arguments.of("\\w+\\s\\d", "\\w+\\s\\d"),
                // Backslashes pass through literally (no escape processing)
                Arguments.of("\\\\", "\\\\"),
                // Escaped quote passes through literally (tokenizer handles termination)
                Arguments.of("\\\"", "\\\""),
                Arguments.of("a\\\"b", "a\\\"b"),
                Arguments.of("hello", "hello"),
                Arguments.of("", ""),
                Arguments.of("[a-z]+\\.(\\d{1,3}\\.){3}\\d{1,3}", "[a-z]+\\.(\\d{1,3}\\.){3}\\d{1,3}"));
    }

    @ParameterizedTest
    @MethodSource("reTagTests")
    public void parsesReTaggedStrings(String input, String expected) {
        TaggedStringLiteral.Result result = TaggedStringLiteral.scan("re", input, false);
        assertThat(result.token, is(IdlToken.STRING));
        assertThat(result.stringValue.toString(), equalTo(expected));
    }

    @Test
    public void regexNewlinesAreStripped() {
        // Newlines are stripped automatically (no backslash needed)
        TaggedStringLiteral.Result result = TaggedStringLiteral.scan("re", "abc\ndef", false);
        assertThat(result.stringValue.toString(), equalTo("abcdef"));
    }

    @Test
    public void regexTextBlockStripsNewlinesAndConcatenatesLines() {
        // Text block raw content starts with \n (as it comes from the tokenizer).
        // After text block normalization, lines are concatenated by stripping newlines.
        TaggedStringLiteral.Result result = TaggedStringLiteral.scan("re", "\n    ^\\d{5}$\n    ", true);
        assertThat(result.token, is(IdlToken.STRING));
        assertThat(result.stringValue.toString(), equalTo("^\\d{5}$"));
    }

    @Test
    public void regexTextBlockMultilinePattern() {
        // Multiline pattern: lines are joined without needing trailing backslash
        TaggedStringLiteral.Result result = TaggedStringLiteral.scan("re",
                "\n    [A-Z]:\\\\\\\\\n    ([\\w\\s\\d]+\\\\)*\n    ",
                true);
        assertThat(result.token, is(IdlToken.STRING));
        assertThat(result.stringValue.toString(), equalTo("[A-Z]:\\\\\\\\([\\w\\s\\d]+\\\\)*"));
    }

    // --- #b tag tests ---

    public static Stream<Arguments> bTagTests() {
        return Stream.of(
                Arguments.of("Hello world", base64("Hello world")),
                Arguments.of("\\xaa", base64((byte) 0xaa)),
                Arguments.of("A\\x42C", base64((byte) 'A', (byte) 0x42, (byte) 'C')),
                Arguments.of("\\n", base64((byte) '\n')),
                Arguments.of("\\t", base64((byte) '\t')),
                Arguments.of("\\r", base64((byte) '\r')),
                Arguments.of("\\\\", base64((byte) '\\')),
                Arguments.of("\\0", base64((byte) 0)),
                // Python-compatible escapes
                Arguments.of("\\a", base64((byte) 0x07)),
                Arguments.of("\\b", base64((byte) 0x08)),
                Arguments.of("\\f", base64((byte) 0x0C)),
                Arguments.of("\\v", base64((byte) 0x0B)),
                // Octal escapes
                Arguments.of("\\101", base64((byte) 'A')), // 0101 = 65 = 'A'
                Arguments.of("\\377", base64((byte) 0xFF)), // max single byte
                Arguments.of("\\7", base64((byte) 7)), // single octal digit
                Arguments.of("\\77", base64((byte) 63)), // two octal digits
                // Escaped quote
                Arguments.of("a\\\"b", base64((byte) 'a', (byte) '"', (byte) 'b')),
                Arguments.of("", base64()));
    }

    @ParameterizedTest
    @MethodSource("bTagTests")
    public void parsesBTaggedStrings(String input, String expected) {
        TaggedStringLiteral.Result result = TaggedStringLiteral.scan("b", input, false);
        assertThat(result.token, is(IdlToken.STRING));
        assertThat(result.stringValue.toString(), equalTo(expected));
    }

    @Test
    public void binaryInvalidEscapeThrows() {
        RuntimeException e = assertThrows(RuntimeException.class,
                () -> TaggedStringLiteral.scan("b", "\\q", false));
        assertTrue(e.getMessage().contains("Invalid escape in binary string"));
    }

    @Test
    public void binaryIncompleteHexEscapeThrows() {
        RuntimeException e = assertThrows(RuntimeException.class,
                () -> TaggedStringLiteral.scan("b", "\\x0", false));
        assertTrue(e.getMessage().contains("Incomplete \\x escape"));
    }

    @Test
    public void binaryOctalOverflowThrows() {
        RuntimeException e = assertThrows(RuntimeException.class,
                () -> TaggedStringLiteral.scan("b", "\\777", false));
        assertTrue(e.getMessage().contains("exceeds byte range"));
    }

    // --- #hex tag tests ---

    @Test
    public void parsesHexBasic() {
        TaggedStringLiteral.Result result = TaggedStringLiteral.scan("hex", "48656c6c6f", false);
        assertThat(result.token, is(IdlToken.STRING));
        assertThat(result.stringValue.toString(), equalTo(base64("Hello")));
    }

    @Test
    public void parsesHexWithSpaces() {
        TaggedStringLiteral.Result result = TaggedStringLiteral.scan("hex", "48 65 6c 6c 6f", false);
        assertThat(result.stringValue.toString(), equalTo(base64("Hello")));
    }

    @Test
    public void parsesHexUpperCase() {
        TaggedStringLiteral.Result result = TaggedStringLiteral.scan("hex", "4F 4B", false);
        assertThat(result.stringValue.toString(), equalTo(base64("OK")));
    }

    @Test
    public void parsesAnnotatedCborSimple() {
        TaggedStringLiteral.Result result = TaggedStringLiteral.scan("hex", "81c1", false);
        assertThat(result.token, is(IdlToken.STRING));
        assertThat(result.stringValue.toString(), equalTo(base64((byte) 0x81, (byte) 0xc1)));
    }

    @Test
    public void parsesAnnotatedCborWithCommentsAndWhitespace() {
        String input = "81 # array(1)\n"
                + "c1 # tag(1)\n"
                + "fb 41d9ad970f9b4396 # float\n";
        TaggedStringLiteral.Result result = TaggedStringLiteral.scan("hex", input, false);
        assertThat(result.stringValue.toString(),
                equalTo(base64(
                        (byte) 0x81,
                        (byte) 0xc1,
                        (byte) 0xfb,
                        (byte) 0x41,
                        (byte) 0xd9,
                        (byte) 0xad,
                        (byte) 0x97,
                        (byte) 0x0f,
                        (byte) 0x9b,
                        (byte) 0x43,
                        (byte) 0x96)));
    }

    @Test
    public void parsesAnnotatedCborCommentOnlyLines() {
        String input = "# This is a comment-only line\n"
                + "ff\n";
        TaggedStringLiteral.Result result = TaggedStringLiteral.scan("hex", input, false);
        assertThat(result.stringValue.toString(), equalTo(base64((byte) 0xff)));
    }

    @Test
    public void parsesAnnotatedCborMultipleCommentLines() {
        String input = "# File header\n"
                + "89 50 4e 47 0d 0a 1a 0a\n"
                + "# IHDR chunk\n"
                + "00 00 00 0d 49 48 44 52\n";
        TaggedStringLiteral.Result result = TaggedStringLiteral.scan("hex", input, false);
        assertThat(result.stringValue.toString(),
                equalTo(base64(
                        (byte) 0x89,
                        (byte) 0x50,
                        (byte) 0x4e,
                        (byte) 0x47,
                        (byte) 0x0d,
                        (byte) 0x0a,
                        (byte) 0x1a,
                        (byte) 0x0a,
                        (byte) 0x00,
                        (byte) 0x00,
                        (byte) 0x00,
                        (byte) 0x0d,
                        (byte) 0x49,
                        (byte) 0x48,
                        (byte) 0x44,
                        (byte) 0x52)));
    }

    @Test
    public void parsesAnnotatedCborTabsIgnored() {
        TaggedStringLiteral.Result result = TaggedStringLiteral.scan("hex", "81\tc1", false);
        assertThat(result.stringValue.toString(), equalTo(base64((byte) 0x81, (byte) 0xc1)));
    }

    @Test
    public void hexOddDigitsThrows() {
        RuntimeException e = assertThrows(RuntimeException.class,
                () -> TaggedStringLiteral.scan("hex", "abc", false));
        assertTrue(e.getMessage().contains("Odd number of hex digits"));
    }

    @Test
    public void hexInvalidCharThrows() {
        RuntimeException e = assertThrows(RuntimeException.class,
                () -> TaggedStringLiteral.scan("hex", "GG", false));
        assertTrue(e.getMessage().contains("Invalid character in hex string"));
    }

    @Test
    public void hexEmptyStringProducesEmptyResult() {
        TaggedStringLiteral.Result result = TaggedStringLiteral.scan("hex", "", false);
        assertThat(result.stringValue.toString(), equalTo(""));
    }

    // --- #timestamp tag tests ---

    @Test
    public void parsesTimestampWholeSeconds() {
        TaggedStringLiteral.Result result = TaggedStringLiteral.scan("timestamp", "2024-01-01T00:00:00Z", false);
        assertThat(result.token, is(IdlToken.NUMBER));
        assertThat(result.numberValue.longValue(), equalTo(1704067200L));
    }

    @Test
    public void parsesTimestampWithMillis() {
        TaggedStringLiteral.Result result = TaggedStringLiteral.scan("timestamp", "2024-06-15T12:30:00.500Z", false);
        assertThat(result.token, is(IdlToken.NUMBER));
        assertThat(result.numberValue.doubleValue(), equalTo(1718454600.5));
    }

    @Test
    public void parsesTimestampEpoch() {
        TaggedStringLiteral.Result result = TaggedStringLiteral.scan("timestamp", "1970-01-01T00:00:00Z", false);
        assertThat(result.token, is(IdlToken.NUMBER));
        assertThat(result.numberValue.longValue(), equalTo(0L));
    }

    @Test
    public void parsesTimestampWithThreeDigitMillis() {
        TaggedStringLiteral.Result result = TaggedStringLiteral.scan(
                "timestamp",
                "2026-04-14T01:40:23.657Z",
                false);
        assertThat(result.token, is(IdlToken.NUMBER));
        assertThat(result.numberValue.doubleValue(), equalTo(1776130823.657));
    }

    @Test
    public void timestampInvalidThrows() {
        RuntimeException e = assertThrows(RuntimeException.class,
                () -> TaggedStringLiteral.scan("timestamp", "not-a-date", false));
        assertTrue(e.getMessage().contains("Invalid RFC 3339 timestamp"));
    }

    // --- Handler lookup ---

    @Test
    public void hasHandlerForKnownTags() {
        assertThat(TaggedStringLiteral.hasHandler("re"), is(true));
        assertThat(TaggedStringLiteral.hasHandler("b"), is(true));
        assertThat(TaggedStringLiteral.hasHandler("hex"), is(true));
        assertThat(TaggedStringLiteral.hasHandler("timestamp"), is(true));
    }

    @Test
    public void hasHandlerReturnsFalseForUnknown() {
        assertThat(TaggedStringLiteral.hasHandler("unknown"), is(false));
    }

    // --- Helpers ---

    private static String base64(String s) {
        return Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }

    private static String base64(byte... bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }
}
