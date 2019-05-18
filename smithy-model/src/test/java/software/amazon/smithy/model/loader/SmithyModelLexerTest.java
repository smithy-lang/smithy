package software.amazon.smithy.model.loader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class SmithyModelLexerTest {
    @ParameterizedTest
    @MethodSource("validStringEscapeProvider")
    public void parsesEscapesInString(String input, String lexeme) {
        SmithyModelLexer lexer = new SmithyModelLexer(input);

        assertThat(lexer.next().lexeme, equalTo(lexeme));
    }

    private static Stream<Arguments> validStringEscapeProvider() {
        return Stream.of(
                // DQUOTE
                Arguments.of("\"foo\"", "foo"),
                Arguments.of("\"foo\\\\bar\"", "foo\\bar"),
                Arguments.of("\"\t\"", "\t"),
                Arguments.of("\"\r\"", "\r"),
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
                Arguments.of("\"\r\\u000A\r\n\"", "\r\n\r\n"),
                Arguments.of("\"\\\"\"", "\""),
                Arguments.of("\"foo\\\\\"", "foo\\"),
                Arguments.of("\"\\/\"", "/"),

                // SQUOTE
                Arguments.of("'foo'", "foo"),
                Arguments.of("'foo\\\\bar'", "foo\\bar"),
                Arguments.of("'\t'", "\t"),
                Arguments.of("'\r'", "\r"),
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
                Arguments.of("'\r\\u000A\r\n'", "\r\n\r\n"),
                Arguments.of("'\\''", "'"),
                Arguments.of("'foo\\\\'", "foo\\"),
                Arguments.of("'\\/'", "/"));
    }

    @ParameterizedTest
    @MethodSource("invalidStringEscapeProvider")
    public void parsesInvalidEscapesInString(String input, String errorContains) {
        SmithyModelLexer.Token token = new SmithyModelLexer(input).next();
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
                Arguments.of("'\\uaaat'", "Invalid unicode escape character: `t`"));
    }
}
