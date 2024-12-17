/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class ExpressionSerializerTest {
    @ParameterizedTest
    @MethodSource("shapeSource")
    public void serializesExpressions(String expressionString, String expectedString) {
        JmespathExpression expression = JmespathExpression.parse(expressionString);
        ExpressionSerializer serializer = new ExpressionSerializer();
        String serialized = serializer.serialize(expression);

        // First make sure we made a valid expression.
        JmespathExpression reparsed = JmespathExpression.parse(serialized);

        // Now make sure it's equivalent to the origin expression.
        assertThat(expression, equalTo(reparsed));

        assertThat(serialized, equalTo(expectedString));
    }

    public static Collection<Object[]> shapeSource() {
        return Arrays.asList(new Object[][] {
                // Current node
                {"@", "@"},

                // Identifiers
                {"foo", "\"foo\""},
                {"\"a\\\"b\"", "\"a\\\"b\""},
                {"\"a\\\\b\"", "\"a\\\\b\""},

                // Array index
                {"[0]", "[0]"},
                {"[10]", "[10]"},

                // Array slices
                {"[0:]", "[0::1]"}, // projection that contains the slice
                {"[0:].foo", "[0::1].\"foo\""},

                // Subexpressions
                {"foo | bar", "\"foo\" | \"bar\""},
                {"foo.bar", "\"foo\".\"bar\""},
                {"foo.bar.baz", "\"foo\".\"bar\".\"baz\""},
                {"foo | bar | baz", "\"foo\" | \"bar\" | \"baz\""},
                {"foo | bar | baz | \"a.b\"", "\"foo\" | \"bar\" | \"baz\" | \"a.b\""},

                // Object projections
                {"*", "*"},
                {"foo.*", "\"foo\".*"},
                {"foo.* | @", "\"foo\".* | @"},
                {"foo.*.bar", "\"foo\".*.\"bar\""},
                {"foo.*.bar | bam", "\"foo\".*.\"bar\" | \"bam\""},

                // Array projections / flatten
                {"[]", "@[]"},
                {"foo[]", "\"foo\"[]"},
                {"foo[].bar", "\"foo\"[].\"bar\""},
                {"foo[] | bar", "\"foo\"[] | \"bar\""},

                // Not
                {"!@", "!(@)"},
                {"!foo.bar", "!(\"foo\").\"bar\""}, // this expression in nonsensical, but valid.

                // And
                {"@ && @", "(@ && @)"},
                {"foo.bar && foo.baz", "(\"foo\".\"bar\" && \"foo\".\"baz\")"},

                // Or
                {"@ || @", "(@ || @)"},
                {"foo.bar || foo.baz", "(\"foo\".\"bar\" || \"foo\".\"baz\")"},

                // functions
                {"length(@)", "length(@)"},
                {"ends_with(@, @)", "ends_with(@, @)"},
                {"min_by(@, &foo)", "min_by(@, &(\"foo\"))"},

                // comparator
                {"@ == @", "@ == @"},

                // multi-select list
                {"[@]", "[@]"},
                {"[@, @]", "[@, @]"},

                // multi-select hash
                {"{foo: foo, bar: bar}", "{\"foo\": \"foo\", \"bar\": \"bar\"}"},

                // Filter expressions.
                {"foo[?bar > baz][?qux > baz]", "\"foo\"[?\"bar\" > \"baz\"][?\"qux\" > \"baz\"]"}
        });
    }

    @ParameterizedTest()
    @MethodSource("validExpressions")
    public void canSerializeEveryValidExpressionFromFile(String line) {
        JmespathExpression expression = JmespathExpression.parse(line);
        ExpressionSerializer serializer = new ExpressionSerializer();
        String serialized = serializer.serialize(expression);

        try {
            JmespathExpression reparsed = JmespathExpression.parse(serialized);

            // The AST of the originally parsed value must be equal to the AST of the reserialized then
            // parsed value.
            assertThat(line, expression, equalTo(reparsed));
        } catch (JmespathException e) {
            throw new RuntimeException("Error parsing " + serialized + ": " + e.getMessage(), e);
        }
    }

    // Ensures that every expression in "valid" can be serialized and reparsed correctly.
    // The serialized string my be different, but the AST must be the same.
    public static Stream<Object[]> validExpressions() {
        return new NewLineExpressionsDataSource().validTests()
                .map(line -> new Object[] {line});
    }
}
