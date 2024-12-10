/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.MapUtils;

public class DependencyTrackerTest {
    @Test
    public void canAddAndQueryDependencies() {
        DependencyTracker tracker = new DependencyTracker();
        tracker.addDependency("a", "b", "c");
        tracker.addDependency("d", "e", "f");
        tracker.addDependency(SymbolDependency.builder().packageName("h").version("i").dependencyType("j").build());

        assertThat(tracker.getDependencies(), hasSize(3));
        assertThat(tracker.getByName("a").getPackageName(), equalTo("a"));
        assertThat(tracker.getByName("d").getPackageName(), equalTo("d"));
        assertThat(tracker.getByName("h").getPackageName(), equalTo("h"));

        assertThat(tracker.getByName("a", "c").getPackageName(), equalTo("a"));
        assertThat(tracker.getByName("d", "f").getPackageName(), equalTo("d"));
        assertThat(tracker.getByName("h", "j").getPackageName(), equalTo("h"));

        assertThat(tracker.getByProperty("nope"), empty());
        assertThat(tracker.getByProperty("nope", 10), empty());

        assertThat(tracker.getByType("c"), hasSize(1));
        assertThat(tracker.getByType("f"), hasSize(1));
        assertThat(tracker.getByType("j"), hasSize(1));
    }

    @Test
    public void throwsWhenNotFoundByName() {
        DependencyTracker tracker = new DependencyTracker();

        Assertions.assertThrows(IllegalArgumentException.class, () -> tracker.getByName("foo"));
    }

    @Test
    public void throwsWhenNotFoundByNameAndType() {
        DependencyTracker tracker = new DependencyTracker();

        Assertions.assertThrows(IllegalArgumentException.class, () -> tracker.getByName("foo", "baz"));
    }

    @Test
    public void canQueryByProperty() {
        DependencyTracker tracker = new DependencyTracker();
        SymbolDependency a = SymbolDependency.builder().packageName("a").version("1").putProperty("x", 1).build();
        SymbolDependency b = SymbolDependency.builder().packageName("b").version("1").putProperty("x", 2).build();
        SymbolDependency c = SymbolDependency.builder().packageName("c").version("1").putProperty("y", true).build();
        tracker.addDependency(a);
        tracker.addDependency(b);
        tracker.addDependency(c);

        assertThat(tracker.getByProperty("x"), contains(a, b));
        assertThat(tracker.getByProperty("y"), contains(c));
        assertThat(tracker.getByProperty("x", 1), contains(a));
        assertThat(tracker.getByProperty("x", 2), contains(b));
        assertThat(tracker.getByProperty("x", 3), empty());
        assertThat(tracker.getByProperty("y", true), contains(c));
        assertThat(tracker.getByProperty("y", false), empty());
    }

    @Test
    public void canAddFromContainer() {
        DependencyTracker tracker = new DependencyTracker();
        SymbolDependency a = SymbolDependency.builder().packageName("a").version("1").putProperty("x", 1).build();
        SymbolDependency b = SymbolDependency.builder().packageName("b").version("1").putProperty("x", 2).build();
        tracker.addDependency(a);
        tracker.addDependency(b);

        DependencyTracker tracker2 = new DependencyTracker();
        SymbolDependency c = SymbolDependency.builder().packageName("c").version("1").putProperty("y", true).build();
        tracker2.addDependency(c);

        tracker.addDependencies(tracker2);

        assertThat(tracker.getByName("c").getPackageName(), equalTo("c"));
        assertThat(tracker.getDependencies(), hasSize(3));
        assertThat(tracker2.getDependencies(), hasSize(1));
    }

    @Test
    public void canLoadDependenciesFromJsonFiles() {
        DependencyTracker tracker = new DependencyTracker();
        tracker.addDependenciesFromJson(getClass().getResource("dependencies-test.json"));

        // Multiple dependencies can appear with the same name.
        assertThat(tracker.getDependencies(), hasSize(4));

        // First by this name is "test".
        assertThat(tracker.getByName("Test1").getDependencyType(), equalTo("test"));
        assertThat(tracker.getByName("Test1").getVersion(), equalTo("1"));
        assertThat(tracker.getByName("Test1", "runtime").getVersion(), equalTo("1-prod"));

        assertThat(tracker.getByName("Test2").getDependencyType(), equalTo(""));

        assertThat(tracker.getByName("Test3").getProperty("foo").get(), equalTo(true));
        assertThat(tracker.getByName("Test3").getProperty("foo").get(), equalTo(true));
        assertThat(tracker.getByName("Test3").getProperty("bar").get(), equalTo(ListUtils.of("a")));
        assertThat(tracker.getByName("Test3").getProperty("baz").get(), equalTo(MapUtils.of("greeting", "hi")));
    }
}
