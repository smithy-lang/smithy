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
        GenerateService<TestContext, Object> d = createDirective("service-title.smithy");

        assertThat(d.serviceTitle(), equalTo("Foo Service"));
    }

    @Test
    public void getsServiceTitleFromSymbolDefault() {
        GenerateService<TestContext, Object> d = createDirective(
                "service-title.smithy", ShapeId.from("smithy.example#Foo2"));

        assertThat(d.serviceTitle(), equalTo("Foo2"));
    }

    @Test
    public void providesPaginatedMap() {
        GenerateService<TestContext, Object> d = createDirective("service-paginated.smithy");

        Map<ShapeId, PaginationInfo> info = d.paginatedOperations();
        assertThat(info, hasKey(ShapeId.from("smithy.example#ListA")));
        assertThat(info, hasKey(ShapeId.from("smithy.example#ListB")));
    }

    @Test
    public void providesEventStreamMap() {
        GenerateService<TestContext, Object> d = createDirective("service-eventstream.smithy");

        Map<ShapeId, EventStreamInfo> input = d.inputEventStreamOperations();
        Map<ShapeId, EventStreamInfo> output = d.outputEventStreamOperations();

        assertThat(input, hasKey(ShapeId.from("smithy.example#GetAndSendMovements")));
        assertThat(output, hasKey(ShapeId.from("smithy.example#GetAndSendMovements")));
    }

    private GenerateService<TestContext, Object> createDirective(String modelFile, ShapeId serviceId) {
        TestContext context = TestContext.create(modelFile, serviceId);
        return new GenerateService<>(context, context.service());
    }

    private GenerateService<TestContext, Object> createDirective(String modelFile) {
        return createDirective(modelFile, ShapeId.from("smithy.example#Foo"));
    }
}
