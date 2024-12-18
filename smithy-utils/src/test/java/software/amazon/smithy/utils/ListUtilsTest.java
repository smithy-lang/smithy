/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;

import java.util.Collections;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

public class ListUtilsTest {
    @Test
    public void copyOfEmptyIsEmpty() {
        assertThat(ListUtils.copyOf(Collections.emptyList()), empty());
    }

    @Test
    public void copyOfIsSame() {
        assertThat(ListUtils.copyOf(Collections.singletonList("Jason")), contains("Jason"));
    }

    @Test
    public void ofEmptyIsEmpty() {
        assertThat(ListUtils.of(), empty());
    }

    @Test
    public void ofOneIsOne() {
        assertThat(ListUtils.of("Jason"), contains("Jason"));
    }

    @Test
    public void ofManyIsMany() {
        assertThat(ListUtils.of("Jason", "Michael", "Kevin"), containsInAnyOrder("Jason", "Michael", "Kevin"));
    }

    @Test
    public void collectsToList() {
        assertThat(Stream.of("Jason", "Michael", "Kevin").collect(ListUtils.toUnmodifiableList()),
                containsInAnyOrder("Jason", "Michael", "Kevin"));
    }
}
