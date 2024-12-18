/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.utils.ListUtils;

public class IntegrationTopologicalSortTest {

    private static final class MySettings {}

    private static final class MyIntegration implements SmithyIntegration<
            MySettings,
            MySimpleWriter,
            CodegenContext<MySettings, MySimpleWriter, MyIntegration>> {
        private final String name;
        private final byte priority;
        private final List<String> runBefore;
        private final List<String> runAfter;

        MyIntegration(String name) {
            this(name, (byte) 0);
        }

        MyIntegration(String name, byte priority) {
            this(name, priority, Collections.emptyList());
        }

        MyIntegration(String name, byte priority, List<String> runBefore) {
            this(name, priority, runBefore, Collections.emptyList());
        }

        MyIntegration(String name, byte priority, List<String> runBefore, List<String> runAfter) {
            this.name = name;
            this.priority = priority;
            this.runBefore = runBefore;
            this.runAfter = runAfter;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public byte priority() {
            return priority;
        }

        @Override
        public List<String> runBefore() {
            return runBefore;
        }

        @Override
        public List<String> runAfter() {
            return runAfter;
        }

        @Override
        public String toString() {
            return name();
        }
    }

    static List<String> toStrings(List<? extends SmithyIntegration<?, ?, ?>> integrations) {
        return SmithyIntegration.sort(integrations)
                .stream()
                .map(SmithyIntegration::name)
                .collect(Collectors.toList());
    }

    @Test
    public void sortsIntegrationsWithAllDefaults() {
        List<MyIntegration> integrations = new ArrayList<>();
        integrations.add(new MyIntegration("a"));
        integrations.add(new MyIntegration("b"));
        integrations.add(new MyIntegration("c"));
        integrations.add(new MyIntegration("d"));

        List<String> result = toStrings(SmithyIntegration.sort(integrations));

        // Note that insertion order is maintained.
        assertThat(result, contains("a", "b", "c", "d"));
    }

    @Test
    public void sortsIntegrationsBasedOnlyOnPriority1() {
        List<MyIntegration> integrations = new ArrayList<>();
        integrations.add(new MyIntegration("a", (byte) 0));
        integrations.add(new MyIntegration("b", (byte) 1));
        integrations.add(new MyIntegration("c", (byte) 2));
        integrations.add(new MyIntegration("d", (byte) 3));

        List<String> result = toStrings(SmithyIntegration.sort(integrations));

        assertThat(result, contains("d", "c", "b", "a"));
    }

    @Test
    public void sortsIntegrationsBasedOnlyOnPriority2() {
        List<MyIntegration> integrations = new ArrayList<>();
        integrations.add(new MyIntegration("a", (byte) 2));
        integrations.add(new MyIntegration("b", (byte) 0));
        integrations.add(new MyIntegration("c", (byte) 1));
        integrations.add(new MyIntegration("d", (byte) 3));

        List<String> result = toStrings(SmithyIntegration.sort(integrations));

        assertThat(result, contains("d", "a", "c", "b"));
    }

    @Test
    public void sortsIntegrationsBasedOnBefore() {
        List<MyIntegration> integrations = new ArrayList<>();
        integrations.add(new MyIntegration("a", (byte) 0, ListUtils.of("b")));
        integrations.add(new MyIntegration("b", (byte) 0, ListUtils.of("c")));
        integrations.add(new MyIntegration("c", (byte) 0, ListUtils.of("d")));
        integrations.add(new MyIntegration("d", (byte) 0));

        List<String> result = toStrings(SmithyIntegration.sort(integrations));

        assertThat(result, contains("a", "b", "c", "d"));
    }

    @Test
    public void sortsIntegrationsBasedOnAfterAndPriority() {
        List<MyIntegration> integrations = new ArrayList<>();
        integrations.add(new MyIntegration("a", (byte) 0, ListUtils.of(), ListUtils.of("b")));
        integrations.add(new MyIntegration("b"));
        integrations.add(new MyIntegration("c", (byte) 50, ListUtils.of(), ListUtils.of("b")));
        integrations.add(new MyIntegration("d", (byte) -50, ListUtils.of(), ListUtils.of("b")));

        List<String> result = toStrings(SmithyIntegration.sort(integrations));

        assertThat(result, contains("b", "c", "a", "d"));
    }

    @Test
    public void sortsIntegrationsBasedOnBeforeAfterAndPriority() {
        List<MyIntegration> integrations = new ArrayList<>();
        // c (before d) -> a (before b) -> b (before d) -> d
        integrations.add(new MyIntegration("a", (byte) 0, ListUtils.of("b"), ListUtils.of("c")));
        integrations.add(new MyIntegration("b", (byte) 0, ListUtils.of("d"), ListUtils.of("c")));
        integrations.add(new MyIntegration("c", (byte) 50, ListUtils.of("d")));
        integrations.add(new MyIntegration("d", (byte) -50, ListUtils.of(), ListUtils.of("a")));

        List<String> result = toStrings(SmithyIntegration.sort(integrations));

        assertThat(result, contains("c", "a", "b", "d"));
    }

    @Test
    public void detectsCycles() {
        List<MyIntegration> integrations = new ArrayList<>();
        integrations.add(new MyIntegration("a", (byte) 0, ListUtils.of("b"), ListUtils.of("c")));
        integrations.add(new MyIntegration("b", (byte) 0, ListUtils.of("d"), ListUtils.of("c")));
        integrations.add(new MyIntegration("c", (byte) 0, ListUtils.of("d")));
        integrations.add(new MyIntegration("d", (byte) 0, ListUtils.of("b"), ListUtils.of("a")));

        RuntimeException e = Assertions.assertThrows(IllegalArgumentException.class,
                () -> SmithyIntegration.sort(integrations));
        assertThat(e.getMessage(), equalTo("SmithyIntegration cycles detected among [b, d]"));
    }

    @Test
    public void detectsConflictingNames() {
        List<MyIntegration> integrations = new ArrayList<>();
        integrations.add(new MyIntegration("a"));
        integrations.add(new MyIntegration("a"));

        RuntimeException e = Assertions.assertThrows(IllegalArgumentException.class,
                () -> SmithyIntegration.sort(integrations));
        assertThat(e.getMessage(),
                equalTo(
                        "Conflicting SmithyIntegration names detected for 'a': software.amazon.smithy.codegen.core.IntegrationTopologicalSortTest.MyIntegration "
                                + "and software.amazon.smithy.codegen.core.IntegrationTopologicalSortTest.MyIntegration"));
    }

    @Test
    public void dependenciesAreSoft() {
        List<MyIntegration> integrations = new ArrayList<>();
        integrations.add(new MyIntegration("a", (byte) 0, ListUtils.of("foo"), ListUtils.of("baz")));
        integrations.add(new MyIntegration("b", (byte) 10, ListUtils.of("bam"), ListUtils.of("boo")));

        List<String> result = toStrings(SmithyIntegration.sort(integrations));

        assertThat(result, contains("b", "a"));
    }
}
