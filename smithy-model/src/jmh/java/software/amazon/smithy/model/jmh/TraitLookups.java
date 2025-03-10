/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.jmh;

import java.util.ArrayList;
import java.util.List;
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
import software.amazon.smithy.model.loader.ModelAssembler;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.ErrorTrait;
import software.amazon.smithy.model.traits.HttpErrorTrait;
import software.amazon.smithy.model.traits.InputTrait;
import software.amazon.smithy.model.traits.OutputTrait;
import software.amazon.smithy.utils.ListUtils;

@Warmup(iterations = 3)
@Measurement(iterations = 3, timeUnit = TimeUnit.MICROSECONDS)
@BenchmarkMode(Mode.AverageTime)
@Fork(1)
public class TraitLookups {
    @State(Scope.Thread)
    public static class TraitLookupState {
        public List<ShapeId> shapeIds = new ArrayList<>();
        public Model model;

        @Setup
        public void prepare() {
            ModelAssembler assembler = new ModelAssembler();

            for (String prefix : ListUtils.of("one", "two", "three", "four")) {
                for (int i = 0; i < 100; i++) {
                    ShapeId id = ShapeId.fromParts("ns.foo", prefix + i);
                    shapeIds.add(id);
                    StructureShape.Builder builder = StructureShape.builder()
                            .id(id);
                    switch (prefix) {
                        case "four":
                            builder.addTrait(new InputTrait());
                        case "three":
                            builder.addTrait(new OutputTrait());
                        case "two":
                            builder.addTrait(new ErrorTrait("client"));
                        case "one":
                            builder.addTrait(new HttpErrorTrait(400));
                    }
                    assembler.addShape(builder.build());
                }
            }

            model = assembler
                    .disableValidation()
                    .assemble()
                    .getResult()
                    .get();
        }
    }

    @Benchmark
    public void hasTraitByShapeId(TraitLookupState state) {
        for (ShapeId shapeId : state.shapeIds) {
            state.model.expectShape(shapeId).hasTrait(InputTrait.ID);
            state.model.expectShape(shapeId).hasTrait(OutputTrait.ID);
            state.model.expectShape(shapeId).hasTrait(ErrorTrait.ID);
            state.model.expectShape(shapeId).hasTrait(HttpErrorTrait.ID);
        }
    }

    @Benchmark
    public void hasTraitByShapeIdOld(TraitLookupState state) {
        for (ShapeId shapeId : state.shapeIds) {
            state.model.expectShape(shapeId).findTrait(InputTrait.ID).isPresent();
            state.model.expectShape(shapeId).findTrait(OutputTrait.ID).isPresent();
            state.model.expectShape(shapeId).findTrait(ErrorTrait.ID).isPresent();
            state.model.expectShape(shapeId).findTrait(HttpErrorTrait.ID).isPresent();
        }
    }

    @Benchmark
    public void hasTraitByString(TraitLookupState state) {
        for (ShapeId shapeId : state.shapeIds) {
            state.model.expectShape(shapeId).hasTrait(InputTrait.ID.toString());
            state.model.expectShape(shapeId).hasTrait(OutputTrait.ID.toString());
            state.model.expectShape(shapeId).hasTrait(ErrorTrait.ID.toString());
            state.model.expectShape(shapeId).hasTrait(HttpErrorTrait.ID.toString());
        }
    }

    @Benchmark
    public void hasTraitByClass(TraitLookupState state) {
        for (ShapeId shapeId : state.shapeIds) {
            state.model.expectShape(shapeId).hasTrait(InputTrait.class);
            state.model.expectShape(shapeId).hasTrait(OutputTrait.class);
            state.model.expectShape(shapeId).hasTrait(ErrorTrait.class);
            state.model.expectShape(shapeId).hasTrait(HttpErrorTrait.class);
        }
    }
}
