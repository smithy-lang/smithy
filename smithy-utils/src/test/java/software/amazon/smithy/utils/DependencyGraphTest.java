/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

public class DependencyGraphTest {
    @Test
    public void addsNodes() {
        DependencyGraph<String> graph = new DependencyGraph<>();
        assertTrue(graph.isEmpty());

        assertTrue(graph.add("foo"));
        assertFalse(graph.isEmpty());
        assertEquals(1, graph.size());
        assertThat(graph, contains("foo"));

        assertFalse(graph.add("foo"));
        assertEquals(1, graph.size());
        assertThat(graph, contains("foo"));

        List<String> newNodes = ListUtils.of("foo", "bar", "baz");
        assertTrue(graph.addAll(newNodes));
        assertFalse(graph.isEmpty());
        assertEquals(3, graph.size());
        assertThat(graph, contains("foo", "bar", "baz"));

        assertFalse(graph.addAll(newNodes));
        assertEquals(3, graph.size());
        assertThat(graph, contains("foo", "bar", "baz"));
    }

    @Test
    public void constructsFromCollection() {
        List<String> nodes = ListUtils.of("foo", "bar", "baz");
        DependencyGraph<String> graph = new DependencyGraph<>(nodes);
        assertFalse(graph.isEmpty());
        assertEquals(3, graph.size());
        assertThat(graph, contains("foo", "bar", "baz"));
    }

    @Test
    public void createsArrays() {
        List<String> nodes = ListUtils.of("foo", "bar", "baz");
        DependencyGraph<String> graph = new DependencyGraph<>(nodes);
        String[] expected = new String[] {"foo", "bar", "baz"};
        assertArrayEquals(expected, graph.toArray());
        assertArrayEquals(expected, graph.toArray(new String[0]));
    }

    @Test
    public void iteratesAllElements() {
        List<String> nodes = ListUtils.of("foo", "bar", "baz");
        DependencyGraph<String> graph = new DependencyGraph<>(nodes);
        List<String> iterated = new ArrayList<>();
        for (String node : graph) {
            iterated.add(node);
        }
        assertEquals(nodes, iterated);
    }

    @Test
    public void addsDependency() {
        List<String> nodes = ListUtils.of("foo", "bar", "baz");
        DependencyGraph<String> graph = new DependencyGraph<>(nodes);

        graph.addDependency("foo", "bar");
        assertThat(graph.getDirectDependencies("foo"), contains("bar"));
        assertThat(graph.getDirectDependants("bar"), contains("foo"));
    }

    @Test
    public void addsDependencies() {
        List<String> nodes = ListUtils.of("foo", "bar", "baz");
        DependencyGraph<String> graph = new DependencyGraph<>(nodes);

        graph.addDependencies("foo", ListUtils.of("bar", "baz"));
        assertThat(graph.getDirectDependencies("foo"), contains("bar", "baz"));
        assertThat(graph.getDirectDependants("bar"), contains("foo"));
        assertThat(graph.getDirectDependants("baz"), contains("foo"));
    }

    @Test
    public void addDependenciesAddsMissingNodes() {
        DependencyGraph<String> graph = new DependencyGraph<>();
        graph.addDependency("spam", "eggs");
        assertThat(graph, contains("spam", "eggs"));

        graph = new DependencyGraph<>();
        graph.addDependencies("foo", ListUtils.of("bar", "baz"));
        assertThat(graph, contains("foo", "bar", "baz"));
    }

    @Test
    public void removesNodes() {
        DependencyGraph<String> graph = new DependencyGraph<>();
        graph.add("foo");
        assertThat(graph, contains("foo"));

        assertTrue(graph.remove("foo"));
        assertTrue(graph.isEmpty());
        assertFalse(graph.remove("foo"));

        graph.addAll(ListUtils.of("foo", "bar", "baz"));
        assertThat(graph, contains("foo", "bar", "baz"));
        assertTrue(graph.removeAll(ListUtils.of("foo", "bar", "baz")));
        assertTrue(graph.isEmpty());
        assertFalse(graph.removeAll(ListUtils.of("foo", "bar", "baz")));

        graph.addAll(ListUtils.of("foo", "bar", "baz"));
        assertThat(graph, contains("foo", "bar", "baz"));
        graph.clear();
        assertTrue(graph.isEmpty());
    }

    @Test
    public void removedNodesAreRemovedFromDependencies() {
        DependencyGraph<String> graph = new DependencyGraph<>();
        graph.addDependency("foo", "bar");
        assertThat(graph, contains("foo", "bar"));
        assertThat(graph.getDirectDependencies("foo"), contains("bar"));
        assertThat(graph.getDirectDependants("bar"), contains("foo"));

        graph.remove("bar");
        assertThat(graph, contains("foo"));
        assertTrue(graph.getDirectDependencies("foo").isEmpty());
        assertNull(graph.getDirectDependants("bar"));
    }

    @Test
    public void removesDependency() {
        DependencyGraph<String> graph = new DependencyGraph<>();
        graph.addDependency("spam", "eggs");
        assertThat(graph, contains("spam", "eggs"));
        assertThat(graph.getDirectDependencies("spam"), contains("eggs"));

        graph.removeDependency("spam", "eggs");
        assertThat(graph, contains("spam", "eggs"));
        assertTrue(graph.getDirectDependencies("spam").isEmpty());
    }

