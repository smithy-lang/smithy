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
import static org.hamcrest.Matchers.is;

import java.util.Optional;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;

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
        OperationIndex opIndex = model.getKnowledge(OperationIndex.class);

        assertThat(opIndex.getInput(ShapeId.from("ns.foo#A")), is(Optional.empty()));
        assertThat(opIndex.getOutput(ShapeId.from("ns.foo#A")), is(Optional.empty()));
        assertThat(opIndex.getErrors(ShapeId.from("ns.foo#A")), empty());
    }

    @Test
    public void indexesOperations() {
        OperationIndex opIndex = model.getKnowledge(OperationIndex.class);
        Shape input = model.expectShape(ShapeId.from("ns.foo#Input"));
        Shape output = model.expectShape(ShapeId.from("ns.foo#Output"));
        Shape error1 = model.expectShape(ShapeId.from("ns.foo#Error1"));
        Shape error2 = model.expectShape(ShapeId.from("ns.foo#Error2"));

        assertThat(opIndex.getInput(ShapeId.from("ns.foo#B")), is(Optional.of(input)));
        assertThat(opIndex.getOutput(ShapeId.from("ns.foo#B")), is(Optional.of(output)));
        assertThat(opIndex.getErrors(ShapeId.from("ns.foo#B")), containsInAnyOrder(error1, error2));
    }
}
