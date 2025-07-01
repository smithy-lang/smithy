/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.mqtt.traits;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class TopicConflictTest {
    @ParameterizedTest
    @MethodSource("topicCases")
    public void topicPatternConflicts(String topicA, String topicB, boolean isConflicting) {
        Topic a = Topic.parse(Topic.TopicRules.TOPIC, topicA);
        Topic b = Topic.parse(Topic.TopicRules.TOPIC, topicB);

        if (a.conflictsWith(b) != isConflicting) {
            if (isConflicting) {
                Assertions.fail("Expected conflict between `" + a + "` and `" + b + "`");
            } else {
                Assertions.fail("Unexpected conflict between `" + a + "` and `" + b + "`");
            }
        }
    }

    public static Collection<Object[]> topicCases() {
        return Arrays.asList(new Object[][] {
                // No conflict because a is more specific.
                {"a", "{x}", false},
                // No conflict because "a" is more specific than "{y}".
                {"a/{x}", "{y}/a", false},
                // No conflict because "a" is more specific than "{x}".
                {"{x}/a", "a/{y}", false},
                // Conflicts because the topics are equivalent and the same length.
                {"a/{x}", "a/{y}", true},
                // Does not conflict because "{x}" and "{y}" are under different level prefixes.
                {"a/{x}", "b/{y}", false},
                // Conflicts because they have the same levels and the same length.
                {"a/{x}/b", "a/{y}/b", true},
                // Does not conflict because one is longer than the other.
                {"a/{x}/b", "a/{y}/b/{z}", false},
                // Does not conflict because one is longer than the other.
                {"a/{x}/b", "a/{y}/b/{z}/c", false},
                // Do not conflict because "b" is more specific than "{b}"
                {"a/b/c", "a/{b}/c", false},
                // Conflicts because they are all labels at the same level.
                {"{a}/{b}/{c}", "{x}/{y}/{z}", true},
                // No conflicts because one is longer than the other.
                {"{a}/{b}/{c}", "{x}/{y}/{z}/{a}", false},
                // No conflict
                {"a/b/c/d", "a/{b}/c/{d}", false},
                // No conflict.
                {"$aws/things/{thingName}/jobs/get", "$aws/things/{thingName}/jobs/start-next", false},
                // No conflict, empty second level creates mismatch with single-level topic
                {"a/", "a", false}
        });
    }

    @ParameterizedTest
    @MethodSource("topicFilterCases")
    public void topicFilterPatternConflicts(String topicA, String topicB, boolean isConflicting) {
        Topic a = Topic.parse(Topic.TopicRules.FILTER, topicA);
        Topic b = Topic.parse(Topic.TopicRules.FILTER, topicB);

        if (a.conflictsWith(b) != isConflicting) {
            if (isConflicting) {
                List<String> aLevels = a.getLevels().stream().map(Topic.Level::toString).collect(Collectors.toList());
                String aMarkedTopic = String.join("@", aLevels);

                List<String> bLevels = b.getLevels().stream().map(Topic.Level::toString).collect(Collectors.toList());
                String bMarkedTopic = String.join("@", bLevels);

                Assertions.fail("Expected conflict between `" + aMarkedTopic + "` and `" + bMarkedTopic + "`");
            } else {
                Assertions.fail("Unexpected conflict between `" + a + "` and `" + b + "`");
            }
        }
    }

    public static Collection<Object[]> topicFilterCases() {
        return Arrays.asList(new Object[][] {
                // No conflict because a is more specific.
                {"a", "{x}", false},
                // No conflict because "a" is more specific than "{y}".
                {"a/{x}", "{y}/a", false},
                // No conflict because "a" is more specific than "{x}".
                {"{x}/a", "a/{y}", false},
                // Conflicts because the topics are equivalent and the same length.
                {"a/{x}", "a/{y}", true},
                // Does not conflict because "{x}" and "{y}" are under different level prefixes.
                {"a/{x}", "b/{y}", false},
                // Conflicts because they have the same levels and the same length.
                {"a/{x}/b", "a/{y}/b", true},
                // Does not conflict because one is longer than the other.
                {"a/{x}/b", "a/{y}/b/{z}", false},
                // Does not conflict because one is longer than the other.
                {"a/{x}/b", "a/{y}/b/{z}/c", false},
                // Do not conflict because "b" is more specific than "{b}"
                {"a/b/c", "a/{b}/c", false},
                // Conflicts because they are all labels at the same level.
                {"{a}/{b}/{c}", "{x}/{y}/{z}", true},
                // No conflicts because one is longer than the other.
                {"{a}/{b}/{c}", "{x}/{y}/{z}/{a}", false},
                // No conflict
                {"a/b/c/d", "a/{b}/c/{d}", false},
                // No conflict.
                {"$aws/things/{thingName}/jobs/get", "$aws/things/{thingName}/jobs/start-next", false},
                // Conflicts because multi-level wild card matches rest of path
                {"a/#", "a/b/c/d", true},
                // Conflicts becase single-level wild card matches segment
                {"a/+/c", "a/b/c", true},
                // Conflicts becase single-level wild card matches label segment
                {"a/{b}/c", "a/+/c", true},
                // No conflict because single-level wildcard doesn't match multi-segment "b/c"
                {"a/+/c", "a/b/c/d", false},
                // Conflicts because '#' matches everything
                {"#", "/", true},
                // Conflicts because '#' matches everything
                {"+/a", "#", true},
                // Conflicts because 'a/a' matches both
                {"+/a", "a/+", true},
                // Conflicts because single-level wildcard matches empty segments
                {"+/+", "/", true},
                // Conflict because wildcard matches empty level
                {"/", "+/", true},
                // Conflict because wildcard matches empty level
                {"/+", "/", true},
        });
    }
}
