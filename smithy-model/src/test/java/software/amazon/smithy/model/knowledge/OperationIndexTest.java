/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.knowledge;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.UnitTypeTrait;
import software.amazon.smithy.utils.SetUtils;

public class OperationIndexTest {

    private static Model model;

    @BeforeAll
    public static void before() {
        model = Model.assembler()
                .addImport(OperationIndexTest.class.getResource("operation-index-test.smithy"))
                .assemble()
                .unwrap();
    }

    @AfterAll
    public static void after() {
        model = null;
    }

    @Test
    public void indexesUnitOperations() {
        OperationIndex opIndex = OperationIndex.of(model);

        assertThat(opIndex.getInput(ShapeId.from("ns.foo#A")), is(Optional.empty()));
        assertThat(opIndex.getInputShape(ShapeId.from("ns.foo#A")).map(Shape::getId),
                equalTo(Optional.of(UnitTypeTrait.UNIT)));
        assertThat(opIndex.expectInputShape(ShapeId.from("ns.foo#A")).getId(), equalTo(UnitTypeTrait.UNIT));
        assertThat(opIndex.getOutput(ShapeId.from("ns.foo#A")), is(Optional.empty()));
        assertThat(opIndex.getOutputShape(ShapeId.from("ns.foo#A")).map(Shape::getId),
                equalTo(Optional.of(UnitTypeTrait.UNIT)));
        assertThat(opIndex.expectOutputShape(ShapeId.from("ns.foo#A")).getId(), equalTo(UnitTypeTrait.UNIT));
        assertThat(opIndex.getErrors(ShapeId.from("ns.foo#A")), empty());
    }

    @Test
    public void indexesOperations() {
        OperationIndex opIndex = OperationIndex.of(model);
        Shape input = model.getShape(ShapeId.from("ns.foo#Input")).get();
        Shape output = model.getShape(ShapeId.from("ns.foo#Output")).get();

        assertThat(opIndex.getInput(ShapeId.from("ns.foo#B")), is(Optional.of(input)));
        assertThat(opIndex.getOutput(ShapeId.from("ns.foo#B")), is(Optional.of(output)));
    }

    @Test
    public void returnsAllInputMembers() {
        OperationIndex opIndex = OperationIndex.of(model);
        StructureShape input = model.expectShape(ShapeId.from("ns.foo#Input"), StructureShape.class);

        assertThat(opIndex.getInputMembers(ShapeId.from("ns.foo#B")), equalTo(input.getAllMembers()));
        assertThat(opIndex.getInputMembers(ShapeId.from("ns.foo#Missing")), equalTo(Collections.emptyMap()));
    }

    @Test
    public void returnsAllOutputMembers() {
        OperationIndex opIndex = OperationIndex.of(model);
        StructureShape output = model.expectShape(ShapeId.from("ns.foo#Output"), StructureShape.class);

        assertThat(opIndex.getOutputMembers(ShapeId.from("ns.foo#B")), equalTo(output.getAllMembers()));
        assertThat(opIndex.getOutputMembers(ShapeId.from("ns.foo#Missing")), equalTo(Collections.emptyMap()));
    }

    @Test
    public void determinesIfShapeIsUsedAsInput() {
        OperationIndex opIndex = OperationIndex.of(model);
        StructureShape input = model.expectShape(ShapeId.from("ns.foo#Input"), StructureShape.class);
        StructureShape output = model.expectShape(ShapeId.from("ns.foo#Output"), StructureShape.class);

        assertThat(opIndex.isInputStructure(input), is(true));
        assertThat(opIndex.isInputStructure(output), is(false));
    }

    @Test
    public void getsInputBindings() {
        OperationIndex index = OperationIndex.of(model);
        Set<OperationShape> actual = index.getInputBindings(ShapeId.from("ns.foo#Input"));
        Set<OperationShape> expected = SetUtils.of(
                model.expectShape(ShapeId.from("ns.foo#B"), OperationShape.class),
                model.expectShape(ShapeId.from("ns.foo#C"), OperationShape.class));
        assertThat(actual, equalTo(expected));
        assertThat(index.getInputBindings(ShapeId.from("ns.foo#Output")), empty());
    }

    @Test
    public void determinesIfShapeIsUsedAsOutput() {
        OperationIndex opIndex = OperationIndex.of(model);
        StructureShape input = model.expectShape(ShapeId.from("ns.foo#Input"), StructureShape.class);
        StructureShape output = model.expectShape(ShapeId.from("ns.foo#Output"), StructureShape.class);

        assertThat(opIndex.isOutputStructure(output), is(true));
        assertThat(opIndex.isOutputStructure(input), is(false));
    }

    @Test
    public void getsOutputBindings() {
        OperationIndex index = OperationIndex.of(model);
        Set<OperationShape> actual = index.getOutputBindings(ShapeId.from("ns.foo#Output"));
        Set<OperationShape> expected = SetUtils.of(
                model.expectShape(ShapeId.from("ns.foo#B"), OperationShape.class),
                model.expectShape(ShapeId.from("ns.foo#C"), OperationShape.class));
        assertThat(actual, equalTo(expected));
        assertThat(index.getOutputBindings(ShapeId.from("ns.foo#Input")), empty());
    }

    @Test
    public void getsOperationErrorsAndInheritedErrors() {
        OperationIndex opIndex = OperationIndex.of(model);
        ShapeId a = ShapeId.from("ns.foo#A");
        ShapeId b = ShapeId.from("ns.foo#B");
        ServiceShape service = model.expectShape(ShapeId.from("ns.foo#MyService"), ServiceShape.class);
        StructureShape error1 = model.expectShape(ShapeId.from("ns.foo#Error1"), StructureShape.class);
        StructureShape error2 = model.expectShape(ShapeId.from("ns.foo#Error2"), StructureShape.class);
        StructureShape common1 = model.expectShape(ShapeId.from("ns.foo#CommonError1"), StructureShape.class);
        StructureShape common2 = model.expectShape(ShapeId.from("ns.foo#CommonError2"), StructureShape.class);

        assertThat(opIndex.getErrors(service), containsInAnyOrder(common1, common2));
        assertThat(opIndex.getErrors(a), empty());
        assertThat(opIndex.getErrors(service, a), containsInAnyOrder(common1, common2));
        assertThat(opIndex.getErrors(b), containsInAnyOrder(error1, error2));
        assertThat(opIndex.getErrors(service, b), containsInAnyOrder(error1, error2, common1, common2));
    }

    @Test
    public void getsErrorBindings() {
        OperationIndex index = OperationIndex.of(model);
        Set<Shape> actual = index.getErrorBindings(ShapeId.from("ns.foo#CommonError1"));
        Set<Shape> expected = SetUtils.of(
                model.expectShape(ShapeId.from("ns.foo#MyService"), ServiceShape.class),
                model.expectShape(ShapeId.from("ns.foo#C"), OperationShape.class));
        assertThat(actual, equalTo(expected));

        actual = index.getErrorBindings(ShapeId.from("ns.foo#Error1"));
        expected = SetUtils.of(model.expectShape(ShapeId.from("ns.foo#B"), OperationShape.class));
        assertThat(actual, equalTo(expected));

        assertThat(index.getOutputBindings(ShapeId.from("ns.foo#UnusedError")), empty());
    }
}
