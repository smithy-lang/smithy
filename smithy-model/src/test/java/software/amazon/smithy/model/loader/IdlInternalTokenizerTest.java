/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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

        assertThat(e.getMessage(), startsWith("Syntax error at line 1, column 1: Expected SPACE(' ') but found "
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

        assertThat(e.getMessage(), startsWith("Syntax error at line 1, column 1: Expected one or more whitespace "
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

        assertThat(e.getMessage(), startsWith("Syntax error at line 1, column 1: Expected a line break, but "
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

        assertThat(e.getMessage(), startsWith("Syntax error at line 1, column 1: Expected NUMBER but "
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

        assertThat(e.getMessage(), startsWith("Syntax error at line 1, column 1: Expected one of NUMBER, LBRACE('{'); "
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
}
