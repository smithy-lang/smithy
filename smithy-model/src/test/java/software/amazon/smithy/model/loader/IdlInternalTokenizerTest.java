/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;

import java.util.stream.Stream;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class IdlInternalTokenizerTest {

    @Test
    public void skipSpaces() {
        IdlInternalTokenizer tokenizer = new IdlInternalTokenizer("a.smithy", "    hi");

        tokenizer.skipSpaces();

        MatcherAssert.assertThat(tokenizer.getCurrentToken(), is(IdlToken.IDENTIFIER));
        MatcherAssert.assertThat(tokenizer.getCurrentTokenColumn(), is(5));
    }

    @Test
    public void skipsExpectedSpaces() {
        IdlInternalTokenizer tokenizer = new IdlInternalTokenizer("a.smithy", "    hi");

        tokenizer.expectAndSkipSpaces();

        MatcherAssert.assertThat(tokenizer.getCurrentToken(), is(IdlToken.IDENTIFIER));
        MatcherAssert.assertThat(tokenizer.getCurrentTokenColumn(), is(5));
    }

    @Test
    public void failsWhenExpectedSpacesNotThere() {
        IdlInternalTokenizer tokenizer = new IdlInternalTokenizer("a.smithy", "abc");

        ModelSyntaxException e = Assertions.assertThrows(ModelSyntaxException.class, tokenizer::expectAndSkipSpaces);

        assertThat(e.getMessage(),
                startsWith("Syntax error at line 1, column 1: Expected SPACE(' ') but found "
                        + "IDENTIFIER('abc')"));
    }

    @Test
    public void skipWhitespace() {
        IdlInternalTokenizer tokenizer = new IdlInternalTokenizer("a.smithy", " \n\n hi");

        tokenizer.skipWs();

        MatcherAssert.assertThat(tokenizer.getCurrentToken(), is(IdlToken.IDENTIFIER));
        MatcherAssert.assertThat(tokenizer.getCurrentTokenLine(), is(3));
        MatcherAssert.assertThat(tokenizer.getCurrentTokenColumn(), is(2));
    }

    @Test
    public void expectAndSkipWhitespace() {
        IdlInternalTokenizer tokenizer = new IdlInternalTokenizer("a.smithy", " \n\n hi");

        tokenizer.expectAndSkipWhitespace();

        MatcherAssert.assertThat(tokenizer.getCurrentToken(), is(IdlToken.IDENTIFIER));
        MatcherAssert.assertThat(tokenizer.getCurrentTokenLine(), is(3));
        MatcherAssert.assertThat(tokenizer.getCurrentTokenColumn(), is(2));
    }

    @Test
    public void throwsWhenExpectedWhitespaceNotFound() {
        IdlInternalTokenizer tokenizer = new IdlInternalTokenizer("a.smithy", "hi");

        ModelSyntaxException e = Assertions.assertThrows(ModelSyntaxException.class,
                tokenizer::expectAndSkipWhitespace);

        assertThat(e.getMessage(),
                startsWith("Syntax error at line 1, column 1: Expected one or more whitespace "
                        + "characters, but found IDENTIFIER('hi')"));
    }

    @Test
    public void skipDocsAndWhitespace() {
        IdlInternalTokenizer tokenizer = new IdlInternalTokenizer("a.smithy",
                " \n\n /// Docs\n/// Docs\n\n hi");

        tokenizer.skipWsAndDocs();

        MatcherAssert.assertThat(tokenizer.getCurrentToken(), is(IdlToken.IDENTIFIER));
        MatcherAssert.assertThat(tokenizer.getCurrentTokenLine(), is(6));
        MatcherAssert.assertThat(tokenizer.getCurrentTokenColumn(), is(2));
    }

    @Test
    public void expectsAndSkipsBr() {
        IdlInternalTokenizer tokenizer = new IdlInternalTokenizer("a.smithy", "\n  Hi");

        tokenizer.expectAndSkipBr();

        MatcherAssert.assertThat(tokenizer.getCurrentToken(), is(IdlToken.IDENTIFIER));
        MatcherAssert.assertThat(tokenizer.getCurrentTokenLine(), is(2));
        MatcherAssert.assertThat(tokenizer.getCurrentTokenColumn(), is(3));
    }

    @Test
    public void throwsWhenBrNotFound() {
        IdlInternalTokenizer tokenizer = new IdlInternalTokenizer("a.smithy", "Hi");

        ModelSyntaxException e = Assertions.assertThrows(ModelSyntaxException.class,
                tokenizer::expectAndSkipBr);

        assertThat(e.getMessage(),
                startsWith("Syntax error at line 1, column 1: Expected a line break, but "
                        + "found IDENTIFIER('Hi')"));
    }

    @Test
    public void testsCurrentTokenLexeme() {
        IdlInternalTokenizer tokenizer = new IdlInternalTokenizer("a.smithy", "Hi");

        assertThat(tokenizer.isCurrentLexeme("Hi"), is(true));
    }

    @Test
    public void expectsSingleTokenType() {
        IdlInternalTokenizer tokenizer = new IdlInternalTokenizer("a.smithy", "Hi");

        tokenizer.expect(IdlToken.IDENTIFIER);
    }

    @Test
    public void failsForSingleExpectedToken() {
        IdlInternalTokenizer tokenizer = new IdlInternalTokenizer("a.smithy", "Hi");

        ModelSyntaxException e = Assertions.assertThrows(ModelSyntaxException.class,
                () -> tokenizer.expect(IdlToken.NUMBER));

        assertThat(e.getMessage(),
                startsWith("Syntax error at line 1, column 1: Expected NUMBER but "
                        + "found IDENTIFIER('Hi')"));
    }

    @Test
    public void expectsMultipleTokenTypes() {
        IdlInternalTokenizer tokenizer = new IdlInternalTokenizer("a.smithy", "Hi");

        tokenizer.expect(IdlToken.STRING, IdlToken.IDENTIFIER);
    }

    @Test
    public void failsForMultipleExpectedTokens() {
        IdlInternalTokenizer tokenizer = new IdlInternalTokenizer("a.smithy", "Hi");

        ModelSyntaxException e = Assertions.assertThrows(ModelSyntaxException.class,
                () -> tokenizer.expect(IdlToken.NUMBER, IdlToken.LBRACE));

        assertThat(e.getMessage(),
                startsWith("Syntax error at line 1, column 1: Expected one of NUMBER, LBRACE('{'); "
                        + "but found IDENTIFIER('Hi')"));
    }

    @Test
    public void returnsCapturedDocsInRange() {
        String model = "/// Hi\n"
                + "/// There\n"
                + "/// 123\n"
                + "/// 456\n";
        IdlInternalTokenizer tokenizer = new IdlInternalTokenizer("a.smithy", model);

        tokenizer.skipWsAndDocs();
        String lines = tokenizer.removePendingDocCommentLines();

        assertThat(lines, equalTo("Hi\nThere\n123\n456"));
        MatcherAssert.assertThat(tokenizer.removePendingDocCommentLines(), nullValue());
    }

    public static Stream<Arguments> textBlockTests() {
        return Stream.of(
                Arguments.of(
                        "\"\"\"\n"
                                + "    Hello\n"
                                + "        - Indented\"\"\"\n",
                        "Hello\n    - Indented"),
                Arguments.of(
                        "\"\"\"\n"
                                + "    Hello\n"
                                + "        - Indented\n"
                                + "    \"\"\"\n",
                        "Hello\n    - Indented\n"),
                Arguments.of(
                        "\"\"\"\n"
                                + "    Hello\n"
                                + "        - Indented\n"
                                + "\"\"\"\n",
                        "    Hello\n        - Indented\n"),
                Arguments.of(
                        "\"\"\"\n"
                                + "    Hello\"\"\"\n",
                        "Hello"),
                Arguments.of(
                        "\"\"\"\n"
                                + "    Hello\n"
                                + "\n"
                                + "        - Indented\n"
                                + "\"\"\"\n",
                        "    Hello\n\n        - Indented\n"),
                Arguments.of(
                        "\"\"\"\n"
                                + "                        \n" // only WS doesn't influence line length calculations.
                                + "          Hello\n"
                                + "                        \n" // only WS doesn't influence line length calculations.
                                + "          \"\"\"",
                        "\nHello\n\n"),
                Arguments.of(
                        "\"\"\"\n"
                                + "\n" // empty lines are incidental whitespace.
                                + "          Hello\n"
                                + "                        \n" // only WS doesn't influence line length calculations.
                                + "          \"\"\"",
                        "\nHello\n\n"),
                Arguments.of(
                        "\"\"\"\n"
                                + "\n" // empty lines are incidental whitespace.
                                + "Hello\n"
                                + "\n"
                                + "\n"
                                + "\"\"\"",
                        "\nHello\n\n\n"),
                Arguments.of(
                        "\"\"\"\n"
                                + "\"\"\"",
                        ""));
    }

    @ParameterizedTest
    @MethodSource("textBlockTests")
    public void parsesTextBlocks(String model, String stringValue) {
        IdlInternalTokenizer tokenizer = new IdlInternalTokenizer("a.smithy", model);
        tokenizer.expect(IdlToken.TEXT_BLOCK);

        assertThat(tokenizer.getCurrentTokenStringSlice().toString(), equalTo(stringValue));
    }
}
