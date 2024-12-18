/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.transform;

import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Optional;
import java.util.function.Function;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ModelSerializer;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.InputTrait;
import software.amazon.smithy.model.traits.OutputTrait;
import software.amazon.smithy.model.traits.synthetic.OriginalShapeIdTrait;

public class CreateDedicatedInputAndOutputTest {
    private Model compareTransform(String prefix, Function<Model, Model> transform) {
        Model before = Model.assembler()
                .addImport(getClass().getResource("dedicated-input-output/" + prefix + "-before.smithy"))
                .assemble()
                .unwrap();
        Model actualResult = transform.apply(before);
        Model expectedResult = Model.assembler()
                .addImport(getClass().getResource("dedicated-input-output/" + prefix + "-after.smithy"))
                .assemble()
                .unwrap();

        Node actualNode = ModelSerializer.builder().build().serialize(actualResult);
        Node expectedNode = ModelSerializer.builder().build().serialize(expectedResult);

        Node.assertEquals(actualNode, expectedNode);

        return actualResult;
    }

    @Test
    public void createsDedicatedInputHeuristic() {
        Model result = compareTransform("creates-dedicated-input-heuristic", model -> {
            return ModelTransformer.create().createDedicatedInputAndOutput(model, "Input", "Output");
        });

        result.expectShape(ShapeId.from("smithy.example#GetFooInput")).expectTrait(InputTrait.class);
        result.expectShape(ShapeId.from("smithy.example#GetFooOutput")).expectTrait(OutputTrait.class);
        assertThat(result.expectShape(ShapeId.from("smithy.example#GetFooInput")).getTrait(OriginalShapeIdTrait.class),
                Matchers.equalTo(Optional.empty()));
        assertThat(result.expectShape(ShapeId.from("smithy.example#GetFooOutput")).getTrait(OriginalShapeIdTrait.class),
                Matchers.equalTo(Optional.empty()));
    }

    @Test
    public void createsDedicatedOutputHeuristic() {
        Model result = compareTransform("creates-dedicated-output-heuristic", model -> {
            return ModelTransformer.create().createDedicatedInputAndOutput(model, "Input", "Output");
        });

        result.expectShape(ShapeId.from("smithy.example#GetFooInput")).expectTrait(InputTrait.class);
        result.expectShape(ShapeId.from("smithy.example#GetFooOutput")).expectTrait(OutputTrait.class);
        assertThat(result.expectShape(ShapeId.from("smithy.example#GetFooInput")).getTrait(OriginalShapeIdTrait.class),
                Matchers.equalTo(Optional.empty()));
        assertThat(result.expectShape(ShapeId.from("smithy.example#GetFooOutput")).getTrait(OriginalShapeIdTrait.class),
                Matchers.equalTo(Optional.empty()));
    }

    @Test
    public void createsDedicatedInputAndOutputHeuristic() {
        Model result = compareTransform("creates-dedicated-input-output-heuristic", model -> {
            return ModelTransformer.create().createDedicatedInputAndOutput(model, "Request", "Response");
        });

        result.expectShape(ShapeId.from("smithy.example#GetFooRequest")).expectTrait(InputTrait.class);
        result.expectShape(ShapeId.from("smithy.example#GetFooResponse")).expectTrait(OutputTrait.class);
        assertThat(
                result.expectShape(ShapeId.from("smithy.example#GetFooRequest")).getTrait(OriginalShapeIdTrait.class),
                Matchers.equalTo(Optional.empty()));
        assertThat(
                result.expectShape(ShapeId.from("smithy.example#GetFooResponse")).getTrait(OriginalShapeIdTrait.class),
                Matchers.equalTo(Optional.empty()));
    }

    @Test
    public void createsDedicatedCopies() {
        Model result = compareTransform("creates-dedicated-copies", model -> {
            return ModelTransformer.create().createDedicatedInputAndOutput(model, "Input", "Output");
        });

        result.expectShape(ShapeId.from("smithy.example#GetFooInput")).expectTrait(InputTrait.class);
        result.expectShape(ShapeId.from("smithy.example#GetFooOutput")).expectTrait(OutputTrait.class);
        assertThat(result.expectShape(ShapeId.from("smithy.example#GetFooInput"))
                .expectTrait(OriginalShapeIdTrait.class)
                .getOriginalId(),
                Matchers.equalTo(ShapeId.from("smithy.example#GetFooData")));
        assertThat(result.expectShape(ShapeId.from("smithy.example#GetFooOutput"))
                .expectTrait(OriginalShapeIdTrait.class)
                .getOriginalId(),
                Matchers.equalTo(ShapeId.from("smithy.example#Foo")));
    }

    @Test
    public void createsDedicatedCopiesAndDeconflicts() {
        Model result = compareTransform("creates-dedicated-copies-and-deconflicts", model -> {
            return ModelTransformer.create().createDedicatedInputAndOutput(model, "Input", "Output");
        });

        result.expectShape(ShapeId.from("smithy.example#GetFooOperationInput")).expectTrait(InputTrait.class);
        result.expectShape(ShapeId.from("smithy.example#GetFooOperationOutput")).expectTrait(OutputTrait.class);
        assertThat(result.expectShape(ShapeId.from("smithy.example#GetFooOperationInput"))
                .expectTrait(OriginalShapeIdTrait.class)
                .getOriginalId(),
                Matchers.equalTo(ShapeId.from("smithy.example#GetFooData")));
        assertThat(result.expectShape(ShapeId.from("smithy.example#GetFooOperationOutput"))
                .expectTrait(OriginalShapeIdTrait.class)
                .getOriginalId(),
                Matchers.equalTo(ShapeId.from("smithy.example#Foo")));
    }

