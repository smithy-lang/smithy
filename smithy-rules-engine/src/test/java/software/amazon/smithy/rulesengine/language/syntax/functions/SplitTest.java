/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.language.syntax.functions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.smithy.rulesengine.language.evaluation.Scope;
import software.amazon.smithy.rulesengine.language.evaluation.type.Type;
import software.amazon.smithy.rulesengine.language.evaluation.value.Value;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.Split;
import software.amazon.smithy.rulesengine.language.syntax.expressions.literal.Literal;

public class SplitTest {

    @Test
    void testSplitTypeChecking() {
        Expression input = Literal.of("a--b--c");
        Expression delimiter = Literal.of("--");
        Expression limit = Literal.of(0);
        Split split = Split.ofExpressions(input, delimiter, limit);

        Scope<Type> scope = new Scope<>();
        Type resultType = split.typeCheck(scope);

        assertEquals(Type.arrayType(Type.stringType()), resultType);
    }

    @Test
    void testSplitWithVariableInput() {
        Expression input = Expression.getReference(Identifier.of("bucket"));
        Expression delimiter = Literal.of("--");
        Expression limit = Literal.of(2);
        Split split = Split.ofExpressions(input, delimiter, limit);

        Scope<Type> scope = new Scope<>();
        scope.insert("bucket", Type.stringType());

        Type resultType = split.typeCheck(scope);

        assertEquals(Type.arrayType(Type.stringType()), resultType);
    }

    @Test
    void testSplitWithInvalidInputType() {
        Expression input = Literal.of(42); // integer not string
        Expression delimiter = Literal.of("--");
        Expression limit = Literal.of(0);
        Split split = Split.ofExpressions(input, delimiter, limit);

        Scope<Type> scope = new Scope<>();

        assertThrows(Exception.class, () -> split.typeCheck(scope));
    }

    @Test
    void testEvaluateRoundTrip() {
        List<Value> args = Arrays.asList(Value.stringValue("a--b--c"), Value.stringValue("--"), Value.integerValue(2));
        Value result = Split.getDefinition().evaluate(args);
        List<String> out = result.expectArrayValue()
                .getValues()
                .stream()
                .map(v -> v.expectStringValue().getValue())
                .collect(Collectors.toList());

        assertEquals(Arrays.asList("a", "b--c"), out);
    }

    @ParameterizedTest
    @MethodSource("provideSplitTestCases")
    void testSplitFunction(String input, String delimiter, int limit, List<String> expected) {
        List<String> result = Split.split(input, delimiter, limit);
        assertEquals(expected, result);
    }

