/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.model.transform;

import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Collections;
import java.util.Optional;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.EnumTrait;
import software.amazon.smithy.model.traits.ReadonlyTrait;

public class ModelTransformerTest {

    @Test
    public void discoversOnRemoveClassesWithSpi() {
        ModelTransformer transformer = ModelTransformer.create();
        Model model = createTestModel();
        Model result = transformer.removeShapesIf(model, Shape::isStructureShape);
        ShapeId operation = ShapeId.from("ns.foo#MyOperation");

        assertThat(result.expectShape(operation), Matchers.not(Optional.empty()));
        assertThat(result.expectShape(operation).asOperationShape().flatMap(OperationShape::getInput),
                          Matchers.is(Optional.empty()));
        assertThat(result.expectShape(operation).asOperationShape().flatMap(OperationShape::getOutput),
                          Matchers.is(Optional.empty()));
        assertThat(result.expectShape(operation).asOperationShape().map(OperationShape::getErrors),
                          Matchers.equalTo(Optional.of(Collections.emptyList())));
    }

    @Test
    public void removesTraitShapesButNotTraitUsage() {
        ModelTransformer transformer = ModelTransformer.create();
        Model model = createTestModel();
        Model nonTraitShapes = transformer.getModelWithoutTraitShapes(model);
        ShapeId operation = ShapeId.from("ns.foo#MyOperation");

        assertThat(nonTraitShapes.getShape(operation), Matchers.not(Optional.empty()));
        assertThat(nonTraitShapes.expectShape(operation).getTrait(ReadonlyTrait.class), Matchers.not(Optional.empty()));
        assertThat(nonTraitShapes.getShape(EnumTrait.ID), Matchers.equalTo(Optional.empty()));
    }

    private Model createTestModel() {
        return Model.assembler()
                .addImport(ModelTransformerTest.class.getResource("test-model.json"))
                .assemble()
                .unwrap();
    }
}
