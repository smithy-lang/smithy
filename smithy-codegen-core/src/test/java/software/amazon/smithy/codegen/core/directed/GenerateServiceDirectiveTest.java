/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core.directed;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;

import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.knowledge.EventStreamInfo;
import software.amazon.smithy.model.knowledge.PaginationInfo;
import software.amazon.smithy.model.shapes.ShapeId;

public class GenerateServiceDirectiveTest {
    @Test
    public void getsServiceTitleWithExplicitTrait() {
        GenerateServiceDirective<TestContext, TestSettings> d = createDirective("service-title.smithy");

        assertThat(d.serviceTitle(), equalTo("Foo Service"));
    }

    @Test
    public void getsServiceTitleFromSymbolDefault() {
        GenerateServiceDirective<TestContext, TestSettings> d = createDirective(
                "service-title.smithy",
                ShapeId.from("smithy.example#Foo2"));

        assertThat(d.serviceTitle(), equalTo("Foo2"));
    }

    @Test
    public void providesPaginatedMap() {
        GenerateServiceDirective<TestContext, TestSettings> d = createDirective("service-paginated.smithy");

        Map<ShapeId, PaginationInfo> info = d.paginatedOperations();
        assertThat(info, hasKey(ShapeId.from("smithy.example#ListA")));
        assertThat(info, hasKey(ShapeId.from("smithy.example#ListB")));
    }

    @Test
    public void providesEventStreamMap() {
        GenerateServiceDirective<TestContext, TestSettings> d = createDirective("service-eventstream.smithy");

        Map<ShapeId, EventStreamInfo> input = d.inputEventStreamOperations();
        Map<ShapeId, EventStreamInfo> output = d.outputEventStreamOperations();

        assertThat(input, hasKey(ShapeId.from("smithy.example#GetAndSendMovements")));
        assertThat(output, hasKey(ShapeId.from("smithy.example#GetAndSendMovements")));
    }

    private GenerateServiceDirective<TestContext, TestSettings> createDirective(String modelFile, ShapeId serviceId) {
        TestContext context = TestContext.create(modelFile, serviceId);
        return new GenerateServiceDirective<>(context, context.service());
    }

    private GenerateServiceDirective<TestContext, TestSettings> createDirective(String modelFile) {
        return createDirective(modelFile, ShapeId.from("smithy.example#Foo"));
    }
}
