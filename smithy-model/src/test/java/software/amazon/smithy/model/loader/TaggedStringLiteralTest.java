/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.util.Base64;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class TaggedStringLiteralTest {

    private static IdlInternalTokenizer tokenizerV21(String model) {
        IdlInternalTokenizer tokenizer = new IdlInternalTokenizer("a.smithy", model);
        tokenizer.setVersion(Version.VERSION_2_1);
        return tokenizer;
    }

    // --- #re tag tests ---

    public static Stream<Arguments> reTagTests() {
        return Stream.of(
                Arguments.of("#re \"^\\d{5}$\"", "^\\d{5}$"),
                Arguments.of("#re \"\\w+\\s\\d\"", "\\w+\\s\\d"),
                Arguments.of("#re \"\\\\\"", "\\"),
                Arguments.of("#re \"\\\"\"", "\""),
                Arguments.of("#re \"hello\"", "hello"),
                Arguments.of("#re \"\"", ""),
                Arguments.of("#re \"[a-z]+\\.(\\d{1,3}\\.){3}\\d{1,3}\"", "[a-z]+\\.(\\d{1,3}\\.){3}\\d{1,3}"));
    }

    @ParameterizedTest
    @MethodSource("reTagTests")
    public void parsesReTaggedStrings(String model, String expected) {
        IdlInternalTokenizer tokenizer = tokenizerV21(model);
        assertThat(tokenizer.getCurrentToken(), is(IdlToken.STRING));
        assertThat(tokenizer.getCurrentTokenStringSlice().toString(), equalTo(expected));
    }

    @Test
    public void parsesReTaggedTextBlock() {
        String model = "#re \"\"\"\n"
                + "    ^\\d{5}$\n"
                + "    \"\"\"";
        IdlInternalTokenizer tokenizer = tokenizerV21(model);
        assertThat(tokenizer.getCurrentToken(), is(IdlToken.TEXT_BLOCK));
        assertThat(tokenizer.getCurrentTokenStringSlice().toString(), equalTo("^\\d{5}$\n"));
    }

    // --- #b tag tests ---

    public static Stream<Arguments> bTagTests() {
        return Stream.of(
                Arguments.of("#b \"Hello world\"", base64("Hello world")),
                Arguments.of("#b \"\\xaa\"", base64((byte) 0xaa)),
                Arguments.of("#b \"A\\x42C\"", base64((byte) 'A', (byte) 0x42, (byte) 'C')),
                Arguments.of("#b \"\\n\"", base64((byte) '\n')),
                Arguments.of("#b \"\\t\"", base64((byte) '\t')),
                Arguments.of("#b \"\\r\"", base64((byte) '\r')),
                Arguments.of("#b \"\\\\\"", base64((byte) '\\')),
                Arguments.of("#b \"\\0\"", base64((byte) 0)),
                // Python-compatible escapes
                Arguments.of("#b \"\\a\"", base64((byte) 0x07)),
                Arguments.of("#b \"\\b\"", base64((byte) 0x08)),
                Arguments.of("#b \"\\f\"", base64((byte) 0x0C)),
                Arguments.of("#b \"\\v\"", base64((byte) 0x0B)),
                // Octal escapes
                Arguments.of("#b \"\\101\"", base64((byte) 'A')), // 0101 = 65 = 'A'
                Arguments.of("#b \"\\377\"", base64((byte) 0xFF)), // max single byte
                Arguments.of("#b \"\\7\"", base64((byte) 7)), // single octal digit
                Arguments.of("#b \"\\77\"", base64((byte) 63)), // two octal digits
                Arguments.of("#b \"\"", base64()));
    }

    @ParameterizedTest
    @MethodSource("bTagTests")
    public void parsesBTaggedStrings(String model, String expected) {
        IdlInternalTokenizer tokenizer = tokenizerV21(model);
        assertThat(tokenizer.getCurrentToken(), is(IdlToken.STRING));
        assertThat(tokenizer.getCurrentTokenStringSlice().toString(), equalTo(expected));
    }

    @Test
    public void parsesBTaggedTextBlock() {
        String model = "#b \"\"\"\n"
                + "    \\x48\\x69\n"
                + "    \"\"\"";
        IdlInternalTokenizer tokenizer = tokenizerV21(model);
        assertThat(tokenizer.getCurrentToken(), is(IdlToken.TEXT_BLOCK));
        assertThat(tokenizer.getCurrentTokenStringSlice().toString(),
                equalTo(base64((byte) 'H', (byte) 'i', (byte) '\n')));
    }

    // --- Disambiguation and version gating ---

    @Test
    public void poundStillWorksForShapeIds() {
        IdlInternalTokenizer tokenizer = tokenizerV21("#");
        assertThat(tokenizer.getCurrentToken(), is(IdlToken.POUND));
    }

    @Test
    public void unknownTagFallsBackToPound() {
        IdlInternalTokenizer tokenizer = tokenizerV21("#foo \"bar\"");
        assertThat(tokenizer.getCurrentToken(), is(IdlToken.POUND));
    }

    @Test
    public void tagWithoutStringFallsBackToPound() {
        IdlInternalTokenizer tokenizer = tokenizerV21("#re foo");
        assertThat(tokenizer.getCurrentToken(), is(IdlToken.POUND));
    }

    @Test
    public void tagWithSpacesBeforeString() {
        IdlInternalTokenizer tokenizer = tokenizerV21("#re   \"\\d+\"");
        assertThat(tokenizer.getCurrentToken(), is(IdlToken.STRING));
        assertThat(tokenizer.getCurrentTokenStringSlice().toString(), equalTo("\\d+"));
    }

    @Test
    public void taggedLiteralsNotParsedInVersion20() {
        IdlInternalTokenizer tokenizer = new IdlInternalTokenizer("a.smithy", "#re \"\\d+\"");
        tokenizer.setVersion(Version.VERSION_2_0);
        // Should be POUND, not STRING — tagged literals require 2.1
        assertThat(tokenizer.getCurrentToken(), is(IdlToken.POUND));
    }

    @Test
    public void taggedLiteralsNotParsedWithoutVersion() {
        IdlInternalTokenizer tokenizer = new IdlInternalTokenizer("a.smithy", "#re \"\\d+\"");
        // No version set — should be POUND
        assertThat(tokenizer.getCurrentToken(), is(IdlToken.POUND));
    }

    // --- #timestamp tag tests ---

    @Test
    public void parsesTimestampWithFractionalSeconds() {
        IdlInternalTokenizer tokenizer = tokenizerV21("#timestamp \"2026-04-14T01:40:23.657Z\"");
        assertThat(tokenizer.getCurrentToken(), is(IdlToken.NUMBER));
        assertThat(tokenizer.getCurrentTokenNumberValue().doubleValue(), equalTo(1776130823.657));
    }

    @Test
    public void parsesTimestampWholeSeconds() {
        IdlInternalTokenizer tokenizer = tokenizerV21("#timestamp \"2026-04-14T01:40:23Z\"");
        assertThat(tokenizer.getCurrentToken(), is(IdlToken.NUMBER));
        assertThat(tokenizer.getCurrentTokenNumberValue().longValue(), equalTo(1776130823L));
    }

    @Test
    public void parsesTimestampEpoch() {
        IdlInternalTokenizer tokenizer = tokenizerV21("#timestamp \"1970-01-01T00:00:00Z\"");
        assertThat(tokenizer.getCurrentToken(), is(IdlToken.NUMBER));
        assertThat(tokenizer.getCurrentTokenNumberValue().longValue(), equalTo(0L));
    }

    @Test
    public void invalidTimestampProducesError() {
        IdlInternalTokenizer tokenizer = tokenizerV21("#timestamp \"not-a-timestamp\"");
        assertThat(tokenizer.getCurrentToken(), is(IdlToken.ERROR));
    }

    // --- #hex tag tests ---

    @Test
    public void parsesAnnotatedCborSimpleHex() {
        IdlInternalTokenizer tokenizer = tokenizerV21("#hex \"81c1\"");
        assertThat(tokenizer.getCurrentToken(), is(IdlToken.STRING));
        assertThat(tokenizer.getCurrentTokenStringSlice().toString(),
                equalTo(base64((byte) 0x81, (byte) 0xc1)));
    }

    @Test
    public void parsesAnnotatedCborWithCommentsAndWhitespace() {
        String model = "#hex \"\"\"\n"
                + "    81                        # array(1)\n"
                + "       c1                     #   tag(1)\n"
                + "          fb 41d9ad970f9b4396 #     float\n"
                + "    \"\"\"";
        IdlInternalTokenizer tokenizer = tokenizerV21(model);
        assertThat(tokenizer.getCurrentToken(), is(IdlToken.TEXT_BLOCK));
        assertThat(tokenizer.getCurrentTokenStringSlice().toString(),
                equalTo(base64((byte) 0x81,
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
        String model = "#hex \"\"\"\n"
                + "    # This is a comment-only line\n"
                + "    ff\n"
                + "    \"\"\"";
        IdlInternalTokenizer tokenizer = tokenizerV21(model);
        assertThat(tokenizer.getCurrentToken(), is(IdlToken.TEXT_BLOCK));
        assertThat(tokenizer.getCurrentTokenStringSlice().toString(),
                equalTo(base64((byte) 0xff)));
    }

    @Test
    public void hexOddHexDigitsProducesError() {
        IdlInternalTokenizer tokenizer = tokenizerV21("#hex \"abc\"");
        assertThat(tokenizer.getCurrentToken(), is(IdlToken.ERROR));
    }

    // --- Helpers ---

    private static String base64(String s) {
        return Base64.getEncoder().encodeToString(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static String base64(byte... bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }
}
