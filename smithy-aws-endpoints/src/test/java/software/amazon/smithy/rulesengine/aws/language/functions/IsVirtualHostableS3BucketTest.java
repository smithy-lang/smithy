/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.aws.language.functions;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class IsVirtualHostableS3BucketTest {
    @ParameterizedTest
    @CsvSource({
            "ab, false, false", // Too short (2 chars)
            "abc, false, true", // Min length (3 chars)
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa, false, true", // Max length (63 chars)
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa, false, false", // Too long (64 chars)

            "-abc, false, false", // Starts with dash
            ".abc, true, false", // Starts with dot
            "abc-, false, false", // Ends with dash
            "abc., true, false", // Ends with dot

            "abc, false, true", // Simple alphanumeric, no dots allowed
            "a-b-c, false, true", // With dashes, no dots allowed
            "a.b-c, false, false", // With dots when not allowed

            "abc, true, true", // Simple alphanumeric, dots allowed
            "a-b-c, true, true", // With dashes, dots allowed
            "a.b-c, true, true", // With dots when allowed

            "a..b, true, false", // Consecutive dots
            "a-.b, true, false", // Dash followed by dot
            "a.-b, true, false", // Dot followed by dash

            "192.168.1.1, true, false", // IP address
            "999.999.999.999, true, false", // Invalid IP address format, but looks like an IP
            "192.168.1.word, true, true", // IP-like but with letters

            "a-b.c-d, true, true", // Complex valid name with dots and dashes
            "a0b1c2, true, true", // Alphanumeric mix
            "123, true, true", // Only numbers
            "abc123, true, true", // Letters followed by numbers
            "123abc, true, true", // Numbers followed by letters
            "a0.b1.c2, true, true", // Alphanumeric with dots

            "abc_def, true, false", // Contains underscore
            "abcDef, true, false", // Contains uppercase
            "abc#def, true, false" // Contains special char
    })
    public void isVirtualHostableBucket(String value, boolean allowDots, boolean expected) {
        boolean actual = IsVirtualHostableS3Bucket.isVirtualHostableBucket(value, allowDots);

        if (expected != actual) {
            Assertions.fail("Expected `" + value + "`, allowDots=" + allowDots + " to be " + expected);
        }
    }
}
