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
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Function;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TokenizerTest {
    @Test
    public void tokenizesIdentifierFollowedByQuote() {
        String contents = "metadata\"foo\"=\"bar\"";
        IdlTokenizer tokenizer = IdlTokenizer.builder().model(contents).build();

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
        IdlTokenizer tokenizer = IdlTokenizer.builder().model(contents).build();

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
        IdlTokenizer tokenizer = IdlTokenizer.builder().model(contents).build();

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
        IdlTokenizer tokenizer = IdlTokenizer.builder().model("a").build();

        Assertions.assertThrows(ModelSyntaxException.class, tokenizer::getCurrentTokenError);
    }

    @Test
    public void storesErrorForInvalidSyntax() {
        IdlTokenizer tokenizer = IdlTokenizer.builder().model("!").build();

        assertThat(tokenizer.next(), is(IdlToken.ERROR));
        assertThat(tokenizer.getCurrentTokenError(), equalTo("Unexpected character: '!'"));
    }

    @Test
    public void throwsWhenAccessingNumberAndNoNumber() {
        IdlTokenizer tokenizer = IdlTokenizer.builder().model("a").build();

        Assertions.assertThrows(ModelSyntaxException.class, tokenizer::getCurrentTokenNumberValue);
    }

    @Test
    public void storesCurrentTokenNumber() {
        IdlTokenizer tokenizer = IdlTokenizer.builder().model("10").build();

        assertThat(tokenizer.next(), is(IdlToken.NUMBER));
        assertThat(tokenizer.getCurrentTokenNumberValue(), equalTo(10L));
    }

    @Test
    public void throwsWhenAccessingStringValue() {
        IdlTokenizer tokenizer = IdlTokenizer.builder().model("10").build();

        Assertions.assertThrows(ModelSyntaxException.class, tokenizer::getCurrentTokenStringSlice);
    }

    @Test
    public void storesCurrentTokenString() {
        IdlTokenizer tokenizer = IdlTokenizer.builder().model("\"hello\"").build();

        assertThat(tokenizer.next(), is(IdlToken.STRING));
        assertThat(tokenizer.getCurrentTokenStringSlice().toString(), equalTo("hello"));
    }

    @Test
    public void storesCurrentTokenStringForIdentifier() {
        IdlTokenizer tokenizer = IdlTokenizer.builder().model("hello").build();

        assertThat(tokenizer.next(), is(IdlToken.IDENTIFIER));
        assertThat(tokenizer.getCurrentTokenStringSlice().toString(), equalTo("hello"));
    }

    @Test
    public void throwsWhenTooNested() {
        IdlTokenizer tokenizer = IdlTokenizer.builder().model("").build();

        for (int i = 0; i < 64; i++) {
            tokenizer.increaseNestingLevel();
        }

        ModelSyntaxException e = Assertions.assertThrows(ModelSyntaxException.class, tokenizer::increaseNestingLevel);

        assertThat(e.getMessage(),
                   startsWith("Syntax error at line 1, column 1: Parser exceeded maximum allowed depth of 64"));
    }

    @Test
    public void tokenizerInternsStrings() {
        List<CharSequence> tracked = new ArrayList<>();
        Function<CharSequence, String> table = c -> {
            tracked.add(c);
            return c.toString();
        };

        IdlTokenizer tokenizer = IdlTokenizer.builder()
                .model("foo")
                .stringTable(table)
                .build();

        tokenizer.internString("hi");

        assertThat(tracked, contains("hi"));
    }

    @Test
    public void skipSpaces() {
        IdlTokenizer tokenizer = IdlTokenizer.builder().model("    hi").build();

        tokenizer.skipSpaces();

        assertThat(tokenizer.getCurrentToken(), is(IdlToken.IDENTIFIER));
        assertThat(tokenizer.getCurrentTokenColumn(), is(5));
    }

    @Test
    public void skipsExpectedSpaces() {
        IdlTokenizer tokenizer = IdlTokenizer.builder().model("    hi").build();

        tokenizer.expectAndSkipSpaces();

        assertThat(tokenizer.getCurrentToken(), is(IdlToken.IDENTIFIER));
        assertThat(tokenizer.getCurrentTokenColumn(), is(5));
    }

    @Test
    public void failsWhenExpectedSpacesNotThere() {
        IdlTokenizer tokenizer = IdlTokenizer.builder().model("abc").build();

        ModelSyntaxException e = Assertions.assertThrows(ModelSyntaxException.class, tokenizer::expectAndSkipSpaces);

        assertThat(e.getMessage(), startsWith("Syntax error at line 1, column 1: Expected one or more spaces, but "
                                              + "found IDENTIFIER('abc')"));
    }

    @Test
    public void skipWhitespace() {
        IdlTokenizer tokenizer = IdlTokenizer.builder().model(" \n\n hi").build();

        tokenizer.skipWs();

        assertThat(tokenizer.getCurrentToken(), is(IdlToken.IDENTIFIER));
        assertThat(tokenizer.getCurrentTokenLine(), is(3));
        assertThat(tokenizer.getCurrentTokenColumn(), is(2));
    }

    @Test
    public void expectAndSkipWhitespace() {
        IdlTokenizer tokenizer = IdlTokenizer.builder().model(" \n\n hi").build();

        tokenizer.expectAndSkipWhitespace();

        assertThat(tokenizer.getCurrentToken(), is(IdlToken.IDENTIFIER));
        assertThat(tokenizer.getCurrentTokenLine(), is(3));
        assertThat(tokenizer.getCurrentTokenColumn(), is(2));
    }

    @Test
    public void throwsWhenExpectedWhitespaceNotFound() {
        IdlTokenizer tokenizer = IdlTokenizer.builder().model("hi").build();

        ModelSyntaxException e = Assertions.assertThrows(ModelSyntaxException.class,
                                                         tokenizer::expectAndSkipWhitespace);

        assertThat(e.getMessage(), startsWith("Syntax error at line 1, column 1: Expected one or more whitespace "
                                              + "characters, but found IDENTIFIER('hi')"));
    }

    @Test
    public void expectsAndSkipsBr() {
        IdlTokenizer tokenizer = IdlTokenizer.builder().model("\n  Hi").build();

        tokenizer.expectAndSkipBr();

        assertThat(tokenizer.getCurrentToken(), is(IdlToken.IDENTIFIER));
        assertThat(tokenizer.getCurrentTokenLine(), is(2));
        assertThat(tokenizer.getCurrentTokenColumn(), is(3));
    }

    @Test
    public void throwsWhenBrNotFound() {
        IdlTokenizer tokenizer = IdlTokenizer.builder().model("Hi").build();

        ModelSyntaxException e = Assertions.assertThrows(ModelSyntaxException.class,
                                                         tokenizer::expectAndSkipBr);

        assertThat(e.getMessage(), startsWith("Syntax error at line 1, column 1: Expected a line break, but "
                                              + "found IDENTIFIER('Hi')"));
    }

    @Test
    public void expectCurrentTokenLexeme() {
        IdlTokenizer tokenizer = IdlTokenizer.builder().model("Hi").build();

        tokenizer.expectCurrentLexeme("Hi");
    }

    @Test
    public void throwsWhenCurrentTokenLexemeUnexpected() {
        IdlTokenizer tokenizer = IdlTokenizer.builder().model("Hi").build();

        ModelSyntaxException e = Assertions.assertThrows(ModelSyntaxException.class, () -> {
            tokenizer.expectCurrentLexeme("Bye");
        });

        assertThat(e.getMessage(), startsWith("Syntax error at line 1, column 1: Expected `Bye`, but found `Hi`"));
        assertThat(e.getSourceLocation().getLine(), is(1));
        assertThat(e.getSourceLocation().getColumn(), is(1));
    }

    @Test
    public void throwsWhenCurrentTokenLexemeUnexpectedAndSameLength() {
        IdlTokenizer tokenizer = IdlTokenizer.builder().model("A").build();

        ModelSyntaxException e = Assertions.assertThrows(ModelSyntaxException.class, () -> {
            tokenizer.expectCurrentLexeme("B");
        });

        assertThat(e.getMessage(), startsWith("Syntax error at line 1, column 1: Expected `B`, but found `A`"));
        assertThat(e.getSourceLocation().getLine(), is(1));
        assertThat(e.getSourceLocation().getColumn(), is(1));
    }

    @Test
    public void tokenDoesNotStartWith() {
        IdlTokenizer tokenizer = IdlTokenizer.builder().model("Hi").build();

        assertThat(tokenizer.doesCurrentIdentifierStartWith('B'), is(false));
    }

    @Test
    public void tokenDoesStartWith() {
        IdlTokenizer tokenizer = IdlTokenizer.builder().model("Hi").build();

        assertThat(tokenizer.doesCurrentIdentifierStartWith('H'), is(true));
    }

    @Test
    public void tokenDoesNotStartWithBecauseNotIdentifier() {
        IdlTokenizer tokenizer = IdlTokenizer.builder().model("\"Hi\"").build();

        assertThat(tokenizer.doesCurrentIdentifierStartWith('H'), is(false));
    }

    @Test
    public void expectsSingleTokenType() {
        IdlTokenizer tokenizer = IdlTokenizer.builder().model("Hi").build();

        tokenizer.expect(IdlToken.IDENTIFIER);
    }

    @Test
    public void failsForSingleExpectedToken() {
        IdlTokenizer tokenizer = IdlTokenizer.builder().model("Hi").build();

        ModelSyntaxException e = Assertions.assertThrows(ModelSyntaxException.class,
                                                         () -> tokenizer.expect(IdlToken.NUMBER));

        assertThat(e.getMessage(), startsWith("Syntax error at line 1, column 1: Expected NUMBER but "
                                              + "found IDENTIFIER('Hi')"));
    }

    @Test
    public void expectsMultipleTokenTypes() {
        IdlTokenizer tokenizer = IdlTokenizer.builder().model("Hi").build();

        tokenizer.expect(IdlToken.STRING, IdlToken.IDENTIFIER);
    }

    @Test
    public void failsForMultipleExpectedTokens() {
        IdlTokenizer tokenizer = IdlTokenizer.builder().model("Hi").build();

        ModelSyntaxException e = Assertions.assertThrows(ModelSyntaxException.class,
                                                         () -> tokenizer.expect(IdlToken.NUMBER, IdlToken.LBRACE));

        assertThat(e.getMessage(), startsWith("Syntax error at line 1, column 1: Expected one of NUMBER, LBRACE('{'); "
                                              + "but found IDENTIFIER('Hi')"));
    }

    @Test
    public void failsWhenSingleForwardSlashFound() {
        IdlTokenizer tokenizer = IdlTokenizer.builder().model("/").build();

        tokenizer.next();

        assertThat(tokenizer.getCurrentToken(), is(IdlToken.ERROR));
        assertThat(tokenizer.getCurrentTokenError(), equalTo("Expected a '/' to follow '/' to form a comment."));
    }

    @Test
    public void throwsWhenTraversingPastEof() {
        IdlTokenizer tokenizer = IdlTokenizer.builder().model("").build();

        assertThat(tokenizer.next(), is(IdlToken.EOF));

        Assertions.assertThrows(NoSuchElementException.class, tokenizer::next);
    }

    @Test
    public void parsesDocs() {
        IdlTokenizer tokenizer = IdlTokenizer
                .builder()
                .model("/// Hi\n"
                       + "/// There\n"
                       + "/// 123\n"
                       + "/// 456\n")
                .build();

        String docs = tokenizer.expectAndParseDocs().toString();

        assertThat(docs, equalTo("Hi\nThere\n123\n456"));
    }

    @Test
    public void tokenizesStringWithNewlines() {
        IdlTokenizer tokenizer = IdlTokenizer.builder().model("\"hi\nthere\"").build();

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
        IdlTokenizer tokenizer = IdlTokenizer.builder().model("\"\"").build();

        tokenizer.next();

        assertThat(tokenizer.getCurrentToken(), is(IdlToken.STRING));
        assertThat(tokenizer.getCurrentTokenStringSlice().toString(), equalTo(""));
        assertThat(tokenizer.getCurrentTokenLexeme().toString(), equalTo("\"\""));
        assertThat(tokenizer.getCurrentTokenSpan(), is(2));
    }
}