    private static Stream<Arguments> provideSplitTestCases() {
        return Stream.of(
                // Basic splitting
                Arguments.of("a--b--c", "--", 0, Arrays.asList("a", "b", "c")),
                Arguments.of("a--b--c", "--", 2, Arrays.asList("a", "b--c")),
                Arguments.of("a--b--c", "--", 1, Arrays.asList("a--b--c")),

                // Empty strings and edge cases
                Arguments.of("", "--", 0, Arrays.asList("")),
                Arguments.of("", "--", 1, Arrays.asList("")),
                Arguments.of("--", "--", 0, Arrays.asList("", "")),
                Arguments.of("----", "--", 0, Arrays.asList("", "", "")),
                Arguments.of("----", "--", 2, Arrays.asList("", "--")),
                Arguments.of("--b--", "--", 0, Arrays.asList("", "b", "")),

                // Overlapping delimiter cases
                Arguments.of("aaa", "aa", 0, Arrays.asList("", "a")),
                Arguments.of("aaaa", "aa", 0, Arrays.asList("", "", "")),
                Arguments.of("aaaa", "aa", 2, Arrays.asList("", "aa")),

                // Limits near boundaries
                Arguments.of("a--b--c", "--", 3, Arrays.asList("a", "b", "c")),
                Arguments.of("a--b--c", "--", 20, Arrays.asList("a", "b", "c")),

                // Delimiter equals the whole string
                Arguments.of("abc", "abc", 0, Arrays.asList("", "")),
                Arguments.of("abc", "abc", 2, Arrays.asList("", "")),
                Arguments.of("abc", "abc", 1, Arrays.asList("abc")),

                // No delimiter found
                Arguments.of("abc", "x", 0, Arrays.asList("abc")),
                Arguments.of("a-b-c", "--", 0, Arrays.asList("a-b-c")),
                Arguments.of("prefix", "--", 0, Arrays.asList("prefix")),
                Arguments.of("mybucket", "--", 1, Arrays.asList("mybucket")),

                // S3Express patterns
                Arguments.of("--x-s3--azid--suffix", "--", 0, Arrays.asList("", "x-s3", "azid", "suffix")),
                Arguments.of("--x-s3--azid--suffix", "--", 2, Arrays.asList("", "x-s3--azid--suffix")),
                Arguments.of("--x-s3--azid--suffix", "--", 3, Arrays.asList("", "x-s3", "azid--suffix")),
                Arguments.of("--x-s3--azid--suffix", "--", 4, Arrays.asList("", "x-s3", "azid", "suffix")),

                // Multiple parts with limits
                Arguments.of("a--b--c--d", "--", 3, Arrays.asList("a", "b", "c--d")),
                Arguments.of("a--b--c--d--e", "--", 2, Arrays.asList("a", "b--c--d--e")),

                // Leading and trailing delimiters
                Arguments.of("--leading", "--", 0, Arrays.asList("", "leading")),
                Arguments.of("--lead", "--", 2, Arrays.asList("", "lead")),
                Arguments.of("trailing--", "--", 0, Arrays.asList("trailing", "")),
                Arguments.of("trail--", "--", 2, Arrays.asList("trail", "")),
                Arguments.of("--both--", "--", 0, Arrays.asList("", "both", "")),
                Arguments.of("--both--", "--", 2, Arrays.asList("", "both--")),

                // Single character delimiter
                Arguments.of("a,b,c", ",", 0, Arrays.asList("a", "b", "c")),
                Arguments.of("a,b,c", ",", 2, Arrays.asList("a", "b,c")),

                // Longer delimiter
                Arguments.of("foo<=>bar<=>baz", "<=>", 0, Arrays.asList("foo", "bar", "baz")),
                Arguments.of("foo<=>bar<=>baz", "<=>", 2, Arrays.asList("foo", "bar<=>baz")),

                // Unicode / multi-code-point safety
                Arguments.of("a🌟b🌟c", "🌟", 0, Arrays.asList("a", "b", "c")),
                Arguments.of("👨‍👩‍👧‍👦X👨‍👩‍👧‍👦", "👨‍👩‍👧‍👦", 0, Arrays.asList("", "X", "")),
                Arguments.of("👨‍👩‍👧‍👦X👨‍👩‍👧‍👦", "👨‍👩‍👧‍👦", 2, Arrays.asList("", "X👨‍👩‍👧‍👦")),

                // "Looks like regex but isn't" sanity checks
                Arguments.of("a.*b.*c", ".*", 0, Arrays.asList("a", "b", "c")),
                Arguments.of("a|b|c", "|", 0, Arrays.asList("a", "b", "c")),

                // Delimiter longer than input
                Arguments.of("ab", "abcd", 0, Arrays.asList("ab")),
                Arguments.of("ab", "abcd", 2, Arrays.asList("ab")));
    }

    @Test
    void testSplitInvariants() {
        assertThrows(NullPointerException.class, () -> Split.split(null, "--", 0));
        assertThrows(NullPointerException.class, () -> Split.split("test", null, 0));
        assertThrows(IllegalArgumentException.class, () -> Split.split("test", "", 0));
        assertThrows(IllegalArgumentException.class, () -> Split.split("test", "-", -1));
    }

    @Test
    void testSplitForContainsCheck() {
        // split(input, delim, 2) -> isSet(parts[1]) pattern for contains
        List<String> hasDelimiter = Split.split("foo--bar", "--", 2);
        assertEquals(2, hasDelimiter.size());
        assertEquals("foo", hasDelimiter.get(0));
        assertEquals("bar", hasDelimiter.get(1));

        List<String> noDelimiter = Split.split("foobar", "--", 2);
        assertEquals(1, noDelimiter.size());
        assertEquals("foobar", noDelimiter.get(0));
    }

    @Test
    void testSplitForStartsWithCheck() {
        // Empty first element means input starts with delimiter
        List<String> startsWithDelim = Split.split("--prefix", "--", 2);
        assertEquals(2, startsWithDelim.size());
        assertEquals("", startsWithDelim.get(0));
        assertEquals("prefix", startsWithDelim.get(1));

        List<String> notStartsWith = Split.split("prefix--", "--", 2);
        assertEquals(2, notStartsWith.size());
        assertEquals("prefix", notStartsWith.get(0));
        assertEquals("", notStartsWith.get(1));
    }
}
