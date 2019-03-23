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

package software.amazon.smithy.model.selector;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;

import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeIndex;

public class NeighborSelectorTest {
    private static ShapeIndex index;

    @BeforeAll
    public static void before() {
        index = Model.assembler()
                .addImport(NeighborSelectorTest.class.getResource("neighbor-test.smithy"))
                .assemble()
                .unwrap()
                .getShapeIndex();
    }

    @AfterAll
    public static void after() {
        index = null;
    }

    private Set<String> selectIds(String expression) {
        return Selector.parse(expression)
                .select(index)
                .stream()
                .map(Shape::getId)
                .map(ShapeId::toString)
                .collect(Collectors.toSet());
    }

    @Test
    public void undirectedEdgeTraversal() {
        Set<String> result = selectIds("operation > *");

        assertThat(result, containsInAnyOrder(
                "smithy.example#Input", "smithy.example#Output", "smithy.example#Error"));
    }

    @Test
    public void directedMultiEdgeTraversal() {
        Set<String> result = selectIds("operation -[input, output]->");

        assertThat(result, contains("smithy.example#Input", "smithy.example#Output"));
    }
}
