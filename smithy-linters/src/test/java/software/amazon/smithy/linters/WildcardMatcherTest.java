/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.linters;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class WildcardMatcherTest {

    @ParameterizedTest
    @MethodSource("patternProvider")
    public void matchesPatterns(String text, String pattern, boolean match) {
        WildcardMatcher matcher = new WildcardMatcher();
        matcher.addSearch(pattern);

        assertThat("'" + text + "' matches '" + pattern + '\'', matcher.test(text), is(match));
    }

    public static Stream<Arguments> patternProvider() {
        return Stream.of(
                // Can't match empty or null.
                Arguments.of("", "foo", false),
                Arguments.of(null, "foo", false),

                // Not a contains match
                Arguments.of("foo", "*hello*", false),

                // Good contains matches.
                Arguments.of("__accessKeyId__", "*accesskeyid*", true),
                Arguments.of("accessKeyId", "*accesskeyid*", true),
                Arguments.of("hello", "*hello*", true),
                Arguments.of("foo_hello_there", "*hello*", true),

                // Not a prefix match.
                Arguments.of("foo", "hello*", false),

                // Good prefix matches.
                Arguments.of("accessKeyId", "accesskeyid*", true),
                Arguments.of("hello", "hello*", true),
                Arguments.of("hello_there", "hello*", true),

                // Not a suffix match.
                Arguments.of("foo", "*hello", false),

                // Good suffix matches.
                Arguments.of("accessKeyId", "*accesskeyid", true),
                Arguments.of("hello", "*hello", true),
                Arguments.of("well_hello", "*hello", true),

                // An exact match.
                Arguments.of("string", "string", true));
    }

    @ParameterizedTest
    @MethodSource("invalidPatternProvider")
    public void validatesSyntax(String invalidPattern) {
        WildcardMatcher matcher = new WildcardMatcher();

        IllegalArgumentException e = Assertions.assertThrows(IllegalArgumentException.class,
                () -> matcher.addSearch(invalidPattern),
                invalidPattern);

        // All syntax errors should show the invalid pattern.
        assertThat(e.getMessage(), containsString(invalidPattern));
    }

    public static Stream<Arguments> invalidPatternProvider() {
        return Stream.of(
                Arguments.of("*"),
                Arguments.of("**foo"),
                Arguments.of("foo*bar"),
                Arguments.of(""));
    }
}
