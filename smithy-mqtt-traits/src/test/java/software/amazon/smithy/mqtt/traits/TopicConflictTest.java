/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.mqtt.traits;

import java.util.Arrays;
import java.util.Collection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class TopicConflictTest {
    @ParameterizedTest
    @MethodSource("data")
    public void patternConflicts(String topicA, String topicB, boolean isConflicting) {
        Topic a = Topic.parse(topicA);
        Topic b = Topic.parse(topicB);

        if (a.conflictsWith(b) != isConflicting) {
            if (isConflicting) {
                Assertions.fail("Expected conflict between `" + a + "` and `" + b + "`");
            } else {
                Assertions.fail("Unexpected conflict between `" + a + "` and `" + b + "`");
            }
        }
    }

    public static Collection<Object[]> data() {
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
                {"$aws/things/{thingName}/jobs/get", "$aws/things/{thingName}/jobs/start-next", false}
        });
    }
}
