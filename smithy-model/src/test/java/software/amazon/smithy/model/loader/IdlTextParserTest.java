package software.amazon.smithy.model.loader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.smithy.model.node.StringNode;

public class IdlTextParserTest {
    @ParameterizedTest
    @MethodSource("validTextProvider")
    public void parsesText(String input, String lexeme) {
        IdlModelParser parser = new IdlModelParser("/foo", input);
        StringNode result = IdlNodeParser.parseNode(parser).expectStringNode();
        assertThat(result.getValue(), equalTo(lexeme));
    }

    private static Stream<Arguments> validTextProvider() {
        return Stream.of(
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
                Arguments.of("\"\\\"\"", "\""),
                Arguments.of("\"\\ud83d\\ude00\"", "\uD83D\uDE00"),

                // Text blocks
                Arguments.of("\"\"\"\nf\"\"\"", "f"),
                Arguments.of("\"\"\"\nfoo\"\"\"", "foo"),
                Arguments.of("\"\"\"\nfoo\\\"\"\"\"", "foo\""),
                Arguments.of("\"\"\"\nf\n    foo\n\"\"\"", "f\n    foo\n"),
                Arguments.of("\"\"\"\n  f\n    foo\n\"\"\"", "f\n  foo\n"),
                Arguments.of("\"\"\"\n  foo\nf\n\"\"\"", "  foo\nf\n"),
                Arguments.of("\"\"\"\n    foo\n  f\n\"\"\"", "  foo\nf\n"),
                Arguments.of("\"\"\"\n    foo\n    baz\"\"\"", "foo\nbaz"),
                Arguments.of("\"\"\"\n\n\n\"\"\"", "\n\n"),
                Arguments.of("\"\"\"\n  foo\n  baz\n  \"\"\"", "foo\nbaz\n"),
                Arguments.of("\"\"\"\n  foo\n    baz\n  \"\"\"", "foo\n  baz\n"),
                Arguments.of("\"\"\"\n\"foo\\\"\"\"\"", "\"foo\""),
                Arguments.of("\"\"\"\n  foo\\n    bar\n  baz\"\"\"", "foo\n    bar\nbaz"),
                // Empty lines and lines with only ws do not contribute to incidental ws.
                Arguments.of("\"\"\"\n\n    foo\n  \n\n      \n    \"\"\"", "\nfoo\n\n\n\n"),
                // If the last line is offset to the right, it's discarded since it's all whitespace.
                Arguments.of("\"\"\"\n  foo\n    \"\"\"", "foo\n"),
                Arguments.of("\"\"\"\n  Foo\\\n  Baz\"\"\"", "FooBaz"),
                Arguments.of("\"\"\"\r  Foo\\\r  Baz\"\"\"", "FooBaz"),
                Arguments.of("\"\"\"\r\n  Foo\\\r\n  Baz\"\"\"", "FooBaz"));
    }
}
