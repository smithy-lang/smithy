/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.jmh;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.selector.Selector;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.HttpTrait;

@Warmup(iterations = 3)
@Measurement(iterations = 3, timeUnit = TimeUnit.MICROSECONDS)
@BenchmarkMode(Mode.AverageTime)
@Fork(1)
public class Selectors {

    @State(Scope.Thread)
    public static class SelectorState {

        public Model model;
        public Selector suboptimalHttpBindingSelector = createSuboptimalHttpBindingIncompatibilitySelector();
        public Selector httpBindingSelector = createHttpBindingIncompatibilitySelector();
        public String testIdlModelLocation = "test-model.smithy";
        public String testJsonModelLocation = "test-model.json";

        @Setup
        public void prepare() {
            model = Model.assembler()
                    .addImport(Selectors.class.getResource("http-model.smithy"))
                    .assemble()
                    .getResult()
                    .get();
        }

        private Selector createSuboptimalHttpBindingIncompatibilitySelector() {
            return Selector.parse("$service(service) ${service}\n"
                    + "$operations(~> operation)\n"
                    + ":test(${operations}[trait|http])\n"
                    + "${operations}\n"
                    + ":not([trait|http])");
        }

        private Selector createHttpBindingIncompatibilitySelector() {
            return Selector.parse("service\n"
                    + "$operations(~> operation)\n"
                    + ":test(${operations}[trait|http])\n"
                    + "${operations}\n"
                    + ":not([trait|http])");
        }
    }

    @Benchmark
    public Model loadsIdlModelWithoutValidation(SelectorState state) {
        return Model.assembler()
                .addImport(Selectors.class.getResource(state.testIdlModelLocation))
                .disableValidation()
                .assemble()
                .unwrap();
    }

    @Benchmark
    public Model loadsIdlModelWithValidation(SelectorState state) {
        return Model.assembler()
                .addImport(Selectors.class.getResource(state.testIdlModelLocation))
                .assemble()
                .unwrap();
    }

    @Benchmark
    public Model loadsJsonModelWithoutValidation(SelectorState state) {
        return Model.assembler()
                .addImport(Selectors.class.getResource(state.testJsonModelLocation))
                .disableValidation()
                .assemble()
                .unwrap();
    }

    // Benchmarks just parsing the selector.
    @Benchmark
    public Selector parseHttpBindingIncompatibilitySelector(SelectorState state) {
        return state.createHttpBindingIncompatibilitySelector();
    }

    // The selector based version of evaluateHttpBindingManually.
    @Benchmark
    public Set<Shape> evaluateHttpBindingSelector(SelectorState state) {
        return state.httpBindingSelector.select(state.model);
    }

    // The selector based version of evaluateHttpBindingManually. It does not take
    // advantage of the Model.shapes() optimization because the first selector is
    // not an instance of ShapeTypeSelector.
    @Benchmark
    public Set<Shape> evaluateSuboptimalHttpBindingSelector(SelectorState state) {
        return state.suboptimalHttpBindingSelector.select(state.model);
    }

    // The is the hand-written alternative to evaluateHttpBindingSelector to provide
    // a baseline.
    @Benchmark
    public Set<Shape> evaluateHttpBindingManually(SelectorState state) {
        Model model = state.model;
        TopDownIndex topDownIndex = TopDownIndex.of(model);
        return model.shapes(ServiceShape.class).flatMap(service -> {
            Set<OperationShape> operations = topDownIndex.getContainedOperations(service);
            // Stop early if there are no bindings at all in the model for any operation.
            if (operations.stream().noneMatch(o -> o.hasTrait(HttpTrait.class))) {
                return Stream.empty();
            }
            return operations.stream().filter(shape -> !shape.hasTrait(HttpTrait.class));
        })
                .collect(Collectors.toSet());
    }
}
