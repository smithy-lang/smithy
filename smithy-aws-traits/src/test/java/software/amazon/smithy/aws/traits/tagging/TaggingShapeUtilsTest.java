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

    public static List<Arguments> tagMembers() {
        return ListUtils.of(
                // accepted plural / list forms (existing)
                Arguments.of("Tags", true),
                Arguments.of("tags", true),
                Arguments.of("TagList", true),
                Arguments.of("tagList", true),
                Arguments.of("Taglist", true),
                Arguments.of("taglist", true),
                // accepted map forms (new)
                Arguments.of("TagMap", true),
                Arguments.of("tagMap", true),
                Arguments.of("Tagmap", true),
                Arguments.of("tagmap", true),
                Arguments.of("TagsMap", true),
                Arguments.of("tagsMap", true),
                Arguments.of("Tagsmap", true),
                Arguments.of("tagsmap", true),
                // rejected
                Arguments.of("", false),
                Arguments.of("Tag", false),
                Arguments.of("tag", false),
                Arguments.of("Tagger", false),
                Arguments.of("tagging", false),
                Arguments.of("mytags", false),
                Arguments.of("Maptag", false),
                Arguments.of("Maps", false));
    }

    @ParameterizedTest
    @MethodSource("tagMembers")
    public void isTagDesiredName(String memberName, boolean expected) {
        assertEquals(expected, TaggingShapeUtils.isTagDesiredName(memberName));
    }
}
