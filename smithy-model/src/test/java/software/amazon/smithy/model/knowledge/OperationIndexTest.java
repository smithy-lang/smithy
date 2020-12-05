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

package software.amazon.smithy.model.knowledge;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;

public class OperationIndexTest {

    private static Model model;

    @BeforeAll
    public static void before() {
        model = Model.assembler()
                .addImport(OperationIndexTest.class.getResource("operation-index-test.json"))
                .assemble()
                .unwrap();
    }

    @AfterAll
    public static void after() {
        model = null;
    }

    @Test
    public void indexesEmptyOperations() {
        OperationIndex opIndex = OperationIndex.of(model);

        assertThat(opIndex.getInput(ShapeId.from("ns.foo#A")), is(Optional.empty()));
        assertThat(opIndex.getOutput(ShapeId.from("ns.foo#A")), is(Optional.empty()));
        assertThat(opIndex.getErrors(ShapeId.from("ns.foo#A")), empty());
    }

    @Test
    public void indexesOperations() {
        OperationIndex opIndex = OperationIndex.of(model);
        Shape input = model.getShape(ShapeId.from("ns.foo#Input")).get();
        Shape output = model.getShape(ShapeId.from("ns.foo#Output")).get();
        Shape error1 = model.getShape(ShapeId.from("ns.foo#Error1")).get();
        Shape error2 = model.getShape(ShapeId.from("ns.foo#Error2")).get();

        assertThat(opIndex.getInput(ShapeId.from("ns.foo#B")), is(Optional.of(input)));
        assertThat(opIndex.getOutput(ShapeId.from("ns.foo#B")), is(Optional.of(output)));
        assertThat(opIndex.getErrors(ShapeId.from("ns.foo#B")), containsInAnyOrder(error1, error2));
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
    public void determinesIfShapeIsUsedAsOutput() {
        OperationIndex opIndex = OperationIndex.of(model);
        StructureShape input = model.expectShape(ShapeId.from("ns.foo#Input"), StructureShape.class);
        StructureShape output = model.expectShape(ShapeId.from("ns.foo#Output"), StructureShape.class);

        assertThat(opIndex.isOutputStructure(output), is(true));
        assertThat(opIndex.isOutputStructure(input), is(false));
    }
}
