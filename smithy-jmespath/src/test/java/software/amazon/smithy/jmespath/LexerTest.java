/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class LexerTest {

    private List<Token> tokenize(String expression) {
        TokenIterator iterator = Lexer.tokenize(expression);
        List<Token> tokens = new ArrayList<>();
        while (iterator.hasNext()) {
            tokens.add(iterator.next());
        }
        return tokens;
    }

    @Test
    public void tokenizesField() {
        TokenIterator tokens = Lexer.tokenize("foo_123_FOO");

        Token token = tokens.next();
        assertThat(token.type, equalTo(TokenType.IDENTIFIER));
        assertThat(token.value.expectStringValue(), equalTo("foo_123_FOO"));
        assertThat(token.line, equalTo(1));
        assertThat(token.column, equalTo(1));

        token = tokens.next();
        assertThat(token.type, equalTo(TokenType.EOF));
        assertThat(token.line, equalTo(1));
        assertThat(token.column, equalTo(12));

        assertThat(tokens.hasNext(), is(false));
    }

    @Test
    public void tokenizesSubexpression() {
        List<Token> tokens = tokenize("foo.bar");

        assertThat(tokens, hasSize(4));
        assertThat(tokens.get(0).type, equalTo(TokenType.IDENTIFIER));
        assertThat(tokens.get(0).value.expectStringValue(), equalTo("foo"));
        assertThat(tokens.get(0).line, equalTo(1));
        assertThat(tokens.get(0).column, equalTo(1));

        assertThat(tokens.get(1).type, equalTo(TokenType.DOT));
        assertThat(tokens.get(1).line, equalTo(1));
        assertThat(tokens.get(1).column, equalTo(4));

        assertThat(tokens.get(2).type, equalTo(TokenType.IDENTIFIER));
        assertThat(tokens.get(2).value.expectStringValue(), equalTo("bar"));
        assertThat(tokens.get(2).line, equalTo(1));
        assertThat(tokens.get(2).column, equalTo(5));

        assertThat(tokens.get(3).type, equalTo(TokenType.EOF));
        assertThat(tokens.get(3).line, equalTo(1));
        assertThat(tokens.get(3).column, equalTo(8));
    }

    @Test
    public void tokenizesJsonArray() {
        List<Token> tokens = tokenize("` [ 1 , true , false , null ,  -2  ,  \"hi\"   ]  `");

        assertThat(tokens, hasSize(2));
        assertThat(tokens.get(0).type, equalTo(TokenType.LITERAL));
        assertThat(tokens.get(0).value.expectArrayValue(), equalTo(Arrays.asList(1.0, true, false, null, -2.0, "hi")));
        assertThat(tokens.get(0).line, equalTo(1));
        assertThat(tokens.get(0).column, equalTo(1));

        assertThat(tokens.get(1).type, equalTo(TokenType.EOF));
        assertThat(tokens.get(1).line, equalTo(1));
        assertThat(tokens.get(1).column, equalTo(50));
    }

    @Test
    public void doesNotEatTrailingLiteralTick() {
        Assertions.assertThrows(JmespathException.class, () -> tokenize("`true``"));
    }

    @Test
    public void tokenizesEmptyJsonArray() {
        List<Token> tokens = tokenize("`[]`");

        assertThat(tokens, hasSize(2));
        assertThat(tokens.get(0).type, equalTo(TokenType.LITERAL));
        assertThat(tokens.get(0).value.expectArrayValue(), empty());
        assertThat(tokens.get(0).line, equalTo(1));
        assertThat(tokens.get(0).column, equalTo(1));

        assertThat(tokens.get(1).type, equalTo(TokenType.EOF));
        assertThat(tokens.get(1).line, equalTo(1));
        assertThat(tokens.get(1).column, equalTo(5));
    }

    @Test
    public void findsUnclosedJsonArrays() {
        JmespathException e = Assertions.assertThrows(JmespathException.class, () -> Lexer.tokenize("`[`"));

        assertThat(e.getMessage(), containsString("Unclosed JSON array"));
    }

    @Test
    public void findsUnclosedJsonArrayLiteral() {
        JmespathException e = Assertions.assertThrows(JmespathException.class, () -> Lexer.tokenize("`["));

        assertThat(e.getMessage(), containsString("Unclosed JSON array"));
    }

    @Test
    public void doesNotSupportTrailingJsonArrayCommas() {
        Assertions.assertThrows(JmespathException.class, () -> Lexer.tokenize("`[1,]"));
    }

    @Test
    public void detectsJsonArraySyntaxError() {
        Assertions.assertThrows(JmespathException.class, () -> Lexer.tokenize("`[:]"));
    }

    @Test
    public void tokenizesJsonObject() {
        List<Token> tokens = tokenize("`{\"foo\": true,\"bar\" : { \"bam\": []   }  }`");

        assertThat(tokens, hasSize(2));
        assertThat(tokens.get(0).type, equalTo(TokenType.LITERAL));
        Map<String, Object> obj = tokens.get(0).value.expectObjectValue();
        assertThat(obj.entrySet(), hasSize(2));
        assertThat(obj.keySet(), contains("foo", "bar"));
        assertThat(obj.get("foo"), equalTo(true));
        assertThat(obj.get("bar"), equalTo(Collections.singletonMap("bam", Collections.emptyList())));
        assertThat(tokens.get(0).line, equalTo(1));
        assertThat(tokens.get(0).column, equalTo(1));

        assertThat(tokens.get(1).type, equalTo(TokenType.EOF));
        assertThat(tokens.get(1).line, equalTo(1));
        assertThat(tokens.get(1).column, equalTo(42));
    }

    @Test
    public void tokenizesEmptyJsonObject() {
        List<Token> tokens = tokenize("`{}`");

        assertThat(tokens, hasSize(2));
        assertThat(tokens.get(0).type, equalTo(TokenType.LITERAL));
        assertThat(tokens.get(0).value.expectObjectValue().entrySet(), empty());
        assertThat(tokens.get(0).line, equalTo(1));
        assertThat(tokens.get(0).column, equalTo(1));

        assertThat(tokens.get(1).type, equalTo(TokenType.EOF));
        assertThat(tokens.get(1).line, equalTo(1));
        assertThat(tokens.get(1).column, equalTo(5));
    }

    @Test
    public void findsUnclosedJsonObjects() {
        JmespathException e = Assertions.assertThrows(JmespathException.class, () -> Lexer.tokenize("`{`"));

        assertThat(e.getMessage(), containsString("Unclosed JSON object"));
    }

    @Test
    public void findsUnclosedJsonObjectLiteral() {
        JmespathException e = Assertions.assertThrows(JmespathException.class, () -> Lexer.tokenize("`{"));

        assertThat(e.getMessage(), containsString("Unclosed JSON object"));
    }

    @Test
    public void doesNotSupportTrailingJsonObjectCommas() {
        Assertions.assertThrows(JmespathException.class, () -> Lexer.tokenize("`{\"foo\": true,}"));
    }

    @Test
    public void detectsJsonObjectSyntaxError() {
        Assertions.assertThrows(JmespathException.class, () -> Lexer.tokenize("`{true:true}"));
    }

    @Test
    public void defendsAgainstTooMuchRecursionInObjects() {
        StringBuilder text = new StringBuilder("`");
        for (int i = 0; i < 100; i++) {
            text.append('{');
        }

        Assertions.assertThrows(JmespathException.class, () -> Lexer.tokenize(text.toString()));
    }

    @Test
    public void defendsAgainstTooMuchRecursionInArrays() {
        StringBuilder text = new StringBuilder("`");
        for (int i = 0; i < 100; i++) {
            text.append('[');
        }

        Assertions.assertThrows(JmespathException.class, () -> Lexer.tokenize(text.toString()));
    }

    @Test
    public void canEscapeTicksInJsonLiteralStrings() {
        List<Token> tokens = tokenize("`\"\\`\"`");

        assertThat(tokens, hasSize(2));
        assertThat(tokens.get(0).type, equalTo(TokenType.LITERAL));
        assertThat(tokens.get(0).value.expectStringValue(), equalTo("`"));
        assertThat(tokens.get(0).line, equalTo(1));
        assertThat(tokens.get(0).column, equalTo(1));

        assertThat(tokens.get(1).type, equalTo(TokenType.EOF));
        assertThat(tokens.get(1).line, equalTo(1));
        assertThat(tokens.get(1).column, equalTo(7));
    }

    @Test
    public void cannotEscapeTicksOutsideOfJsonLiteral() {
        JmespathException e = Assertions.assertThrows(JmespathException.class, () -> Lexer.tokenize("\"\\`\""));

        assertThat(e.getMessage(), containsString("Invalid escape: `"));
    }

    @Test
    public void parsesQuotedString() {
        List<Token> tokens = tokenize("\"foo\" \"\"");

        assertThat(tokens, hasSize(3));
        assertThat(tokens.get(0).type, equalTo(TokenType.IDENTIFIER));
        assertThat(tokens.get(0).value.expectStringValue(), equalTo("foo"));
        assertThat(tokens.get(0).line, equalTo(1));
        assertThat(tokens.get(0).column, equalTo(1));

        assertThat(tokens.get(1).type, equalTo(TokenType.IDENTIFIER));
        assertThat(tokens.get(1).value.expectStringValue(), equalTo(""));
        assertThat(tokens.get(1).line, equalTo(1));
        assertThat(tokens.get(1).column, equalTo(7));

        assertThat(tokens.get(2).type, equalTo(TokenType.EOF));
        assertThat(tokens.get(2).line, equalTo(1));
        assertThat(tokens.get(2).column, equalTo(9));
    }

    @Test
    public void detectsUnclosedQuotes() {
        Assertions.assertThrows(JmespathException.class, () -> Lexer.tokenize("\""));
    }

    @Test
    public void parsesQuotedStringEscapes() {
        List<Token> tokens = tokenize("\"\\\" \\n \\t \\r \\f \\b \\/ \\\\ \"");

        assertThat(tokens, hasSize(2));
        assertThat(tokens.get(0).type, equalTo(TokenType.IDENTIFIER));
        assertThat(tokens.get(0).value.expectStringValue(), equalTo("\" \n \t \r \f \b / \\ "));
        assertThat(tokens.get(0).line, equalTo(1));
        assertThat(tokens.get(0).column, equalTo(1));

        assertThat(tokens.get(1).type, equalTo(TokenType.EOF));
        assertThat(tokens.get(1).line, equalTo(1));
        assertThat(tokens.get(1).column, equalTo(27));
    }

    @Test
    public void parsesQuotedStringValidHex() {
        List<Token> tokens = tokenize("\"\\u000A\\u000a\"");

        assertThat(tokens, hasSize(2));
        assertThat(tokens.get(0).type, equalTo(TokenType.IDENTIFIER));
        assertThat(tokens.get(0).value.expectStringValue(), equalTo("\n\n"));
        assertThat(tokens.get(0).line, equalTo(1));
        assertThat(tokens.get(0).column, equalTo(1));

        assertThat(tokens.get(1).type, equalTo(TokenType.EOF));
        assertThat(tokens.get(1).line, equalTo(1));
        assertThat(tokens.get(1).column, equalTo(15));
    }

    @Test
    public void detectsTooShortHex() {
        Assertions.assertThrows(JmespathException.class, () -> Lexer.tokenize("\"\\u0A\""));
    }

    @Test
    public void detectsInvalidHex() {
        Assertions.assertThrows(JmespathException.class, () -> Lexer.tokenize("\"\\u0L\""));
    }

    @Test
    public void parsesLbrackets() {
        List<Token> tokens = tokenize("[? [] [");

        assertThat(tokens, hasSize(4));
        assertThat(tokens.get(0).type, equalTo(TokenType.FILTER));
        assertThat(tokens.get(0).line, equalTo(1));
        assertThat(tokens.get(0).column, equalTo(1));

        assertThat(tokens.get(1).type, equalTo(TokenType.FLATTEN));
        assertThat(tokens.get(1).line, equalTo(1));
        assertThat(tokens.get(1).column, equalTo(4));

        assertThat(tokens.get(2).type, equalTo(TokenType.LBRACKET));
        assertThat(tokens.get(2).line, equalTo(1));
        assertThat(tokens.get(2).column, equalTo(7));

        assertThat(tokens.get(3).type, equalTo(TokenType.EOF));
        assertThat(tokens.get(3).line, equalTo(1));
        assertThat(tokens.get(3).column, equalTo(8));
    }

    @Test
    public void parsesStar() {
        List<Token> tokens = tokenize("*");

        assertThat(tokens, hasSize(2));
        assertThat(tokens.get(0).type, equalTo(TokenType.STAR));
        assertThat(tokens.get(0).line, equalTo(1));
        assertThat(tokens.get(0).column, equalTo(1));
    }

    @Test
    public void parsesPipeAndOr() {
        List<Token> tokens = tokenize("| ||");

        assertThat(tokens, hasSize(3));
        assertThat(tokens.get(0).type, equalTo(TokenType.PIPE));
        assertThat(tokens.get(0).line, equalTo(1));
        assertThat(tokens.get(0).column, equalTo(1));

        assertThat(tokens.get(1).type, equalTo(TokenType.OR));
        assertThat(tokens.get(1).line, equalTo(1));
        assertThat(tokens.get(1).column, equalTo(3));
    }

    @Test
    public void parsesAt() {
        List<Token> tokens = tokenize("@");

        assertThat(tokens, hasSize(2));
        assertThat(tokens.get(0).type, equalTo(TokenType.CURRENT));
        assertThat(tokens.get(0).line, equalTo(1));
        assertThat(tokens.get(0).column, equalTo(1));
    }

    @Test
    public void parsesRbracketLbraceRbrace() {
        List<Token> tokens = tokenize("]{}");

        assertThat(tokens, hasSize(4));
        assertThat(tokens.get(0).type, equalTo(TokenType.RBRACKET));
        assertThat(tokens.get(0).line, equalTo(1));
        assertThat(tokens.get(0).column, equalTo(1));

        assertThat(tokens.get(1).type, equalTo(TokenType.LBRACE));
        assertThat(tokens.get(1).line, equalTo(1));
        assertThat(tokens.get(1).column, equalTo(2));

        assertThat(tokens.get(2).type, equalTo(TokenType.RBRACE));
        assertThat(tokens.get(2).line, equalTo(1));
        assertThat(tokens.get(2).column, equalTo(3));
    }

    @Test
    public void parsesAmpersand() {
        List<Token> tokens = tokenize("&");

        assertThat(tokens, hasSize(2));
        assertThat(tokens.get(0).type, equalTo(TokenType.EXPREF));
        assertThat(tokens.get(0).line, equalTo(1));
        assertThat(tokens.get(0).column, equalTo(1));
    }

    @Test
    public void parsesParens() {
        List<Token> tokens = tokenize("()");

        assertThat(tokens, hasSize(3));
        assertThat(tokens.get(0).type, equalTo(TokenType.LPAREN));
        assertThat(tokens.get(0).line, equalTo(1));
        assertThat(tokens.get(0).column, equalTo(1));

        assertThat(tokens.get(1).type, equalTo(TokenType.RPAREN));
        assertThat(tokens.get(1).line, equalTo(1));
        assertThat(tokens.get(1).column, equalTo(2));
    }

    @Test
    public void parsesCommasAndColons() {
        List<Token> tokens = tokenize(",:");

        assertThat(tokens, hasSize(3));
        assertThat(tokens.get(0).type, equalTo(TokenType.COMMA));
        assertThat(tokens.get(0).line, equalTo(1));
        assertThat(tokens.get(0).column, equalTo(1));

        assertThat(tokens.get(1).type, equalTo(TokenType.COLON));
        assertThat(tokens.get(1).line, equalTo(1));
        assertThat(tokens.get(1).column, equalTo(2));
    }

    @Test
    public void parsesValidEquals() {
        List<Token> tokens = tokenize("==");

        assertThat(tokens, hasSize(2));
        assertThat(tokens.get(0).type, equalTo(TokenType.EQUAL));
        assertThat(tokens.get(0).line, equalTo(1));
        assertThat(tokens.get(0).column, equalTo(1));

        assertThat(tokens.get(1).type, equalTo(TokenType.EOF));
        assertThat(tokens.get(1).line, equalTo(1));
        assertThat(tokens.get(1).column, equalTo(3));
    }

    @Test
    public void parsesInvalidEquals() {
        Assertions.assertThrows(JmespathException.class, () -> Lexer.tokenize("="));
    }

    @Test
    public void parsesGtLtNot() {
        List<Token> tokens = tokenize("> >= < <= ! !=");

        assertThat(tokens, hasSize(7));
        assertThat(tokens.get(0).type, equalTo(TokenType.GREATER_THAN));
        assertThat(tokens.get(0).line, equalTo(1));
        assertThat(tokens.get(0).column, equalTo(1));

        assertThat(tokens.get(1).type, equalTo(TokenType.GREATER_THAN_EQUAL));
        assertThat(tokens.get(1).line, equalTo(1));
        assertThat(tokens.get(1).column, equalTo(3));

        assertThat(tokens.get(2).type, equalTo(TokenType.LESS_THAN));
        assertThat(tokens.get(2).line, equalTo(1));
        assertThat(tokens.get(2).column, equalTo(6));

        assertThat(tokens.get(3).type, equalTo(TokenType.LESS_THAN_EQUAL));
        assertThat(tokens.get(3).line, equalTo(1));
        assertThat(tokens.get(3).column, equalTo(8));

        assertThat(tokens.get(4).type, equalTo(TokenType.NOT));
        assertThat(tokens.get(4).line, equalTo(1));
        assertThat(tokens.get(4).column, equalTo(11));

        assertThat(tokens.get(5).type, equalTo(TokenType.NOT_EQUAL));
        assertThat(tokens.get(5).line, equalTo(1));
        assertThat(tokens.get(5).column, equalTo(13));

        assertThat(tokens.get(6).type, equalTo(TokenType.EOF));
        assertThat(tokens.get(6).line, equalTo(1));
        assertThat(tokens.get(6).column, equalTo(15));
    }

    @Test
    public void parsesNumbers() {
        List<Token> tokens = tokenize("123 -1 0.0");

        assertThat(tokens, hasSize(4));
        assertThat(tokens.get(0).type, equalTo(TokenType.NUMBER));
        assertThat(tokens.get(0).value.expectNumberValue().doubleValue(), equalTo(123.0));
        assertThat(tokens.get(0).line, equalTo(1));
        assertThat(tokens.get(0).column, equalTo(1));

        assertThat(tokens.get(1).type, equalTo(TokenType.NUMBER));
        assertThat(tokens.get(1).value.expectNumberValue().doubleValue(), equalTo(-1.0));
        assertThat(tokens.get(1).line, equalTo(1));
        assertThat(tokens.get(1).column, equalTo(5));

        assertThat(tokens.get(2).type, equalTo(TokenType.NUMBER));
        assertThat(tokens.get(2).value.expectNumberValue().doubleValue(), equalTo(0.0));
        assertThat(tokens.get(2).line, equalTo(1));
        assertThat(tokens.get(2).column, equalTo(8));

        assertThat(tokens.get(3).type, equalTo(TokenType.EOF));
        assertThat(tokens.get(3).line, equalTo(1));
        assertThat(tokens.get(3).column, equalTo(11));
    }

    @Test
    public void negativeMustBeFollowedByDigit() {
        Assertions.assertThrows(JmespathException.class, () -> Lexer.tokenize("-"));
    }

    @Test
    public void decimalMustBeFollowedByDigit() {
        Assertions.assertThrows(JmespathException.class, () -> Lexer.tokenize("0.a"));
    }

    @Test
    public void exponentMustBeFollowedByDigit() {
        Assertions.assertThrows(JmespathException.class, () -> Lexer.tokenize("0.0ea"));
    }

    @Test
    public void ignoresNonNumericExponents() {
        List<Token> tokens = tokenize("0.0a");

        assertThat(tokens, hasSize(3));
        assertThat(tokens.get(0).type, equalTo(TokenType.NUMBER));
        assertThat(tokens.get(0).value.expectNumberValue().doubleValue(), equalTo(0.0));
        assertThat(tokens.get(0).line, equalTo(1));
        assertThat(tokens.get(0).column, equalTo(1));

        assertThat(tokens.get(1).type, equalTo(TokenType.IDENTIFIER));
        assertThat(tokens.get(1).value.expectStringValue(), equalTo("a"));
        assertThat(tokens.get(1).line, equalTo(1));
        assertThat(tokens.get(1).column, equalTo(4));

        assertThat(tokens.get(2).type, equalTo(TokenType.EOF));
        assertThat(tokens.get(2).line, equalTo(1));
        assertThat(tokens.get(2).column, equalTo(5));
    }

    @Test
    public void parsesComplexNumbers() {
        List<Token> tokens = tokenize("123.009e+12 -001.109E-12");

        assertThat(tokens, hasSize(3));
        assertThat(tokens.get(0).type, equalTo(TokenType.NUMBER));
        assertThat(tokens.get(0).value.expectNumberValue().doubleValue(), equalTo(123.009e12));
        assertThat(tokens.get(0).line, equalTo(1));
        assertThat(tokens.get(0).column, equalTo(1));

        assertThat(tokens.get(1).type, equalTo(TokenType.NUMBER));
        assertThat(tokens.get(1).value.expectNumberValue().doubleValue(), equalTo(-001.109e-12));
        assertThat(tokens.get(1).line, equalTo(1));
        assertThat(tokens.get(1).column, equalTo(13));

        assertThat(tokens.get(2).type, equalTo(TokenType.EOF));
        assertThat(tokens.get(2).line, equalTo(1));
        assertThat(tokens.get(2).column, equalTo(25));
    }

    @Test
    public void detectsTopLevelInvalidSyntax() {
        Assertions.assertThrows(JmespathException.class, () -> Lexer.tokenize("~"));
    }

    @Test
    public void parsesRawStringLiteral() {
        List<Token> tokens = tokenize("'foo' 'foo\\'s' 'foo\\a'");

        assertThat(tokens, hasSize(4));
        assertThat(tokens.get(0).type, equalTo(TokenType.LITERAL));
        assertThat(tokens.get(0).value.expectStringValue(), equalTo("foo"));
        assertThat(tokens.get(0).line, equalTo(1));
        assertThat(tokens.get(0).column, equalTo(1));

        assertThat(tokens.get(1).type, equalTo(TokenType.LITERAL));
        assertThat(tokens.get(1).value.expectStringValue(), equalTo("foo's"));
        assertThat(tokens.get(1).line, equalTo(1));
        assertThat(tokens.get(1).column, equalTo(7));

        assertThat(tokens.get(2).type, equalTo(TokenType.LITERAL));
        assertThat(tokens.get(2).value.expectStringValue(), equalTo("foo\\a"));
        assertThat(tokens.get(2).line, equalTo(1));
        assertThat(tokens.get(2).column, equalTo(16));

        assertThat(tokens.get(3).type, equalTo(TokenType.EOF));
        assertThat(tokens.get(3).line, equalTo(1));
        assertThat(tokens.get(3).column, equalTo(23));
    }

    @Test
    public void parsesEmptyRawString() {
        List<Token> tokens = tokenize("''");

        assertThat(tokens, hasSize(2));
        assertThat(tokens.get(0).type, equalTo(TokenType.LITERAL));
        assertThat(tokens.get(0).value.expectStringValue(), equalTo(""));
        assertThat(tokens.get(0).line, equalTo(1));
        assertThat(tokens.get(0).column, equalTo(1));

        assertThat(tokens.get(1).type, equalTo(TokenType.EOF));
        assertThat(tokens.get(1).line, equalTo(1));
        assertThat(tokens.get(1).column, equalTo(3));
    }

    @Test
    public void detectsUnclosedRawString() {
        Assertions.assertThrows(JmespathException.class, () -> Lexer.tokenize("'foo"));
    }

    @Test
    public void convertsLexemeTokensToString() {
        List<Token> tokens = tokenize("abc . : 10");

        assertThat(tokens.get(0).toString(), equalTo("'abc'"));
        assertThat(tokens.get(1).toString(), equalTo("'.'"));
        assertThat(tokens.get(2).toString(), equalTo("':'"));
        assertThat(tokens.get(3).toString(), equalTo("'10.0'"));
    }

    @Test
    public void tracksLineAndColumn() {
        List<Token> tokens = tokenize(" abc\n .\n:\n10\r\na\rb");

        assertThat(tokens.get(0).line, is(1));
        assertThat(tokens.get(0).column, is(2));

        assertThat(tokens.get(1).line, is(2));
        assertThat(tokens.get(1).column, is(2));

        assertThat(tokens.get(2).line, is(3));
        assertThat(tokens.get(2).column, is(1));

        assertThat(tokens.get(3).toString(), equalTo("'10.0'"));
        assertThat(tokens.get(3).line, is(4));
        assertThat(tokens.get(3).column, is(1));

        assertThat(tokens.get(4).toString(), equalTo("'a'"));
        assertThat(tokens.get(4).line, is(5));
        assertThat(tokens.get(4).column, is(1));

        assertThat(tokens.get(5).toString(), equalTo("'b'"));
        assertThat(tokens.get(5).line, is(6));
        assertThat(tokens.get(5).column, is(1));

        assertThat(tokens.get(6).line, is(6));
        assertThat(tokens.get(6).column, is(2));
    }

    @Test
    public void tokenizesRawStrings() {
        List<Token> tokens = tokenize("starts_with(@, 'foo')");

        assertThat(tokens.get(0).type, is(TokenType.IDENTIFIER));
        assertThat(tokens.get(1).type, is(TokenType.LPAREN));
        assertThat(tokens.get(2).type, is(TokenType.CURRENT));
        assertThat(tokens.get(3).type, is(TokenType.COMMA));
        assertThat(tokens.get(4).type, is(TokenType.LITERAL));
        assertThat(tokens.get(5).type, is(TokenType.RPAREN));
        assertThat(tokens.get(6).type, is(TokenType.EOF));
    }

    @Test
    public void tokenizesQuotedIdentifier() {
        List<Token> tokens = tokenize("\"1\"");

        assertThat(tokens.get(0).type, is(TokenType.IDENTIFIER));
        assertThat(tokens.get(1).type, is(TokenType.EOF));
    }
}
