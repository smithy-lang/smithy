/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.jmh;

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
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.BottomUpIndex;

@Warmup(iterations = 3)
@Measurement(iterations = 3, timeUnit = TimeUnit.MICROSECONDS)
@BenchmarkMode(Mode.AverageTime)
@Fork(1)
public class KnowledgeIndicies {
    @State(Scope.Thread)
    public static class IndiciesState {
        public Model model;

        @Setup
        public void prepare() {
            model = Model.assembler()
                    .addImport(KnowledgeIndicies.class.getResource("test-model.smithy"))
                    .disableValidation()
                    .assemble()
                    .getResult()
                    .get();
        }
    }

    @Benchmark
    public BottomUpIndex createsBottomUpIndex(IndiciesState state) {
        // Use the ctor so the index doesn't get cached in warmup runs and re-used
        return new BottomUpIndex(state.model);
    }
}