    @Test
    public void removesDependencies() {
        DependencyGraph<String> graph = new DependencyGraph<>();
        graph.addDependencies("foo", ListUtils.of("bar", "baz"));
        assertThat(graph, contains("foo", "bar", "baz"));
        assertThat(graph.getDirectDependencies("foo"), contains("bar", "baz"));

        graph.removeDependencies("foo", ListUtils.of("bar", "baz"));
        assertThat(graph, contains("foo", "bar", "baz"));
        assertTrue(graph.getDirectDependencies("foo").isEmpty());
    }

    @Test
    public void setsDependencies() {
        DependencyGraph<String> graph = new DependencyGraph<>();
        graph.addDependencies("foo", ListUtils.of("bar", "baz"));
        assertThat(graph, contains("foo", "bar", "baz"));
        assertThat(graph.getDirectDependencies("foo"), contains("bar", "baz"));

        graph.setDependencies("foo", ListUtils.of("baz", "bam"));
        assertThat(graph.getDirectDependencies("foo"), contains("baz", "bam"));
        assertThat(graph, contains("foo", "bar", "baz", "bam"));
    }

    @Test
    public void retainsNodes() {
        List<String> nodes = ListUtils.of("foo", "bar", "baz");
        DependencyGraph<String> graph = new DependencyGraph<>(nodes);
        assertThat(graph, contains("foo", "bar", "baz"));

        assertTrue(graph.retainAll(ListUtils.of("foo", "baz")));
        assertThat(graph, contains("foo", "baz"));
        assertFalse(graph.retainAll(ListUtils.of("foo", "baz")));
    }

    @Test
    public void returnsNullEdgesForMissingNodes() {
        DependencyGraph<String> graph = new DependencyGraph<>();
        assertTrue(graph.isEmpty());
        assertNull(graph.getDirectDependencies("foo"));
        assertNull(graph.getDirectDependants("bar"));
    }

    @Test
    public void getsIndependentNodes() {
        DependencyGraph<String> graph = new DependencyGraph<>();
        assertTrue(graph.getIndependentNodes().isEmpty());

        graph.addDependency("spam", "eggs");
        graph.add("foo");
        assertThat(graph, contains("spam", "eggs", "foo"));

        assertThat(graph.getIndependentNodes(), contains("eggs", "foo"));
    }

    @Test
    public void topologicallySorts() {
        List<String> nodes = ListUtils.of("foo", "bar", "baz");
        DependencyGraph<String> graph = new DependencyGraph<>(nodes);
        graph.addDependency("bar", "baz");
        List<String> actual = graph.toSortedList();
        List<String> expected = ListUtils.of("foo", "baz", "bar");
        assertEquals(expected, actual);
    }

    @Test
    public void sortsComplexGraph() {
        DependencyGraph<String> graph = new DependencyGraph<>();
        graph.addDependencies("a", ListUtils.of("b", "c"));
        graph.addDependencies("b", ListUtils.of("c", "d"));
        graph.addDependency("c", "d");
        List<String> actual = graph.toSortedList();
        List<String> expected = ListUtils.of("d", "c", "b", "a");
        assertEquals(expected, actual);
    }

    @Test
    public void topologicallySortsWithCustomComparator() {
        List<String> nodes = ListUtils.of("foo", "bar", "baz");
        DependencyGraph<String> graph = new DependencyGraph<>(nodes);
        graph.addDependency("bar", "baz");
        List<String> actual = graph.toSortedList(String.CASE_INSENSITIVE_ORDER);
        List<String> expected = ListUtils.of("baz", "bar", "foo");
        assertEquals(expected, actual);
    }

    @Test()
    public void sortedListThrowsErrorOnCycle() {
        DependencyGraph<String> graph = new DependencyGraph<>();
        graph.add("foo");
        graph.addDependency("bar", "foo");
        graph.addDependency("spam", "eggs");
        graph.addDependency("eggs", "spam");

        CycleException exception = assertThrows(CycleException.class, graph::toSortedList);
        assertThat(exception.getSortedNodes(String.class), contains("foo", "bar"));
        assertThat(exception.getCyclicNodes(String.class), containsInAnyOrder("spam", "eggs"));
    }

    @Test
    public void detectsSimpleCycle() {
        DependencyGraph<String> graph = new DependencyGraph<>();
        graph.addDependency("spam", "eggs");
        graph.addDependency("eggs", "spam");
        assertThat(graph.findCycles(), contains(ListUtils.of("eggs", "spam")));
    }

    @Test
    public void detectsComplexCycle() {
        DependencyGraph<String> graph = new DependencyGraph<>();
        graph.addDependency("a", "b");
        graph.addDependency("b", "c");
        graph.addDependency("c", "d");
        graph.addDependency("d", "b");
        assertThat(graph.findCycles(), contains(ListUtils.of("c", "d", "b")));
    }

    @Test
    public void detectsMultipleCycles() {
        DependencyGraph<String> graph = new DependencyGraph<>();
        graph.addDependency("spam", "eggs");
        graph.addDependency("eggs", "spam");
        graph.addDependency("a", "b");
        graph.addDependency("b", "c");
        graph.addDependency("c", "d");
        graph.addDependency("d", "b");
        assertThat(graph.findCycles(),
                containsInAnyOrder(
                        ListUtils.of("eggs", "spam"),
                        ListUtils.of("c", "d", "b")));

    }
}
