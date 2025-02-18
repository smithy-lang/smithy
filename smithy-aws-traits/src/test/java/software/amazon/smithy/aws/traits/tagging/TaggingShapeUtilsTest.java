/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.traits.tagging;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.smithy.utils.ListUtils;

public class TaggingShapeUtilsTest {
    public static List<Arguments> arnMembers() {
        return ListUtils.of(
                Arguments.of("", false),
                Arguments.of("foo", false),
                Arguments.of("fooArn", false),
                Arguments.of("resourceFoo", false),
                Arguments.of("arnResource", false),
                Arguments.of("resourceResourceArn", false),
                Arguments.of("resourceArnARN", false),
                Arguments.of("resourceArn", true),
                Arguments.of("resource", true),
                Arguments.of("Resource", true),
                Arguments.of("arn", true),
                Arguments.of("Arn", true),
                Arguments.of("ARN", true),
                Arguments.of("ResourceARN", true));
    }

    @ParameterizedTest
    @MethodSource("arnMembers")
    public void isArnMemberDesiredName(String arnMember, boolean expected) {
        assertEquals(expected, TaggingShapeUtils.isArnMemberDesiredName(arnMember));
    }
}
