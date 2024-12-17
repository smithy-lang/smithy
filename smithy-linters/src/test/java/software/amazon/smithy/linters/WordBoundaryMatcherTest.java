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

public class WordBoundaryMatcherTest {

    @ParameterizedTest
    @MethodSource("patternProvider")
    public void matchesPatterns(String text, String pattern, boolean match) {
        WordBoundaryMatcher matcher = new WordBoundaryMatcher();
        matcher.addSearch(pattern);

        assertThat("'" + text + "' matches '" + pattern + '\'', matcher.test(text), is(match));
    }

    public static Stream<Arguments> patternProvider() {
        return Stream.of(
                // Can't match empty or null.
                Arguments.of("", "access key id", false),
                Arguments.of(null, "access key id", false),

                // Good word matches.
                Arguments.of("accessKeyId", "access key id", true),
                Arguments.of("accesskeyid", "access key id", true),
                Arguments.of("access_key_id", "access key id", true),
                Arguments.of("access_key_ID", "access key id", true),
                Arguments.of("accessKey__Id", "access key id", true),

                // Tricky word boundary matches.
                Arguments.of("accessKey__Id", "access key id", true),
                Arguments.of("accessKey__Id", "access key id", true),
                Arguments.of("accessKey__Id", "access key id", true),
                Arguments.of("accessKey__Id", "access key id", true),
                Arguments.of("accessKey__Id", "access key id", true),
                Arguments.of("accessKey__Id", "access key id", true),
                Arguments.of("accessKey__Id", "access key id", true),
                Arguments.of("access:Key:Id", "access key id", true),
                Arguments.of("access Key Id", "access key id", true),
                Arguments.of("access-Key-Id", "access key id", true),
                Arguments.of("access.Key.Id200", "access key id", true),
                Arguments.of("AccessKeyIDValue", "access key id", true),
                Arguments.of("__AccessKeyIDValue__", "access key id", true),
                Arguments.of("zip", "zip", true),
                Arguments.of("unzip", "zip", false),
                Arguments.of("zipCode", "zip", true),

                // No match because zipcode is parsed as one word.
                Arguments.of("zipcode", "zip", false),

                // No match is found because "accesskey_id" is split into "accesskey id"
                Arguments.of("foo accesskey_id", "access key id", false),

                // Cases where no match is found and the word counts differ.
                Arguments.of("string", "this is too long to match", false),
                Arguments.of("this is not a match", "no", false),

                // An exact match.
                Arguments.of("string", "string", true),

                Arguments.of("foo_bar_baz", "bar", true),
                Arguments.of("foo_baz_bar", "bar", true),
                Arguments.of("foo_bazbar", "bar", false),
                Arguments.of("bazbarbaz", "bar", false));
    }

    @ParameterizedTest
    @MethodSource("invalidPatternProvider")
    public void validatesSyntax(String invalidPattern) {
        WordBoundaryMatcher matcher = new WordBoundaryMatcher();

        IllegalArgumentException e = Assertions.assertThrows(IllegalArgumentException.class,
                () -> matcher.addSearch(invalidPattern),
                invalidPattern);

        // All syntax errors should show the invalid pattern.
        assertThat(e.getMessage(), containsString(invalidPattern));
    }

    public static Stream<Arguments> invalidPatternProvider() {
        return Stream.of(
                Arguments.of("foo  bar"),
                Arguments.of(" foo bar "),
                Arguments.of("   foo"),
                Arguments.of("foo_bar"), // non alphanumeric
                Arguments.of("foo+bar") // non alphanumeric
        );
    }
}
