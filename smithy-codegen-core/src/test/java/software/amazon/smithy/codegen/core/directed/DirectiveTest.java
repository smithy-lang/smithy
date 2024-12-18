/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core.directed;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.sameInstance;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;

public class DirectiveTest {
    @Test
    public void providesOperations() {
        TestContext context = TestContext.create("directive-operations.smithy", ShapeId.from("smithy.example#Foo"));
        GenerateServiceDirective<TestContext, TestSettings> d = new GenerateServiceDirective<>(
                context,
                context.service());

        Set<OperationShape> operationShapes = d.operations();
        assertThat(operationShapes, sameInstance(d.operations()));

        Set<ShapeId> operations = operationShapes.stream().map(Shape::getId).collect(Collectors.toSet());

        assertThat(operations,
                containsInAnyOrder(ShapeId.from("smithy.example#GetA"),
                        ShapeId.from("smithy.example#DeleteA")));

    }

    @Test
    public void providesConnectedShapes() {
        TestContext context = TestContext.create("directive-operations.smithy", ShapeId.from("smithy.example#Foo"));
        GenerateServiceDirective<TestContext, TestSettings> d = new GenerateServiceDirective<>(
                context,
                context.service());

        Map<ShapeId, Shape> connected = d.connectedShapes();
        assertThat(connected, sameInstance(d.connectedShapes()));

        assertThat(connected, hasKey(ShapeId.from("smithy.example#GetA")));
        assertThat(connected, hasKey(ShapeId.from("smithy.example#DeleteA")));
    }
}