    @Test
    public void removesDisconnectedShapes() {
        Model result = compareTransform("removes-disconnected-shapes", model -> {
            return ModelTransformer.create().createDedicatedInputAndOutput(model, "Input", "Output");
        });

        assertThat(result.getShapeIds(), Matchers.not(Matchers.hasItem(ShapeId.from("smithy.example#GetFooData"))));
        assertThat(result.getShapeIds(), Matchers.not(Matchers.hasItem(ShapeId.from("smithy.example#Foo"))));

        result.expectShape(ShapeId.from("smithy.example#GetFooInput")).expectTrait(InputTrait.class);
        result.expectShape(ShapeId.from("smithy.example#GetFooOutput")).expectTrait(OutputTrait.class);

        assertThat(result.expectShape(ShapeId.from("smithy.example#GetFooInput"))
                .expectTrait(OriginalShapeIdTrait.class)
                .getOriginalId(),
                Matchers.equalTo(ShapeId.from("smithy.example#GetFooData")));
        assertThat(result.expectShape(ShapeId.from("smithy.example#GetFooOutput"))
                .expectTrait(OriginalShapeIdTrait.class)
                .getOriginalId(),
                Matchers.equalTo(ShapeId.from("smithy.example#Foo")));
    }

    @Test
    public void handlesUnitTypes() {
        Model result = compareTransform("handles-unit-types", model -> {
            return ModelTransformer.create().createDedicatedInputAndOutput(model, "Input", "Output");
        });

        result.expectShape(ShapeId.from("smithy.example#GetFooInput")).expectTrait(InputTrait.class);
        result.expectShape(ShapeId.from("smithy.example#GetFooOutput")).expectTrait(OutputTrait.class);

        assertThat(result.expectShape(ShapeId.from("smithy.example#GetFooInput"))
                .expectTrait(OriginalShapeIdTrait.class)
                .getOriginalId(),
                Matchers.equalTo(ShapeId.from("smithy.api#Unit")));
        assertThat(result.expectShape(ShapeId.from("smithy.example#GetFooOutput"))
                .expectTrait(OriginalShapeIdTrait.class)
                .getOriginalId(),
                Matchers.equalTo(ShapeId.from("smithy.api#Unit")));
    }

    @Test
    public void removesDisconnectedSharedShape() {
        Model result = compareTransform("removes-disconnected-shared-shape",
                model -> ModelTransformer.create().createDedicatedInputAndOutput(model, "Input", "Output"));

        assertThat(result.getShapeIds(), Matchers.not(Matchers.hasItem(ShapeId.from("smithy.example#MyGetFooOutput"))));

        result.expectShape(ShapeId.from("smithy.example#GetFooInput")).expectTrait(InputTrait.class);
        result.expectShape(ShapeId.from("smithy.example#GetFooOutput")).expectTrait(OutputTrait.class);

        assertThat(result.expectShape(ShapeId.from("smithy.example#GetFooInput"))
                .expectTrait(OriginalShapeIdTrait.class)
                .getOriginalId(),
                Matchers.equalTo(ShapeId.from("smithy.example#MyGetFooOutput")));

        assertThat(result.expectShape(ShapeId.from("smithy.example#GetFooOutput"))
                .expectTrait(OriginalShapeIdTrait.class)
                .getOriginalId(),
                Matchers.equalTo(ShapeId.from("smithy.example#MyGetFooOutput")));
    }

    @Test
    public void createsDedicatedHeuristicForSharedShape() {
        Model result = compareTransform("creates-dedicated-heuristic-for-shared",
                model -> ModelTransformer.create().createDedicatedInputAndOutput(model, "Input", "Output"));

        result.expectShape(ShapeId.from("smithy.example#GetFooInput")).expectTrait(InputTrait.class);
        result.expectShape(ShapeId.from("smithy.example#GetFooOutput")).expectTrait(OutputTrait.class);

        assertThat(result.expectShape(ShapeId.from("smithy.example#GetFooInput"))
                .expectTrait(OriginalShapeIdTrait.class)
                .getOriginalId(),
                Matchers.equalTo(ShapeId.from("smithy.example#GetFooOutput")));

        assertThat(result.expectShape(ShapeId.from("smithy.example#GetFooOutput"))
                .getTrait(OriginalShapeIdTrait.class), Matchers.equalTo(Optional.empty()));
    }

    @Test
    public void detectsConflictResolverLoop() {
        Assertions.assertThrows(ModelTransformException.class, () -> {
            Model before = Model.assembler()
                    .addImport(getClass().getResource("dedicated-input-output/unable-to-deconflict.smithy"))
                    .assemble()
                    .unwrap();
            ModelTransformer.create().createDedicatedInputAndOutput(before, "Input", "Output");
        });
    }
}
