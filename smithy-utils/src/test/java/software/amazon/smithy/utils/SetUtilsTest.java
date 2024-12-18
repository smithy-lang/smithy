/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

public class SetUtilsTest {
    @Test
    public void copyOfEmptyIsEmpty() {
        assertThat(SetUtils.copyOf(Collections.emptyList()), empty());
    }

    @Test
    public void orderedCopyOfEmptyIsEmpty() {
        assertThat(SetUtils.orderedCopyOf(Collections.emptyList()), empty());
    }

    @Test
    public void copyOfIsSame() {
        assertThat(SetUtils.copyOf(Collections.singletonList("Jason")), contains("Jason"));
    }

    @Test
    public void orderedCopyOfIsSame() {
        Set<String> input = new LinkedHashSet<>();
        input.add("A");
        input.add("B");
        Set<String> copy = SetUtils.orderedCopyOf(input);

        assertThat(ListUtils.copyOf(input), equalTo(ListUtils.copyOf(copy)));
    }

    @Test
    public void ofEmptyIsEmpty() {
        assertThat(SetUtils.of(), empty());
    }

    @Test
    public void ofOneIsOne() {
        assertThat(SetUtils.of("Jason"), contains("Jason"));
    }

    @Test
    public void ofManyIsMany() {
        assertThat(SetUtils.of("Jason", "Michael", "Kevin"), containsInAnyOrder("Jason", "Michael", "Kevin"));
    }

    @Test
    public void collectsToSet() {
        assertThat(Stream.of("Jason", "Michael", "Kevin").collect(SetUtils.toUnmodifiableSet()),
                containsInAnyOrder("Jason", "Michael", "Kevin"));
    }
}
