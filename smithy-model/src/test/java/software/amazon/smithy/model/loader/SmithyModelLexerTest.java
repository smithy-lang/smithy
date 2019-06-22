package software.amazon.smithy.model.loader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class SmithyModelLexerTest {
    @Test
    public void throwsWhenNoTokens() {
        Assertions.assertThrows(NoSuchElementException.class, () -> {
            new SmithyModelLexer("/foo.smithy", "").next();
        });
    }

    @ParameterizedTest
    @MethodSource("validTextProvider")
    public void parsesText(String input, String lexeme) {
        SmithyModelLexer lexer = new SmithyModelLexer("/foo.smithy", input);
        SmithyModelLexer.Token token = lexer.next();

        assertThat(token.lexeme, equalTo(lexeme));
        assertThat(lexer.hasNext(), is(false));
    }

    private static Stream<Arguments> validTextProvider() {
        return Stream.of(
                // double quotes
                Arguments.of("\"foo\"", "foo"),
                Arguments.of("\"foo\\\\bar\"", "foo\\bar"),
                Arguments.of("\"\t\"", "\t"),
                Arguments.of("\"\r\"", "\n"),
                Arguments.of("\"\n\"", "\n"),
                Arguments.of("\"\b\"", "\b"),
                Arguments.of("\"\f\"", "\f"),
                Arguments.of("\"\\t\"", "\t"),
                Arguments.of("\"\\r\"", "\r"),
                Arguments.of("\"\\n\"", "\n"),
                Arguments.of("\"\\b\"", "\b"),
                Arguments.of("\"\\f\"", "\f"),
                Arguments.of("\"\\uffff\"", "\uffff"),
                Arguments.of("\"\\u000A\"", "\n"),
                Arguments.of("\"\\u000D\\u000A\"", "\r\n"),
                Arguments.of("\"\r\\u000A\r\n\"", "\n\n\n"),
                Arguments.of("\"\\\"\"", "\""),
                Arguments.of("\"foo\\\\\"", "foo\\"),
                Arguments.of("\"\\/\"", "/"),
                Arguments.of("\"foo\\\nbaz\"", "foobaz"),
                Arguments.of("\"foo\\\rbaz\"", "foobaz"),
                Arguments.of("\"foo\\\r\nbaz\"", "foobaz"),
                Arguments.of("\"\\\r\"", ""),
                Arguments.of("\"\\\n\"", ""),
                Arguments.of("\"\\'\"", "'"),
                Arguments.of("\"\\\"\"", "\""),
                Arguments.of("\"\\ud83d\\ude00\"", "\uD83D\uDE00"),

                // single quotes
                Arguments.of("'foo'", "foo"),
                Arguments.of("'foo\\\\bar'", "foo\\bar"),
                Arguments.of("'\t'", "\t"),
                Arguments.of("'\r'", "\n"),
                Arguments.of("'\n'", "\n"),
                Arguments.of("'\b'", "\b"),
                Arguments.of("'\f'", "\f"),
                Arguments.of("'\\t'", "\t"),
                Arguments.of("'\\r'", "\r"),
                Arguments.of("'\\n'", "\n"),
                Arguments.of("'\\b'", "\b"),
                Arguments.of("'\\f'", "\f"),
                Arguments.of("'\\uffff'", "\uffff"),
                Arguments.of("'\\u000A'", "\n"),
                Arguments.of("'\\u000D\\u000A'", "\r\n"),
                Arguments.of("'\r\\u000A\r\n'", "\n\n\n"),
                Arguments.of("'\\''", "'"),
                Arguments.of("'foo\\\\'", "foo\\"),
                Arguments.of("'\\/'", "/"),
                Arguments.of("'foo\\\nbaz'", "foobaz"),
                Arguments.of("'foo\\\rbaz'", "foobaz"),
                Arguments.of("'foo\\\r\nbaz'", "foobaz"),
                Arguments.of("'\\\r'", ""),
                Arguments.of("'\\\n'", ""),
                Arguments.of("'\\''", "'"),
                Arguments.of("'\\\"'", "\""),

                // Text blocks
                Arguments.of("\"\"\"\nfoo\"\"\"", "foo"),
                Arguments.of("\"\"\"\nfoo\\\"\"\"\"\"", "foo\"\""),
                Arguments.of("\"\"\"\n    foo\n    baz\"\"\"", "foo\nbaz"),
                Arguments.of("\"\"\"\n\n\n\"\"\"", "\n\n"),
                Arguments.of("\"\"\"\n  foo\n  baz\n  \"\"\"", "foo\nbaz\n"),
                Arguments.of("\"\"\"\n  foo\n    baz\n  \"\"\"", "foo\n  baz\n"),
                Arguments.of("\"\"\"\n\"foo\"\"\"\"", "\"foo\""),
                Arguments.of("\"\"\"\n  foo\\n    bar\n  baz\"\"\"", "foo\n    bar\nbaz"),
                // Empty lines and lines with only ws do not contribute to incidental ws.
                Arguments.of("\"\"\"\n\n    foo\n  \n\n      \n    \"\"\"", "\nfoo\n\n\n\n"),
                // If the last line is offset to the right, it's discarded since it's all whitespace.
                Arguments.of("\"\"\"\n  foo\n    \"\"\"", "foo\n"),
                Arguments.of("\"\"\"\n  Foo\\\n  Baz\"\"\"", "FooBaz"),
                Arguments.of("\"\"\"\r  Foo\\\r  Baz\"\"\"", "FooBaz"),
                Arguments.of("\"\"\"\r\n  Foo\\\r\n  Baz\"\"\"", "FooBaz"));
    }

    @ParameterizedTest
    @MethodSource("invalidStringEscapeProvider")
    public void parsesInvalidEscapesInString(String input, String errorContains) {
        SmithyModelLexer lexer = new SmithyModelLexer("/foo.smithy", input);
        SmithyModelLexer.Token token = lexer.next();
        if (token.type != SmithyModelLexer.TokenType.ERROR) {
            Assertions.fail("expected a syntax error for: `" + input + "`, but found " + token);
        }

        assertThat(token.toString(), containsString(errorContains));
    }

    private static Stream<Arguments> invalidStringEscapeProvider() {
        return Stream.of(
                // These don't match the regex:
                Arguments.of("'\\", "ERROR"),
                Arguments.of("'\\\\", "ERROR"),
                Arguments.of("'\\'", "ERROR"),
                Arguments.of("\"\\\"", "ERROR"),

                // These match the regex, but fail to parse escapes within it.
                Arguments.of("\"\\ \"", "Invalid escape found in string: `\\ `"),
                Arguments.of("\"\\a\"", "Invalid escape found in string: `\\a`"),
                Arguments.of("\"\\ua\"", "Invalid unclosed unicode escape"),
                Arguments.of("\"\\ua\"", "Invalid unclosed unicode escape"),
                Arguments.of("\"\\uaa\"", "Invalid unclosed unicode escape"),
                Arguments.of("\"\\uaaa\"", "Invalid unclosed unicode escape"),
                Arguments.of("\"\\uaaat\"", "Invalid unicode escape character: `t`"),

                // Now for SQUOTE.
                Arguments.of("'\\ '", "Invalid escape found in string: `\\ `"),
                Arguments.of("'\\a'", "Invalid escape found in string: `\\a`"),
                Arguments.of("'\\ua'", "Invalid unclosed unicode escape"),
                Arguments.of("'\\ua'", "Invalid unclosed unicode escape"),
                Arguments.of("'\\uaa'", "Invalid unclosed unicode escape"),
                Arguments.of("'\\uaaa'", "Invalid unclosed unicode escape"),
                Arguments.of("'\\uaaat'", "Invalid unicode escape character: `t`"),

                // Text blocks
                Arguments.of("\"\"\"foo\"\"\"", "text block must start with a new line"),
                Arguments.of("\"\"\"\"\"\"", "text block is empty"));
    }

    @ParameterizedTest
    @MethodSource("lineAndColumnProvider")
    public void tracksLineAndColumn(String input, String[] positions) {
        List<SmithyModelLexer.Token> tokens = new ArrayList<>();
        SmithyModelLexer lexer = new SmithyModelLexer("/foo.smithy", input);
        lexer.forEachRemaining(tokens::add);

        assertThat("Token length mismatch: " + tokens, tokens.size(), equalTo(positions.length));
        for (int i = 0; i < positions.length; i++) {
            SmithyModelLexer.Token token = tokens.get(i);
            String position = positions[i];
            int line = Integer.parseInt(position.substring(0, position.indexOf(":")));
            int column = Integer.parseInt(position.substring(position.indexOf(":") + 1));
            assertThat("Line does not match for " + input + " token " + i + " : " + token,
                       token.line, equalTo(line));
            assertThat("Column does not match for " + input + " token " + i + " : " + token,
                       token.column, equalTo(column));
        }
    }

    private static Stream<Arguments> lineAndColumnProvider() {
        return Stream.of(
                Arguments.of("foo baz", new String[]{"1:1", "1:5"}),
                Arguments.of("foo\n baz", new String[]{"1:1", "2:2"}),
                Arguments.of("'foo\n  \nbaz' bar", new String[]{"1:1", "3:6"}),
                Arguments.of("'foo\n  \nbaz'   bar", new String[]{"1:1", "3:8"}),
                Arguments.of("\"\"\"\nfoo\"\"\"  baz", new String[]{"1:1", "2:9"}),
                Arguments.of("'foo\n ' '\nbaz '   'bar'", new String[]{"1:1", "2:4", "3:9"}),
                Arguments.of("'foo\r\n\r\\n\\t' boo", new String[]{"1:1", "3:7"}),
                Arguments.of("\"\"\"\n  Foo\\\n  Baz\"\"\" 'bar'", new String[]{"1:1", "3:10"}),
                Arguments.of("\"\"\"\r  Foo\\\r  Baz\"\"\" 'bar'", new String[]{"1:1", "3:10"}),
                Arguments.of("\"\"\"\r\n  Foo\\\r\n  Baz\"\"\" 'bar'", new String[]{"1:1", "3:10"}));
    }
}
