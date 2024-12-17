/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TokenIteratorTest {
    @Test
    public void peeksAndIterates() {
        List<Token> tokens = Arrays.asList(
                new Token(TokenType.DOT, null, 1, 1),
                new Token(TokenType.STAR, null, 1, 2),
                new Token(TokenType.FLATTEN, null, 1, 3));
        TokenIterator iterator = new TokenIterator(tokens);

        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.peek(), equalTo(tokens.get(0)));
        assertThat(iterator.next(), equalTo(tokens.get(0)));

        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.peek(), equalTo(tokens.get(1)));
        assertThat(iterator.next(), equalTo(tokens.get(1)));

        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.peek(), equalTo(tokens.get(2)));
        assertThat(iterator.next(), equalTo(tokens.get(2)));

        assertThat(iterator.hasNext(), is(false));
        assertThat(iterator.peek(), nullValue());
    }

    @Test
    public void throwsWhenNoMoreTokens() {
        TokenIterator iterator = new TokenIterator(Collections.emptyList());
        Assertions.assertThrows(NoSuchElementException.class, iterator::next);
    }

    @Test
    public void peeksAhead() {
        List<Token> tokens = Arrays.asList(
                new Token(TokenType.DOT, null, 1, 1),
                new Token(TokenType.STAR, null, 1, 2),
                new Token(TokenType.FLATTEN, null, 1, 3));
        TokenIterator iterator = new TokenIterator(tokens);

        assertThat(iterator.peek(), equalTo(tokens.get(0)));
        assertThat(iterator.peek(1), equalTo(tokens.get(1)));
        assertThat(iterator.peek(2), equalTo(tokens.get(2)));
        assertThat(iterator.peek(3), nullValue());
    }

    @Test
    public void expectsTokensWithValidResults() {
        List<Token> tokens = Arrays.asList(
                new Token(TokenType.DOT, null, 1, 1),
                new Token(TokenType.STAR, null, 1, 2),
                new Token(TokenType.FLATTEN, null, 1, 3));
        TokenIterator iterator = new TokenIterator(tokens);

        assertThat(iterator.expect(TokenType.DOT), equalTo(tokens.get(0)));
        assertThat(iterator.expect(TokenType.IDENTIFIER, TokenType.STAR), equalTo(tokens.get(1)));
        assertThat(iterator.expect(TokenType.FLATTEN), equalTo(tokens.get(2)));
    }

    @Test
    public void expectsTokensWithInvalidResultBecauseEmpty() {
        TokenIterator iterator = new TokenIterator(Collections.emptyList());
        JmespathException e = Assertions.assertThrows(
                JmespathException.class,
                () -> iterator.expect(TokenType.DOT));

        assertThat(e.getMessage(),
                equalTo("Syntax error at line 1 column 1: Expected '.', but found EOF"));
    }

    @Test
    public void expectsOneOrMoreTokensWithInvalidResultBecauseEmpty() {
        TokenIterator iterator = new TokenIterator(Collections.emptyList());
        JmespathException e = Assertions.assertThrows(
                JmespathException.class,
                () -> iterator.expect(TokenType.DOT, TokenType.EXPREF));

        assertThat(e.getMessage(),
                equalTo("Syntax error at line 1 column 1: Expected ['.', '&'], but found EOF"));
    }

    @Test
    public void expectsTokensWithInvalidResultBecauseEof() {
        List<Token> tokens = Collections.singletonList(new Token(TokenType.DOT, null, 1, 1));
        TokenIterator iterator = new TokenIterator(tokens);
        iterator.next();
        JmespathException e = Assertions.assertThrows(
                JmespathException.class,
                () -> iterator.expect(TokenType.DOT));

        assertThat(e.getMessage(),
                equalTo("Syntax error at line 1 column 1: Expected '.', but found EOF"));
    }

    @Test
    public void expectsOneOrMoreTokensWithInvalidResultBecauseEof() {
        List<Token> tokens = Collections.singletonList(new Token(TokenType.DOT, null, 1, 1));
        TokenIterator iterator = new TokenIterator(tokens);
        iterator.next();
        JmespathException e = Assertions.assertThrows(
                JmespathException.class,
                () -> iterator.expect(TokenType.DOT, TokenType.EXPREF));

        assertThat(e.getMessage(),
                equalTo("Syntax error at line 1 column 1: Expected ['.', '&'], but found EOF"));
    }

    @Test
    public void expectsTokensWithInvalidResultBecauseWrongType() {
        List<Token> tokens = Collections.singletonList(new Token(TokenType.DOT, null, 1, 1));
        TokenIterator iterator = new TokenIterator(tokens);
        JmespathException e = Assertions.assertThrows(
                JmespathException.class,
                () -> iterator.expect(TokenType.STAR));

        assertThat(e.getMessage(),
                equalTo("Syntax error at line 1 column 1: Expected '*', but found '.'"));
    }

    @Test
    public void expectsOneOrMoreTokensWithInvalidResultBecauseWrongType() {
        List<Token> tokens = Collections.singletonList(new Token(TokenType.DOT, null, 1, 1));
        TokenIterator iterator = new TokenIterator(tokens);
        JmespathException e = Assertions.assertThrows(
                JmespathException.class,
                () -> iterator.expect(TokenType.STAR, TokenType.EXPREF));

        assertThat(e.getMessage(),
                equalTo("Syntax error at line 1 column 1: Expected ['*', '&'], but found '.'"));
    }
}
