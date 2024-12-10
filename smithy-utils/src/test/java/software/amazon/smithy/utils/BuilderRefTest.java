/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

import java.util.List;
import org.junit.jupiter.api.Test;

public class BuilderRefTest {
    @Test
    public void createsValues() {
        BuilderRef<List<String>> strings = BuilderRef.forList();
        strings.get().add("a");
        strings.get().add("b");

        assertThat(strings.copy(), contains("a", "b"));
    }

    @Test
    public void createsValuesAndCanBuildMore() {
        BuilderRef<List<String>> strings = BuilderRef.forList();
        strings.get().add("a");
        strings.get().add("b");

        List<String> first = strings.copy();
        assertThat(strings.peek(), equalTo(first));

        strings.get().add("c");
        assertThat(strings.peek(), not(equalTo(first)));

        List<String> second = strings.copy();

        assertThat(first, not(equalTo(second)));
        assertThat(second, contains("a", "b", "c"));
    }

    @Test
    public void copiesOfBorrowedValuesCopiesBorrowedValue() {
        BuilderRef<List<String>> strings = BuilderRef.forList();
        strings.get().add("a");

        List<String> first = strings.copy();
        List<String> second = strings.copy();

        assertThat(first, equalTo(second));
    }

    @Test
    public void copyingEmptyListsMightBeOptimized() {
        BuilderRef<List<String>> strings = BuilderRef.forList();

        List<String> first = strings.copy();
        List<String> second = strings.copy();

        assertThat(first, equalTo(second));
    }

    @Test
    public void peekingEmptyListsMightBeOptimized() {
        BuilderRef<List<String>> strings = BuilderRef.forList();

        List<String> first = strings.peek();
        List<String> second = strings.peek();

        assertThat(first, equalTo(second));
    }

    @Test
    public void tracksIfHasValue() {
        BuilderRef<List<String>> strings = BuilderRef.forList();

        assertThat(strings.hasValue(), equalTo(false));

        strings.get().add("a");

        assertThat(strings.hasValue(), equalTo(true));

        strings.clear();

        assertThat(strings.hasValue(), equalTo(false));
    }
}
