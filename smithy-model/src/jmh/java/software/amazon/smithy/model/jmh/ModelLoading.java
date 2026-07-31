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
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import software.amazon.smithy.model.Model;

@Warmup(iterations = 5)
@Measurement(iterations = 5, timeUnit = TimeUnit.MICROSECONDS)
@BenchmarkMode(Mode.AverageTime)
@Fork(1)
public class ModelLoading {

    @State(Scope.Thread)
    public static class LoaderState {
        public String smallJsonModel = "test-model.json";
    }

    @Benchmark
    public Model loadsSmallJsonModelWithoutValidation(LoaderState state) {
        return Model.assembler()
                .addImport(ModelLoading.class.getResource(state.smallJsonModel))
                .disableValidation()
                .assemble()
                .unwrap();
    }
}
