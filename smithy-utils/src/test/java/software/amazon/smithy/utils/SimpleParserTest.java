/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SimpleParserTest {

    @Test
    public void simpleParserBasics() {
        SimpleParser p = new SimpleParser("foo", 10);

        assertThat(p.input().toString(), equalTo("foo"));
        assertThat(p.line(), equalTo(1));
        assertThat(p.column(), equalTo(1));
        assertThat(p.position(), equalTo(0));
    }

    @Test
    public void parserValidatesMaxNestingLevelGreaterThanZero() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SimpleParser("foo", -100));
    }

    @Test
    public void expectThrowsWhenNotMatching() {
        RuntimeException e = Assertions.assertThrows(RuntimeException.class, () -> {
            new SimpleParser("foo").expect('!');
        });

        assertThat(e.getMessage(), equalTo("Syntax error at line 1, column 1: Expected: '!', but found 'f'"));
    }

    @Test
    public void expectThrowsWhenNotMatchingOneOfMany() {
        RuntimeException e = Assertions.assertThrows(RuntimeException.class, () -> {
            new SimpleParser("foo").expect('!', '?');
        });

        assertThat(e.getMessage(),
                equalTo(
                        "Syntax error at line 1, column 1: Found 'f', but expected one of the following tokens: '!' '?'"));
    }

    @Test
    public void expectThrowsWhenNotMatchingOneOfManyAndDueToEof() {
        RuntimeException e = Assertions.assertThrows(RuntimeException.class, () -> {
            new SimpleParser("").expect('!', '?');
        });

        assertThat(e.getMessage(),
                equalTo(
                        "Syntax error at line 1, column 1: Found '[EOF]', but expected one of the following tokens: '!' '?'"));
    }

    @Test
    public void tracksLinesAndColumnsAndSlices() {
        SimpleParser p = new SimpleParser("foo\nbaz\r\nBam!\nHi");

        p.consumeRemainingCharactersOnLine();
        assertThat(p.line(), equalTo(1));
        assertThat(p.column(), equalTo(4));
        assertThat(p.sliceFrom(0), equalTo("foo"));
        p.skip();

        int position = p.position();

        p.consumeRemainingCharactersOnLine();
        assertThat(p.line(), equalTo(2));
        assertThat(p.column(), equalTo(4));
        assertThat(p.sliceFrom(position), equalTo("baz"));
        p.skip();

        position = p.position();

        p.consumeRemainingCharactersOnLine();
        assertThat(p.line(), equalTo(3));
        assertThat(p.column(), equalTo(5));
        assertThat(p.sliceFrom(position), equalTo("Bam!"));
        p.skip();

        position = p.position();

        p.consumeRemainingCharactersOnLine();
        assertThat(p.line(), equalTo(4));
        assertThat(p.column(), equalTo(3));
        assertThat(p.sliceFrom(position), equalTo("Hi"));
        p.skip();

        assertThat(p.eof(), equalTo(true));
    }

    @Test
    public void expectConsumesAndDoesNotThrowWhenValid() {
        SimpleParser p = new SimpleParser("hi");

        assertThat(p.expect('h'), equalTo('h'));
        assertThat(p.expect('i', '!'), equalTo('i'));
    }

    @Test
    public void canPeekForwardAndBehind() {
        SimpleParser p = new SimpleParser("foo");

        assertThat(p.peek(), equalTo('f'));
        assertThat(p.peek(0), equalTo('f'));
        assertThat(p.peek(1), equalTo('o'));
        assertThat(p.peek(2), equalTo('o'));
        assertThat(p.peek(3), equalTo(Character.MIN_VALUE));
        assertThat(p.peek(-1), equalTo(Character.MIN_VALUE));

        p.expect('f');
        assertThat(p.peek(-1), equalTo('f'));
    }

    @Test
    public void assertsNestingNotTooDeep() {
        SimpleParser p = new SimpleParser("foo", 2);
        p.increaseNestingLevel();
        p.increaseNestingLevel();
        p.decreaseNestingLevel();
        p.increaseNestingLevel();

        // Hits 3 at this point, so it throws.
        Assertions.assertThrows(RuntimeException.class, p::increaseNestingLevel);
    }

    @Test
    public void doesNotValidateNestingLevelWhenItIs0() {
        SimpleParser p = new SimpleParser("foo");

        // These are all fine.
        p.increaseNestingLevel();
        p.increaseNestingLevel();
        p.increaseNestingLevel();
        assertThat(p.nestingLevel(), equalTo(3));
    }

    @Test
    public void validatesNestingLevelDoesNotGoBelow0() {
        SimpleParser p = new SimpleParser("foo", 2);

        Assertions.assertThrows(RuntimeException.class, p::decreaseNestingLevel);
    }

    @Test
    public void skipsWhitespace() {
        SimpleParser p = new SimpleParser(" \n\t\r  hi");
        p.ws();

        assertThat(p.position(), equalTo(6));
        assertThat(p.line(), equalTo(3));
        assertThat(p.column(), equalTo(3));
        p.expect('h');
        p.expect('i');

        // Skipping ws at EOF is fine.
        assertThat(p.eof(), is(true));
        p.ws();
        assertThat(p.eof(), is(true));
    }

    @Test
    public void skipsSpaces() {
        SimpleParser p = new SimpleParser(" \n\t\r  hi");
        p.sp();

        assertThat(p.position(), equalTo(1));
        assertThat(p.line(), equalTo(1));
        assertThat(p.column(), equalTo(2));
        p.expect('\n');
        p.sp(); // skips the tab!
        assertThat(p.position(), equalTo(3));
        p.sp(); // no-op
        p.expect('\r');
        p.sp();
        p.expect('h');
        p.sp();
        p.expect('i');

        // Skipping sp at EOF is fine.
        p.sp();
    }

    @Test
    public void expectsNewlineAndSkipsSpaces() {
        SimpleParser p = new SimpleParser("   \n.\r.\r\n.");
        p.br();

        assertThat(p.position(), equalTo(4));
        assertThat(p.line(), equalTo(2));
        assertThat(p.column(), equalTo(1));
        p.expect('.');
        p.br();
        p.expect('.');
        p.br();
        p.expect('.');
    }

    @Test
    public void eofIsAcceptableNewline() {
        SimpleParser p = new SimpleParser("");
        p.br();
    }

    @Test
    public void throwsWhenNotNewline() {
        RuntimeException e = Assertions.assertThrows(RuntimeException.class, () -> {
            SimpleParser p = new SimpleParser("H");
            p.br();
        });

        assertThat(e.getMessage(), containsString("Expected a line break, but found 'H'"));
    }
}
