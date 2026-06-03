/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core.directed;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.codegen.core.CodegenException;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;

public class DirectiveTest {
    @Test
    public void providesOperations() {
        TestContext context = TestContext.create("directive-operations.smithy", ShapeId.from("smithy.example#Foo"));
        GenerateServiceDirective<TestContext, TestSettings> d = new GenerateServiceDirective<>(
                context,
                context.service(),
                null,
                false,
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
                context.service(),
                null,
                false,
                context.service());

        Map<ShapeId, Shape> connected = d.connectedShapes();
        assertThat(connected, sameInstance(d.connectedShapes()));

        assertThat(connected, hasKey(ShapeId.from("smithy.example#GetA")));
        assertThat(connected, hasKey(ShapeId.from("smithy.example#DeleteA")));
    }

    @Test
    public void providesOperationsForShapeClosure() {
        TestContext context = TestContext.create("closure-model.smithy");
        CustomizeDirective<TestContext, TestSettings> d = new CustomizeDirective<>(
                context,
                null,
                "smithy.example#getCityClosure",
                false);

        Set<OperationShape> operationShapes = d.operations();
        assertThat(operationShapes, sameInstance(d.operations()));

        Set<ShapeId> operations = operationShapes.stream().map(Shape::getId).collect(Collectors.toSet());
        assertThat(operations, containsInAnyOrder(ShapeId.from("smithy.example#GetCity")));
    }

    @Test
    public void providesConnectedShapesForShapeClosure() {
        TestContext context = TestContext.create("closure-model.smithy");
        CustomizeDirective<TestContext, TestSettings> d = new CustomizeDirective<>(
                context,
                null,
                "smithy.example#getCityClosure",
                false);

        Map<ShapeId, Shape> connected = d.connectedShapes();
        assertThat(connected, sameInstance(d.connectedShapes()));

        assertThat(connected, hasKey(ShapeId.from("smithy.example#GetCity")));
        assertThat(connected, hasKey(ShapeId.from("smithy.example#GetCityInput")));
        assertThat(connected, hasKey(ShapeId.from("smithy.example#GetCityOutput")));
        assertThat(connected, hasKey(ShapeId.from("smithy.example#City")));
        assertThat(connected, hasKey(ShapeId.from("smithy.example#Conditions")));

        // Prelude shapes reached through members are not generated, so they are excluded,
        // matching the set the director generates.
        assertThat(connected, not(hasKey(ShapeId.from("smithy.api#String"))));
    }

    @Test
    public void operationsAreEmptyForTypeCodegen() {
        TestContext context = TestContext.create("closure-model.smithy");
        CustomizeDirective<TestContext, TestSettings> d = new CustomizeDirective<>(
                context,
                null,
                "smithy.example#getCityClosure",
                true);

        // The closure contains the GetCity operation, but no operations are generated
        // during type codegen, so operations() is empty.
        assertThat(d.operations(), empty());
    }

    @Test
    public void connectedShapesExcludeServiceShapesForTypeCodegen() {
        TestContext context = TestContext.create("closure-model.smithy");
        CustomizeDirective<TestContext, TestSettings> d = new CustomizeDirective<>(
                context,
                null,
                "smithy.example#getCityClosure",
                true);

        Map<ShapeId, Shape> connected = d.connectedShapes();

        // Data shapes reached through the operation are still part of the generated set.
        assertThat(connected, hasKey(ShapeId.from("smithy.example#GetCityInput")));
        assertThat(connected, hasKey(ShapeId.from("smithy.example#GetCityOutput")));
        assertThat(connected, hasKey(ShapeId.from("smithy.example#City")));
        assertThat(connected, hasKey(ShapeId.from("smithy.example#Conditions")));

        // The operation itself is not generated, so it is excluded.
        assertThat(connected, not(hasKey(ShapeId.from("smithy.example#GetCity"))));
    }

    @Test
    public void serviceDirectiveHasServiceAndNoClosureId() {
        TestContext context = TestContext.create("directive-operations.smithy", ShapeId.from("smithy.example#Foo"));
        CustomizeDirective<TestContext, TestSettings> d =
                new CustomizeDirective<>(context, context.service(), null, false);

        assertThat(d.getService().get().getId(), equalTo(ShapeId.from("smithy.example#Foo")));
        assertFalse(d.getShapeClosureId().isPresent());
    }

    @Test
    public void closureDirectiveThrowsFromDeprecatedServiceAccessor() {
        TestContext context = TestContext.create("closure-model.smithy");
        CustomizeDirective<TestContext, TestSettings> d = new CustomizeDirective<>(
                context,
                null,
                "smithy.example#getCityClosure",
                false);

        // The deprecated service() accessor throws rather than returning null when code
        // generation is driven by a closure.
        assertFalse(d.getService().isPresent());
        assertThat(d.getShapeClosureId().get(), equalTo("smithy.example#getCityClosure"));
        CodegenException e = Assertions.assertThrows(CodegenException.class, d::service);
        assertThat(e.getMessage(), containsString("smithy.example#getCityClosure"));
    }
}
