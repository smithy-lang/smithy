/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.mqtt.traits;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class TopicTest {
    @Test
    public void requiresThatLabelsSpanWholeLevel() {
        assertThrows(TopicSyntaxException.class, () -> Topic.parse("foo/bar/{baz}bam"));
    }

    @Test
    public void requiresThatLabelsContainOneCharacter() {
        assertThrows(TopicSyntaxException.class, () -> Topic.parse("foo/bar/{}"));
    }

    @Test
    public void requiresThatLabelsContainValidCharacters() {
        assertThrows(TopicSyntaxException.class, () -> Topic.parse("foo/bar/{nope nope}"));
    }

    @Test
    public void doesNotAllowDuplicateLabels() {
        assertThrows(TopicSyntaxException.class, () -> Topic.parse("foo/bar/{nope}/{nope}"));
    }

    @Test
    public void doesNotSupportSingleLevelWildCards() {
        assertThrows(TopicSyntaxException.class, () -> Topic.parse("foo/bar/+/nope"));
    }

    @Test
    public void doesNotSupportMultiLevelWildCards() {
        assertThrows(TopicSyntaxException.class, () -> Topic.parse("foo/bar/nope/#"));
    }

    @Test
    public void detectsLabelSyntaxError() {
        assertThrows(TopicSyntaxException.class, () -> Topic.parse("foo/bar/nope/}"));
    }

    @Test
    public void parsesTopicWithNoLabels() {
        Topic topic = Topic.parse("foo/bar/baz");

        assertThat(topic.toString(), equalTo("foo/bar/baz"));
        assertThat(topic.getLevels(),
                contains(
                        new Topic.Level("foo"),
                        new Topic.Level("bar"),
                        new Topic.Level("baz")));
        assertThat(topic.conflictsWith(topic), is(true));
        assertThat(topic.getLabels(), empty());
        assertFalse(topic.hasLabel("foo"));
        assertThat(topic, equalTo(topic));
    }

    @Test
    public void parsesTopicWithLabels() {
        Topic topic = Topic.parse("foo/{foo}/bar/{baz}");

        assertThat(topic, equalTo(topic));
        assertThat(topic.toString(), equalTo("foo/{foo}/bar/{baz}"));

        assertThat(topic.getLevels(),
                contains(
                        new Topic.Level("foo"),
                        new Topic.Level("foo", true),
                        new Topic.Level("bar"),
                        new Topic.Level("baz", true)));
        assertThat(topic.getLabels(),
                contains(
                        new Topic.Level("foo", true),
                        new Topic.Level("baz", true)));

        assertTrue(topic.hasLabel("foo"));
        assertTrue(topic.hasLabel("baz"));
        assertFalse(topic.hasLabel("bar"));
    }

    @Test
    public void topicEquality() {
        Topic topic1 = Topic.parse("foo/bar");
        Topic topic2 = Topic.parse("foo/{bar}");

        assertThat(topic1, equalTo(topic1));
        assertThat(topic1, not(equalTo(topic2)));
        assertThat(topic1, not(equalTo(null)));
    }

    @Test
    public void labelsPrintWithBraces() {
        Topic.Level level = new Topic.Level("foo", true);

        assertTrue(level.isLabel());
        assertThat(level.toString(), equalTo("{foo}"));
    }

    @Test
    public void nonLabelsPrintWithoutBraces() {
        Topic.Level level = new Topic.Level("foo");

        assertFalse(level.isLabel());
        assertThat(level.toString(), equalTo("foo"));
    }

    @Test
    public void levelEquality() {
        Topic.Level level1 = new Topic.Level("foo", true);
        Topic.Level level2 = new Topic.Level("baz", true);
        Topic.Level level3 = new Topic.Level("bar");

        assertThat(level1, equalTo(level1));
        assertThat(level1, not(equalTo(level2)));
        assertThat(level1, not(equalTo(level3)));
        assertThat(level2, not(equalTo(level3)));
        assertThat(level1, not(equalTo(null)));
    }
}
