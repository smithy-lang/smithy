/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.model.jmh;

import java.util.Set;
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
import software.amazon.smithy.model.selector.Selector;
import software.amazon.smithy.model.shapes.Shape;

@Warmup(iterations = 3)
@Measurement(iterations = 3, timeUnit = TimeUnit.MICROSECONDS)
@BenchmarkMode(Mode.AverageTime)
@Fork(1)
public class Selectors {

    @State(Scope.Thread)
    public static class SelectorState {

        public Model model;
        public Selector authSelector = createAuthIncompatibilitySelector();

        @Setup
        public void prepare() {
            model = Model.assembler()
                    .addImport(Selectors.class.getResource("auth-model.smithy"))
                    .assemble()
                    .getResult()
                    .get();
        }

        private Selector createAuthIncompatibilitySelector() {
            return Selector.parse("service\n"
                                  + "$service(*)\n"
                                  + "$operations(~> operation)\n"
                                  + "$httpOperations(${operations}[trait|http])\n"
                                  + "${operations}\n"
                                  + ":not([trait|http])");
        }
    }

    @Benchmark
    public Set<Shape> evaluateAuthSelector(SelectorState state) {
        return state.authSelector.select(state.model);
    }

    @Benchmark
    public Selector selectorParsing(SelectorState state) {
        return state.createAuthIncompatibilitySelector();
    }
}
