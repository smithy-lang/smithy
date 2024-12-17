/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.util.NoSuchElementException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TokenizerTest {
    @Test
    public void tokenizesIdentifierFollowedByQuote() {
        String contents = "metadata\"foo\"=\"bar\"";
        IdlTokenizer tokenizer = IdlTokenizer.create(contents);

        tokenizer.next();
        assertThat(tokenizer.getCurrentToken(), is(IdlToken.IDENTIFIER));
        assertThat(tokenizer.getCurrentTokenLine(), is(1));
        assertThat(tokenizer.getCurrentTokenColumn(), is(1));

        tokenizer.next();
        assertThat(tokenizer.getCurrentToken(), is(IdlToken.STRING));
        assertThat(tokenizer.getCurrentTokenLine(), is(1));
        assertThat(tokenizer.getCurrentTokenColumn(), is(9));

        tokenizer.next();
        assertThat(tokenizer.getCurrentToken(), is(IdlToken.EQUAL));
        assertThat(tokenizer.getCurrentTokenLine(), is(1));
        assertThat(tokenizer.getCurrentTokenColumn(), is(14));

        tokenizer.next();
        assertThat(tokenizer.getCurrentToken(), is(IdlToken.STRING));
        assertThat(tokenizer.getCurrentTokenLine(), is(1));
        assertThat(tokenizer.getCurrentTokenColumn(), is(15));

        tokenizer.next();
        assertThat(tokenizer.getCurrentToken(), is(IdlToken.EOF));
        assertThat(tokenizer.getCurrentTokenLine(), is(1));
        assertThat(tokenizer.getCurrentTokenColumn(), is(20));
    }

    @Test
    public void tokenizesSingleCharacterLexemes() {
        String contents = "\t\r\n\r,@$.:{}()[]# ";
        IdlTokenizer tokenizer = IdlTokenizer.create(contents);

        tokenizer.next();
        assertThat(tokenizer.getCurrentToken(), is(IdlToken.SPACE));
        assertThat(tokenizer.getCurrentTokenLine(), is(1));
        assertThat(tokenizer.getCurrentTokenColumn(), is(1));
        assertThat(tokenizer.getCurrentTokenSpan(), is(1));

        tokenizer.next();
        assertThat(tokenizer.getCurrentToken(), is(IdlToken.NEWLINE));
        assertThat(tokenizer.getCurrentTokenLine(), is(1));
        assertThat(tokenizer.getCurrentTokenColumn(), is(2));
        assertThat(tokenizer.getCurrentTokenSpan(), is(2));

        tokenizer.next();
        assertThat(tokenizer.getCurrentToken(), is(IdlToken.NEWLINE));
        assertThat(tokenizer.getCurrentTokenLine(), is(2));
        assertThat(tokenizer.getCurrentTokenColumn(), is(1));
        assertThat(tokenizer.getCurrentTokenSpan(), is(1));

        tokenizer.next();
        assertThat(tokenizer.getCurrentToken(), is(IdlToken.COMMA));
        assertThat(tokenizer.getCurrentTokenLine(), is(3));
        assertThat(tokenizer.getCurrentTokenColumn(), is(1));
        assertThat(tokenizer.getCurrentTokenSpan(), is(1));

        tokenizer.next();
        assertThat(tokenizer.getCurrentToken(), is(IdlToken.AT));
        assertThat(tokenizer.getCurrentTokenLine(), is(3));
        assertThat(tokenizer.getCurrentTokenColumn(), is(2));
        assertThat(tokenizer.getCurrentTokenSpan(), is(1));

        tokenizer.next();
        assertThat(tokenizer.getCurrentToken(), is(IdlToken.DOLLAR));
        assertThat(tokenizer.getCurrentTokenLine(), is(3));
        assertThat(tokenizer.getCurrentTokenColumn(), is(3));
        assertThat(tokenizer.getCurrentTokenSpan(), is(1));

        tokenizer.next();
        assertThat(tokenizer.getCurrentToken(), is(IdlToken.DOT));
        assertThat(tokenizer.getCurrentTokenLine(), is(3));
        assertThat(tokenizer.getCurrentTokenColumn(), is(4));
        assertThat(tokenizer.getCurrentTokenSpan(), is(1));

        tokenizer.next();
        assertThat(tokenizer.getCurrentToken(), is(IdlToken.COLON));
        assertThat(tokenizer.getCurrentTokenLine(), is(3));
        assertThat(tokenizer.getCurrentTokenColumn(), is(5));
        assertThat(tokenizer.getCurrentTokenSpan(), is(1));

        tokenizer.next();
        assertThat(tokenizer.getCurrentToken(), is(IdlToken.LBRACE));
        assertThat(tokenizer.getCurrentTokenLine(), is(3));
        assertThat(tokenizer.getCurrentTokenColumn(), is(6));
        assertThat(tokenizer.getCurrentTokenSpan(), is(1));

        tokenizer.next();
        assertThat(tokenizer.getCurrentToken(), is(IdlToken.RBRACE));
        assertThat(tokenizer.getCurrentTokenLine(), is(3));
        assertThat(tokenizer.getCurrentTokenColumn(), is(7));
        assertThat(tokenizer.getCurrentTokenSpan(), is(1));

        tokenizer.next();
        assertThat(tokenizer.getCurrentToken(), is(IdlToken.LPAREN));
        assertThat(tokenizer.getCurrentTokenLine(), is(3));
        assertThat(tokenizer.getCurrentTokenColumn(), is(8));
        assertThat(tokenizer.getCurrentTokenSpan(), is(1));

        tokenizer.next();
        assertThat(tokenizer.getCurrentToken(), is(IdlToken.RPAREN));
        assertThat(tokenizer.getCurrentTokenLine(), is(3));
        assertThat(tokenizer.getCurrentTokenColumn(), is(9));
        assertThat(tokenizer.getCurrentTokenSpan(), is(1));

        tokenizer.next();
        assertThat(tokenizer.getCurrentToken(), is(IdlToken.LBRACKET));
        assertThat(tokenizer.getCurrentTokenLine(), is(3));
        assertThat(tokenizer.getCurrentTokenColumn(), is(10));
        assertThat(tokenizer.getCurrentTokenSpan(), is(1));

        tokenizer.next();
        assertThat(tokenizer.getCurrentToken(), is(IdlToken.RBRACKET));
        assertThat(tokenizer.getCurrentTokenLine(), is(3));
        assertThat(tokenizer.getCurrentTokenColumn(), is(11));
        assertThat(tokenizer.getCurrentTokenSpan(), is(1));

        tokenizer.next();
        assertThat(tokenizer.getCurrentToken(), is(IdlToken.POUND));
        assertThat(tokenizer.getCurrentTokenLine(), is(3));
        assertThat(tokenizer.getCurrentTokenColumn(), is(12));
        assertThat(tokenizer.getCurrentTokenSpan(), is(1));

        tokenizer.next();
        assertThat(tokenizer.getCurrentToken(), is(IdlToken.SPACE));
        assertThat(tokenizer.getCurrentTokenLine(), is(3));
        assertThat(tokenizer.getCurrentTokenColumn(), is(13));
        assertThat(tokenizer.getCurrentTokenSpan(), is(1));

        tokenizer.next();
        assertThat(tokenizer.getCurrentToken(), is(IdlToken.EOF));
        assertThat(tokenizer.getCurrentTokenLine(), is(3));
        assertThat(tokenizer.getCurrentTokenColumn(), is(14));
    }

    @Test
    public void tokenizesMultipleSpacesAndTabsIntoSingleLexeme() {
        String contents = "   \t ";
        IdlTokenizer tokenizer = IdlTokenizer.create(contents);

        tokenizer.next();
        assertThat(tokenizer.getCurrentToken(), is(IdlToken.SPACE));
        assertThat(tokenizer.getCurrentTokenLine(), is(1));
        assertThat(tokenizer.getCurrentTokenColumn(), is(1));
        assertThat(tokenizer.getCurrentTokenSpan(), is(5));
        assertThat(tokenizer.getCurrentTokenLexeme().toString(), equalTo("   \t "));

        tokenizer.next();
        assertThat(tokenizer.getCurrentToken(), is(IdlToken.EOF));
        assertThat(tokenizer.getCurrentTokenLine(), is(1));
        assertThat(tokenizer.getCurrentTokenColumn(), is(6));
    }

    @Test
    public void throwsWhenAccessingErrorAndNoError() {
        IdlTokenizer tokenizer = IdlTokenizer.create("a");

        Assertions.assertThrows(ModelSyntaxException.class, tokenizer::getCurrentTokenError);
    }

    @Test
    public void storesErrorForInvalidSyntax() {
        IdlTokenizer tokenizer = IdlTokenizer.create("!");

        assertThat(tokenizer.next(), is(IdlToken.ERROR));
        assertThat(tokenizer.getCurrentTokenError(), equalTo("Unexpected character: '!'"));
    }

    @Test
    public void throwsWhenAccessingNumberAndNoNumber() {
        IdlTokenizer tokenizer = IdlTokenizer.create("a");

        Assertions.assertThrows(ModelSyntaxException.class, tokenizer::getCurrentTokenNumberValue);
    }

    @Test
    public void storesCurrentTokenNumber() {
        IdlTokenizer tokenizer = IdlTokenizer.create("10");

        assertThat(tokenizer.next(), is(IdlToken.NUMBER));
        assertThat(tokenizer.getCurrentTokenNumberValue(), equalTo(10L));
    }

    @Test
    public void throwsWhenAccessingStringValue() {
        IdlTokenizer tokenizer = IdlTokenizer.create("10");

        Assertions.assertThrows(ModelSyntaxException.class, tokenizer::getCurrentTokenStringSlice);
    }

    @Test
    public void storesCurrentTokenString() {
        IdlTokenizer tokenizer = IdlTokenizer.create("\"hello\"");

        assertThat(tokenizer.next(), is(IdlToken.STRING));
        assertThat(tokenizer.getCurrentTokenStringSlice().toString(), equalTo("hello"));
    }

    @Test
    public void storesCurrentTokenStringForIdentifier() {
        IdlTokenizer tokenizer = IdlTokenizer.create("hello");

        assertThat(tokenizer.next(), is(IdlToken.IDENTIFIER));
        assertThat(tokenizer.getCurrentTokenStringSlice().toString(), equalTo("hello"));
    }

    @Test
    public void failsWhenSingleForwardSlashFound() {
        IdlTokenizer tokenizer = IdlTokenizer.create("/");

        tokenizer.next();

        assertThat(tokenizer.getCurrentToken(), is(IdlToken.ERROR));
        assertThat(tokenizer.getCurrentTokenError(), equalTo("Expected a '/' to follow '/' to form a comment."));
    }

    @Test
    public void throwsWhenTraversingPastEof() {
        IdlTokenizer tokenizer = IdlTokenizer.create("");

        assertThat(tokenizer.next(), is(IdlToken.EOF));

        Assertions.assertThrows(NoSuchElementException.class, tokenizer::next);
    }

    @Test
    public void tokenizesStringWithNewlines() {
        IdlTokenizer tokenizer = IdlTokenizer.create("\"hi\nthere\"");

        tokenizer.next();

        assertThat(tokenizer.getCurrentToken(), is(IdlToken.STRING));
        assertThat(tokenizer.getCurrentTokenStringSlice().toString(), equalTo("hi\nthere"));
        assertThat(tokenizer.getCurrentTokenLexeme().toString(), equalTo("\"hi\nthere\""));
        assertThat(tokenizer.getCurrentTokenSpan(), is(10));

        tokenizer.next();
        assertThat(tokenizer.getCurrentToken(), is(IdlToken.EOF));
        assertThat(tokenizer.getCurrentTokenLine(), is(2));
        assertThat(tokenizer.getCurrentTokenColumn(), is(7));
    }

    @Test
    public void tokenizesEmptyStrings() {
        IdlTokenizer tokenizer = IdlTokenizer.create("\"\"");

        tokenizer.next();

        assertThat(tokenizer.getCurrentToken(), is(IdlToken.STRING));
        assertThat(tokenizer.getCurrentTokenStringSlice().toString(), equalTo(""));
        assertThat(tokenizer.getCurrentTokenLexeme().toString(), equalTo("\"\""));
        assertThat(tokenizer.getCurrentTokenSpan(), is(2));
    }
}
