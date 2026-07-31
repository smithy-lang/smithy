/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

public class SliceMatcherTest {

    private int match(SliceMatcher matcher, String text) {
        char[] chars = ("xx" + text + "xx").toCharArray();
        return matcher.match(chars, 2, text.length());
    }

    @Test
    public void matchesByIndex() {
        SliceMatcher matcher = new SliceMatcher("number", "structure", "string");
        assertThat(match(matcher, "number"), is(0));
        assertThat(match(matcher, "structure"), is(1));
        assertThat(match(matcher, "string"), is(2));
    }

    @Test
    public void returnsNegativeOneWhenNoMatch() {
        SliceMatcher matcher = new SliceMatcher("number", "structure");
        assertThat(match(matcher, "service"), is(-1));
        assertThat(match(matcher, "numbe"), is(-1));
        assertThat(match(matcher, "numbers"), is(-1));
        assertThat(match(matcher, ""), is(-1));
    }

    @Test
    public void distinguishesSameLengthCandidates() {
        // "blob" and "long" are both length 4 and land in the same length bucket.
        SliceMatcher matcher = new SliceMatcher("blob", "long", "byte");
        assertThat(match(matcher, "blob"), is(0));
        assertThat(match(matcher, "long"), is(1));
        assertThat(match(matcher, "byte"), is(2));
        assertThat(match(matcher, "lonb"), is(-1));
    }

    @Test
    public void handlesEmptyMatcher() {
        SliceMatcher matcher = new SliceMatcher();
        assertThat(match(matcher, "anything"), is(-1));
    }

    @Test
    public void matchesAtNonZeroOffset() {
        SliceMatcher matcher = new SliceMatcher("map", "list");
        char[] chars = "prefix:list:suffix".toCharArray();
        assertThat(matcher.match(chars, 7, 4), is(1));
    }

    @Test
    public void lowestIndexWinsOnDuplicates() {
        SliceMatcher matcher = new SliceMatcher("set", "set");
        assertThat(match(matcher, "set"), is(0));
    }
}
