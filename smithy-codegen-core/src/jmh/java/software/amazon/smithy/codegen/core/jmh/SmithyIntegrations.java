/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core.jmh;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import software.amazon.smithy.codegen.core.CodegenContext;
import software.amazon.smithy.codegen.core.ImportContainer;
import software.amazon.smithy.codegen.core.SmithyIntegration;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.codegen.core.SymbolWriter;

@Warmup(iterations = 3)
@Measurement(iterations = 3, timeUnit = TimeUnit.MICROSECONDS)
@BenchmarkMode(Mode.AverageTime)
@Fork(1)
public class SmithyIntegrations {

    @State(Scope.Thread)
    public static class SmithyIntegrationsState {
        public Map<String, SmithyIntegration<?, ?, ?>> independentIntegrations10;
        public Map<String, SmithyIntegration<?, ?, ?>> dependentIntegrations10;
        public Map<String, SmithyIntegration<?, ?, ?>> independentIntegrations100;
        public Map<String, SmithyIntegration<?, ?, ?>> dependentIntegrations100;
        public Map<String, SmithyIntegration<?, ?, ?>> independentIntegrations1000;
        public Map<String, SmithyIntegration<?, ?, ?>> dependentIntegrations1000;

        @Setup
        public void setup() {
            independentIntegrations10 = new LinkedHashMap<>(10);
            independentIntegrations100 = new LinkedHashMap<>(100);
            independentIntegrations1000 = new LinkedHashMap<>(1000);
            for (int i = 0; i < 1000; i++) {
                String name = "integration" + i;
                TestIntegration integration = new TestIntegration(name);
                if (i < 10) {
                    independentIntegrations10.put(name, integration);
                }
                if (i < 100) {
                    independentIntegrations100.put(name, integration);
                }
                independentIntegrations1000.put(name, integration);
            }
            independentIntegrations10 = Collections.unmodifiableMap(independentIntegrations10);
            independentIntegrations100 = Collections.unmodifiableMap(independentIntegrations100);
            independentIntegrations1000 = Collections.unmodifiableMap(independentIntegrations1000);

            dependentIntegrations10 = new LinkedHashMap<>(10);
            dependentIntegrations100 = new LinkedHashMap<>(100);
            dependentIntegrations1000 = new LinkedHashMap<>(1000);
            for (int i = 0; i < 1000; i++) {
                String name = "integration" + i;
                String next = "integration" + (i + 1);
                String afterNext = "integration" + (i + 2);

                if (i < 10) {
                    List<String> dependencies = new ArrayList<>();
                    if (i < 9) {
                        dependencies.add(next);
                    }
                    if (i < 8) {
                        dependencies.add(afterNext);
                    }
                    dependentIntegrations10.put(name,
                            new TestIntegration(name, (byte) 0, Collections.emptyList(), dependencies));
                }

                if (i < 100) {
                    List<String> dependencies = new ArrayList<>();
                    if (i < 99) {
                        dependencies.add(next);
                    }
                    if (i < 98) {
                        dependencies.add(afterNext);
                    }
                    dependentIntegrations100.put(name,
                            new TestIntegration(name, (byte) 0, Collections.emptyList(), dependencies));
                }

                List<String> dependencies = new ArrayList<>();
                if (i < 999) {
                    dependencies.add(next);
                }
                if (i < 998) {
                    dependencies.add(afterNext);
                }
                dependentIntegrations1000.put(name,
                        new TestIntegration(name, (byte) 0, Collections.emptyList(), dependencies));
            }
            dependentIntegrations10 = Collections.unmodifiableMap(dependentIntegrations10);
            dependentIntegrations100 = Collections.unmodifiableMap(dependentIntegrations100);
            dependentIntegrations1000 = Collections.unmodifiableMap(dependentIntegrations1000);
        }
    }

    @Benchmark
    public List<SmithyIntegration<?, ?, ?>> sortIndependentIntegrations10(SmithyIntegrationsState state) {
        return SmithyIntegration.sort(state.independentIntegrations10.values());
    }

    @Benchmark
    public List<SmithyIntegration<?, ?, ?>> sortIndependentIntegrations100(SmithyIntegrationsState state) {
        return SmithyIntegration.sort(state.independentIntegrations100.values());
    }

    @Benchmark
    public List<SmithyIntegration<?, ?, ?>> sortIndependentIntegrations1000(SmithyIntegrationsState state) {
        return SmithyIntegration.sort(state.independentIntegrations1000.values());
    }

    @Benchmark
    public List<SmithyIntegration<?, ?, ?>> sortDependentIntegrations10(SmithyIntegrationsState state) {
        return SmithyIntegration.sort(state.dependentIntegrations10.values());
    }

    @Benchmark
    public List<SmithyIntegration<?, ?, ?>> sortDependentIntegrations100(SmithyIntegrationsState state) {
        return SmithyIntegration.sort(state.dependentIntegrations100.values());
    }

    @Benchmark
    public List<SmithyIntegration<?, ?, ?>> sortDependentIntegrations1000(SmithyIntegrationsState state) {
        return SmithyIntegration.sort(state.dependentIntegrations1000.values());
    }

    private static class IntegrationComparator implements Comparator<String> {

        private final Map<String, SmithyIntegration<?, ?, ?>> lookup;

        IntegrationComparator(Map<String, SmithyIntegration<?, ?, ?>> lookup) {
            this.lookup = lookup;
        }

        @Override
        public int compare(String o1, String o2) {
            SmithyIntegration<?, ?, ?> left = lookup.get(o1);
            SmithyIntegration<?, ?, ?> right = lookup.get(o2);
            if (left == null || right == null) {
                return 0;
            }
            return Byte.compare(left.priority(), right.priority());
        }
    }

    private static class TestImportContainer implements ImportContainer {
        @Override
        public void importSymbol(Symbol symbol, String alias) {

        }
    }

    private static class TestSymbolWriter extends SymbolWriter<TestSymbolWriter, TestImportContainer> {
        public TestSymbolWriter(TestImportContainer importContainer) {
            super(importContainer);
        }
    }

    private static class TestSettings {}

    private static class TestIntegration implements SmithyIntegration<
            TestSettings,
            TestSymbolWriter,
            CodegenContext<TestSettings, TestSymbolWriter, TestIntegration>> {

        private final String name;
        private final byte priority;
        private final List<String> runBefore;
        private final List<String> runAfter;

        TestIntegration(String name) {
            this(name, (byte) 0);
        }

        TestIntegration(String name, byte priority) {
            this(name, priority, Collections.emptyList());
        }

        TestIntegration(String name, byte priority, List<String> runBefore) {
            this(name, priority, runBefore, Collections.emptyList());
        }

        TestIntegration(String name, byte priority, List<String> runBefore, List<String> runAfter) {
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
}
