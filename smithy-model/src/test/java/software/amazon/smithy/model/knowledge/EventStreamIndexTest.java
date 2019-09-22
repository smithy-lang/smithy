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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;

public class EventStreamIndexTest {
    private static Model model;

    @BeforeAll
    public static void beforeAll() {
        model = Model.assembler()
                .addImport(EventStreamIndexTest.class.getResource("event-stream-index.json"))
                .assemble()
                .unwrap();
    }

    @Test
    public void providesEmptyOptionalWhenNotShape() {
        EventStreamIndex index = model.getKnowledge(EventStreamIndex.class);

        assertThat(index.getInputInfo(ShapeId.from("com.foo#Missing")), equalTo(Optional.empty()));
    }

    @Test
    public void providesEmptyOptionalWhenNotOperation() {
        EventStreamIndex index = model.getKnowledge(EventStreamIndex.class);

        assertThat(index.getInputInfo(ShapeId.from("smithy.api#String")), equalTo(Optional.empty()));
    }

    @Test
    public void providesEmptyOptionalWhenNoInput() {
        EventStreamIndex index = model.getKnowledge(EventStreamIndex.class);

        assertThat(index.getInputInfo(ShapeId.from("example.smithy#EmptyOperation")),
                   equalTo(Optional.empty()));
    }

    @Test
    public void providesEmptyOptionalWhenNoOutput() {
        EventStreamIndex index = model.getKnowledge(EventStreamIndex.class);

        assertThat(index.getOutputInfo(ShapeId.from("example.smithy#EmptyOperation")),
                   equalTo(Optional.empty()));
    }

    @Test
    public void providesEmptyOptionalWhenNoEventStreamTarget() {
        EventStreamIndex index = model.getKnowledge(EventStreamIndex.class);

        assertThat(index.getOutputInfo(ShapeId.from("example.smithy#NotEventStreamOperation")),
                   equalTo(Optional.empty()));
    }

    @Test
    public void returnsEventStreamInputInformation() {
        EventStreamIndex index = model.getKnowledge(EventStreamIndex.class);

        assertThat(index.getOutputInfo(ShapeId.from("example.smithy#NotEventStreamOperation")),
                   equalTo(Optional.empty()));
    }

    @Test
    public void returnsEventStreamOutputInformation() {
        EventStreamIndex index = model.getKnowledge(EventStreamIndex.class);

        assertTrue(index.getInputInfo(ShapeId.from("example.smithy#EventStreamOperation")).isPresent());
        assertTrue(index.getOutputInfo(ShapeId.from("example.smithy#EventStreamOperation")).isPresent());
        EventStreamInfo input = index.getInputInfo(
                ShapeId.from("example.smithy#EventStreamOperation")).get();
        EventStreamInfo output = index.getOutputInfo(
                ShapeId.from("example.smithy#EventStreamOperation")).get();

        assertThat(input.getOperation().getId(), equalTo(ShapeId.from("example.smithy#EventStreamOperation")));
        assertThat(input.getStructure().getId(), equalTo(ShapeId.from("example.smithy#EventStreamOperationInput")));
        assertThat(input.getEventStreamMember().getId(),
                   equalTo(ShapeId.from("example.smithy#EventStreamOperationInput$c")));
        assertThat(input.getEventStreamTarget().getId(), equalTo(ShapeId.from("example.smithy#InputEventStream")));
        assertThat(input.getInitialMessageMembers(), hasKey("a"));
        assertThat(input.getInitialMessageMembers(), hasKey("b"));
        assertThat(input.getInitialMessageMembers(), not(hasKey("c")));
        assertThat(input.getInitialMessageTargets().get("a").getId(), equalTo(ShapeId.from("smithy.api#String")));
        assertThat(input.getInitialMessageTargets().get("b").getId(), equalTo(ShapeId.from("smithy.api#Integer")));

        assertThat(output.getOperation().getId(), equalTo(ShapeId.from("example.smithy#EventStreamOperation")));
        assertThat(output.getStructure().getId(), equalTo(ShapeId.from("example.smithy#EventStreamOperationOutput")));
        assertThat(output.getEventStreamMember().getId(),
                   equalTo(ShapeId.from("example.smithy#EventStreamOperationOutput$c")));
        assertThat(output.getEventStreamTarget().getId(), equalTo(ShapeId.from("example.smithy#OutputEventStream")));
        assertThat(output.getInitialMessageMembers(), hasKey("a"));
        assertThat(output.getInitialMessageMembers(), hasKey("b"));
        assertThat(output.getInitialMessageMembers(), not(hasKey("c")));
        assertThat(output.getInitialMessageTargets().get("a").getId(), equalTo(ShapeId.from("smithy.api#String")));
        assertThat(output.getInitialMessageTargets().get("b").getId(), equalTo(ShapeId.from("smithy.api#Integer")));

        assertThat(input, not(equalTo(output)));
    }

    @Test
    public void loadsSingleEventEventStreams() {
        EventStreamIndex index = model.getKnowledge(EventStreamIndex.class);
        EventStreamInfo info = index.getInputInfo(ShapeId.from("example.smithy#SingleEventOperation")).get();

        assertThat(info.getEvents(), hasKey("messages"));
    }
}
