/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInRelativeOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

import java.util.List;
import java.util.Map;
import java.util.Set;
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

    @Test
    public void createsSortedMaps() {
        BuilderRef<Map<String, String>> sortedMap = BuilderRef.forSortedMap();
        sortedMap.get().put("c", "d");
        sortedMap.get().put("a", "b");
        assertThat(sortedMap.peek().keySet(), containsInRelativeOrder("a", "c"));
    }

    @Test
    public void createsSortedMapsWithCustomComparator() {
        BuilderRef<Map<String, String>> sortedMap = BuilderRef.forSortedMap(String.CASE_INSENSITIVE_ORDER);
        sortedMap.get().put("C", "d");
        sortedMap.get().put("a", "b");
        assertThat(sortedMap.peek().keySet(), containsInRelativeOrder("a", "C"));
    }

    @Test
    public void createsSortedSets() {
        BuilderRef<Set<String>> sortedSet = BuilderRef.forSortedSet();
        sortedSet.get().add("b");
        sortedSet.get().add("a");
        assertThat(sortedSet.peek(), containsInRelativeOrder("a", "b"));
    }

    @Test
    public void createsSortedSetsWithCustomComparator() {
        BuilderRef<Set<String>> sortedSet = BuilderRef.forSortedSet(String.CASE_INSENSITIVE_ORDER);
        sortedSet.get().add("B");
        sortedSet.get().add("a");
        assertThat(sortedSet.peek(), containsInRelativeOrder("a", "B"));
    }

    @Test
    public void setBorrowedAllowsPeekWithoutCopy() {
        List<String> immutable = ListUtils.of("a", "b");
        BuilderRef<List<String>> ref = BuilderRef.forList();
        ref.setBorrowed(immutable);

        // peek returns the borrowed value directly
        assertThat(ref.peek(), equalTo(immutable));
        assertThat(ref.hasValue(), equalTo(true));
    }

    @Test
    public void setBorrowedCopiesOnGet() {
        List<String> immutable = ListUtils.of("a", "b");
        BuilderRef<List<String>> ref = BuilderRef.forList();
        ref.setBorrowed(immutable);

        // get() triggers a copy so we can mutate
        List<String> mutable = ref.get();
        mutable.add("c");

        // original is unchanged
        assertThat(immutable.size(), equalTo(2));
        // ref now has the mutated copy
        assertThat(ref.peek(), contains("a", "b", "c"));
    }

    @Test
    public void setBorrowedClearsOwnedState() {
        BuilderRef<List<String>> ref = BuilderRef.forList();
        ref.get().add("x");

        // setBorrowed replaces owned state
        List<String> immutable = ListUtils.of("a", "b");
        ref.setBorrowed(immutable);

        assertThat(ref.peek(), equalTo(immutable));
        assertThat(ref.peek(), contains("a", "b"));
    }

    @Test
    public void setBorrowedWithNullClearsState() {
        BuilderRef<List<String>> ref = BuilderRef.forList();
        ref.get().add("x");

        ref.setBorrowed(null);

        assertThat(ref.hasValue(), equalTo(false));
    }

    @Test
    public void setBorrowedCopyReturnsBorrowedValue() {
        List<String> immutable = ListUtils.of("a", "b");
        BuilderRef<List<String>> ref = BuilderRef.forList();
        ref.setBorrowed(immutable);

        // copy() on a borrowed value copies it and wraps immutably
        List<String> copied = ref.copy();
        assertThat(copied, contains("a", "b"));
    }

    @Test
    public void setBorrowedEnablesToBuilderPattern() {
        // Simulates the toBuilder() optimization:
        // 1. Build an object with copy()
        BuilderRef<Map<String, String>> ref = BuilderRef.forOrderedMap();
        ref.get().put("key", "value");
        Map<String, String> built = ref.copy(); // immutable

        // 2. Create a new builder and setBorrowed (simulates toBuilder)
        BuilderRef<Map<String, String>> ref2 = BuilderRef.forOrderedMap();
        ref2.setBorrowed(built);

        // 3. peek() returns the borrowed value without copy
        assertThat(ref2.peek(), equalTo(built));

        // 4. Mutating via get() doesn't affect the original
        ref2.get().put("key2", "value2");
        assertThat(built.size(), equalTo(1));
        assertThat(ref2.peek().size(), equalTo(2));
    }

    @Test
    public void copyReturnsSameInstanceWhenNotMutatedAfterSetBorrowed() {
        // The key optimization: if setBorrowed is called and no mutation happens,
        // copy() returns the exact same instance (no copy at all).
        BuilderRef<List<String>> ref = BuilderRef.forList();
        ref.get().add("a");
        List<String> original = ref.copy();

        // Simulate toBuilder: setBorrowed with the immutable collection
        BuilderRef<List<String>> ref2 = BuilderRef.forList();
        ref2.setBorrowed(original);

        // copy() without any mutation should return the same instance
        List<String> result = ref2.copy();
        assertThat(result == original, equalTo(true));
    }

    @Test
    public void copyReturnsSameInstanceOnRepeatedCopyCalls() {
        // Even without setBorrowed, repeated copy() calls should reuse the same instance
        BuilderRef<List<String>> ref = BuilderRef.forList();
        ref.get().add("a");

        List<String> first = ref.copy();
        List<String> second = ref.copy();

        assertThat(first == second, equalTo(true));
    }
}
