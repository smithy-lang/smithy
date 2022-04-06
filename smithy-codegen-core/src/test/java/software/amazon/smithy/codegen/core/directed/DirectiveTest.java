/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
                context, context.service());

        Set<OperationShape> operationShapes = d.operations();
        assertThat(operationShapes, sameInstance(d.operations()));

        Set<ShapeId> operations = operationShapes.stream().map(Shape::getId).collect(Collectors.toSet());

        assertThat(operations, containsInAnyOrder(ShapeId.from("smithy.example#GetA"),
                                                  ShapeId.from("smithy.example#DeleteA")));

    }

    @Test
    public void providesConnectedShapes() {
        TestContext context = TestContext.create("directive-operations.smithy", ShapeId.from("smithy.example#Foo"));
        GenerateServiceDirective<TestContext, TestSettings> d = new GenerateServiceDirective<>(
                context, context.service());

        Map<ShapeId, Shape> connected = d.connectedShapes();
        assertThat(connected, sameInstance(d.connectedShapes()));

        assertThat(connected, hasKey(ShapeId.from("smithy.example#GetA")));
        assertThat(connected, hasKey(ShapeId.from("smithy.example#DeleteA")));
    }
}
