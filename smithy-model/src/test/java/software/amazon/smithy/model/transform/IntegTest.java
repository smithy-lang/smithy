/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.transform;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Optional;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;

public class IntegTest {

    private static Model model;

    @BeforeAll
    public static void before() {
        model = Model.assembler()
                .addImport(IntegTest.class.getResource("integration-test-model.json"))
                .assemble()
                .unwrap();
    }

    @Test
    public void removesResources() {
        ModelTransformer transformer = ModelTransformer.create();
        Model result = transformer.removeShapesIf(model, shape -> shape.getId().toString().equals("ns.foo#MyResource"));

        assertValidModel(result);
        assertThat(result.getShape(ShapeId.from("ns.foo#MyResource")), Matchers.is(Optional.empty()));
        // The operations bound to the resource remain, now orphaned.
        assertThat(result.getShape(ShapeId.from("ns.foo#CreateMyResource")), Matchers.not(Optional.empty()));
    }

    @Test
    public void removesServices() {
        ModelTransformer transformer = ModelTransformer.create();
        Model result = transformer.removeShapesIf(model, shape -> shape.getId().toString().equals("ns.foo#MyService"));

        assertValidModel(result);
        // Operations and resources bound to the service remain.
        assertThat(result.getShape(ShapeId.from("ns.foo#MyResource")), Matchers.not(Optional.empty()));
        assertThat(result.getShape(ShapeId.from("ns.foo#MyResourceIdentifier")), Matchers.not(Optional.empty()));
        assertThat(result.getShape(ShapeId.from("ns.foo#CreateMyResource")), Matchers.not(Optional.empty()));
        assertThat(result.getShape(ShapeId.from("ns.foo#MyOperation")), Matchers.not(Optional.empty()));
    }

    @Test
    public void removesUnreferencedShapes() {
        ModelTransformer transformer = ModelTransformer.create();
        Model result = transformer.removeShapesIf(model, shape -> shape.getId().toString().equals("ns.foo#MyResource"));
        result = transformer.removeUnreferencedShapes(result);

        assertValidModel(result);
        assertThat(result.getShape(ShapeId.from("ns.foo#MyResource")), Matchers.is(Optional.empty()));
        assertThat(result.getShape(ShapeId.from("ns.foo#MyResourceIdentifier")), Matchers.not(Optional.empty()));
        assertThat(result.getShape(ShapeId.from("ns.foo#CreateMyResource")), Matchers.is(Optional.empty()));
        assertThat(result.getShape(ShapeId.from("ns.foo#CreateMyResourceOutput")), Matchers.is(Optional.empty()));
        assertThat(result.getShape(ShapeId.from("ns.foo#MyResourceOperationInput")), Matchers.is(Optional.empty()));
        assertThat(result.getShape(ShapeId.from("ns.foo#MyResourceOperationInputString")),
                Matchers.is(Optional.empty()));
    }

    @Test
    public void removesUnreferencedShapesWithFilter() {
        ModelTransformer transformer = ModelTransformer.create();
        Model result = transformer.removeShapesIf(model, shape -> shape.getId().toString().equals("ns.foo#MyResource"));
        result = transformer.removeUnreferencedShapes(result, shape -> !shape.getTags().contains("foo"));

        assertValidModel(result);
        assertThat(result.getShape(ShapeId.from("ns.foo#MyResource")), Matchers.is(Optional.empty()));
        assertThat(result.getShape(ShapeId.from("ns.foo#MyResourceIdentifier")), Matchers.not(Optional.empty()));
        assertThat(result.getShape(ShapeId.from("ns.foo#CreateMyResource")), Matchers.is(Optional.empty()));
        assertThat(result.getShape(ShapeId.from("ns.foo#CreateMyResourceOutput")), Matchers.is(Optional.empty()));
        assertThat(result.getShape(ShapeId.from("ns.foo#MyResourceOperationInput")), Matchers.is(Optional.empty()));
        assertThat(result.getShape(ShapeId.from("ns.foo#MyResourceOperationInputString")),
                Matchers.not(Optional.empty()));
    }

    private void assertValidModel(Model model) {
        assertFalse(Model.assembler().addModel(model).assemble().isBroken());
    }
}
