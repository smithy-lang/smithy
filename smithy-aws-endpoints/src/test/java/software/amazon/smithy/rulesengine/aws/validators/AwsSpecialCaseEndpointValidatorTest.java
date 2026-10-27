/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.aws.validators;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class AwsSpecialCaseEndpointValidatorTest {
    @ParameterizedTest
    @MethodSource("patternCases")
    public void findsPatterns(String endpoint, List<String> expected) {
        assertEquals(expected, AwsSpecialCaseEndpointValidator.findPatterns(endpoint));
    }

    public static Stream<Arguments> patternCases() {
        return Stream.of(
                Arguments.of("https://{region}.{x}", List.of("{region}", "{x}")),
                Arguments.of("{}", List.of("{}")),
                Arguments.of("{a{b}", List.of("{a{b}")),
                Arguments.of("a}{b", List.of()),
                Arguments.of("{a\nb}{", List.of("{a\nb}")),
                Arguments.of("https://example.com", List.of()));
    }
}
