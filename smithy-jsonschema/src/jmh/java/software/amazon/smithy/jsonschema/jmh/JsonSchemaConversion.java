/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jsonschema.jmh;

import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import software.amazon.smithy.jsonschema.JsonSchemaConfig;
import software.amazon.smithy.jsonschema.JsonSchemaConverter;
import software.amazon.smithy.jsonschema.SchemaDocument;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.ModelAssembler;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.ErrorTrait;
import software.amazon.smithy.model.traits.InputTrait;
import software.amazon.smithy.model.traits.MixinTrait;
import software.amazon.smithy.model.traits.OutputTrait;
import software.amazon.smithy.model.traits.RequiredTrait;
import software.amazon.smithy.model.traits.TraitDefinition;

@Warmup(iterations = 5)
@Measurement(iterations = 5, timeUnit = TimeUnit.MICROSECONDS)
@BenchmarkMode(Mode.AverageTime)
@Fork(1)
public class JsonSchemaConversion {

    @State(Scope.Thread)
    public static class ConversionState {

        @Param({"5", "50", "200"})
        public int operationCount;

        @Param({"1", "5", "50"})
        public int serviceCount;

        public Model model;
        public JsonSchemaConfig config;

        @Setup
        public void prepare() {
            model = buildModel(operationCount, serviceCount);
            config = new JsonSchemaConfig();
            config.setService(ShapeId.from("smithy.benchmark.s0#BenchmarkService0"));
        }

        private static Model buildModel(int numOperations, int numServices) {
            ModelAssembler assembler = new ModelAssembler();

            // Mixin structure that all inputs/outputs will use
            StructureShape commonMixin = StructureShape.builder()
                    .id("smithy.benchmark#CommonFields")
                    .addTrait(MixinTrait.builder().build())
                    .addMember("requestId", ShapeId.from("smithy.api#String"))
                    .addMember("timestamp", ShapeId.from("smithy.api#String"))
                    .build();
            assembler.addShape(commonMixin);

            // Trait definition (gets scrubbed during conversion)
            StructureShape traitDef = StructureShape.builder()
                    .id("smithy.benchmark#customTag")
                    .addTrait(TraitDefinition.builder().build())
                    .addMember("value", ShapeId.from("smithy.api#String"))
                    .build();
            assembler.addShape(traitDef);

            for (int s = 0; s < numServices; s++) {
                String ns = "smithy.benchmark.s" + s;
                ServiceShape.Builder serviceBuilder = ServiceShape.builder()
                        .id(ns + "#BenchmarkService" + s)
                        .version("2024-01-01");

                for (int i = 0; i < numOperations; i++) {
                    String opName = "Operation" + i;
                    ShapeId inputId = ShapeId.fromParts(ns, opName + "Input");
                    ShapeId outputId = ShapeId.fromParts(ns, opName + "Output");
                    ShapeId nestedId = ShapeId.fromParts(ns, opName + "Nested");
                    ShapeId itemListId = ShapeId.fromParts(ns, opName + "ItemList");
                    ShapeId itemId = ShapeId.fromParts(ns, opName + "Item");
                    ShapeId errorId = ShapeId.fromParts(ns, opName + "Error");
                    ShapeId opId = ShapeId.fromParts(ns, opName);

                    // Nested structure
                    StructureShape nested = StructureShape.builder()
                            .id(nestedId)
                            .addMember("value0", ShapeId.from("smithy.api#Integer"))
                            .addMember("value1", ShapeId.from("smithy.api#Integer"))
                            .addMember("value2", ShapeId.from("smithy.api#Integer"))
                            .build();
                    assembler.addShape(nested);

                    // Input structure with mixin
                    StructureShape input = StructureShape.builder()
                            .id(inputId)
                            .addTrait(new InputTrait())
                            .addMixin(commonMixin)
                            .addMember("id",
                                    ShapeId.from("smithy.api#String"),
                                    b -> b.addTrait(new RequiredTrait()))
                            .addMember("field0", ShapeId.from("smithy.api#String"))
                            .addMember("field1", ShapeId.from("smithy.api#String"))
                            .addMember("field2", ShapeId.from("smithy.api#String"))
                            .addMember("nested", nestedId)
                            .build();
                    assembler.addShape(input);

                    // Item structure for list
                    StructureShape item = StructureShape.builder()
                            .id(itemId)
                            .addMember("name", ShapeId.from("smithy.api#String"))
                            .addMember("value", ShapeId.from("smithy.api#String"))
                            .build();
                    assembler.addShape(item);

                    // List shape
                    ListShape itemList = ListShape.builder()
                            .id(itemListId)
                            .member(MemberShape.builder()
                                    .id(itemListId.withMember("member"))
                                    .target(itemId)
                                    .build())
                            .build();
                    assembler.addShape(itemList);

                    // Output structure with mixin
                    StructureShape output = StructureShape.builder()
                            .id(outputId)
                            .addTrait(new OutputTrait())
                            .addMixin(commonMixin)
                            .addMember("result", ShapeId.from("smithy.api#String"))
                            .addMember("items", itemListId)
                            .build();
                    assembler.addShape(output);

                    // Error structure
                    StructureShape error = StructureShape.builder()
                            .id(errorId)
                            .addTrait(new ErrorTrait("client"))
                            .addMember("message",
                                    ShapeId.from("smithy.api#String"),
                                    b -> b.addTrait(new RequiredTrait()))
                            .build();
                    assembler.addShape(error);

                    // Operation
                    OperationShape operation = OperationShape.builder()
                            .id(opId)
                            .input(inputId)
                            .output(outputId)
                            .addError(errorId)
                            .build();
                    assembler.addShape(operation);

                    serviceBuilder.addOperation(opId);
                }

                assembler.addShape(serviceBuilder.build());
            }

            return assembler.disableValidation().assemble().unwrap();
        }
    }

    @Benchmark
    public SchemaDocument convertFullService(ConversionState state) {
        JsonSchemaConverter converter = JsonSchemaConverter.builder()
                .model(state.model)
                .config(state.config)
                .rootShape(ShapeId.from("smithy.benchmark.s0#BenchmarkService0"))
                .build();
        return converter.convert();
    }
}
