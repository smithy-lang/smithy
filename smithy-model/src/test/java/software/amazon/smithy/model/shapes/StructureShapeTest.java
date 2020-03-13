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

package software.amazon.smithy.model.shapes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.traits.SensitiveTrait;

public class StructureShapeTest {
    @Test
    public void returnsAppropriateType() {
        StructureShape shape = StructureShape.builder().id("ns.foo#bar").build();

        assertEquals(shape.getType(), ShapeType.STRUCTURE);
        assertThat(shape, is(shape.expectStructureShape()));
    }

    @Test
    public void mustNotContainMembersInShapeId() {
        Assertions.assertThrows(SourceException.class, () -> {
            StructureShape.builder().id("ns.foo#bar$baz").build();
        });
    }

    @Test
    public void addMemberWithTarget() {
        StructureShape shape = StructureShape.builder()
                .id("ns.foo#bar")
                .addMember("foo", ShapeId.from("ns.foo#bam"))
                .build();

        assertEquals(shape.getMember("foo").get(),
                     MemberShape.builder().id(shape.getId().withMember("foo")).target("ns.foo#bam").build());
    }

    @Test
    public void addMemberWithConsumer() {
        StructureShape shape = StructureShape.builder()
                .id("ns.foo#bar")
                .addMember("foo", ShapeId.from("ns.foo#bam"), builder -> builder.addTrait(new SensitiveTrait()))
                .build();

        assertEquals(shape.getMember("foo").get(),
                     MemberShape.builder()
                             .id(shape.getId().withMember("foo"))
                             .target("ns.foo#bam")
                             .addTrait(new SensitiveTrait())
                             .build());
    }

    @Test
    public void returnsMembers() {
        StructureShape shape = StructureShape.builder()
                .id("ns.foo#bar")
                .addMember("foo", ShapeId.from("ns.foo#bam"))
                .addMember("baz", ShapeId.from("ns.foo#bam"))
                .build();

        assertThat(shape.members(), hasSize(2));
    }
}
