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

public class MapShapeTest {
    @Test
    public void returnsAppropriateType() {
        MapShape shape = MapShape.builder()
                .id("ns.foo#bar")
                .key(MemberShape.builder().id("ns.foo#bar$key").target("ns.foo#bam").build())
                .value(MemberShape.builder().id("ns.foo#bar$value").target("ns.foo#bam").build())
                .build();

        assertEquals(shape.getType(), ShapeType.MAP);
        assertThat(shape, is(shape.expectMapShape()));
    }

    @Test
    public void mustNotContainMembersInShapeId() {
        Assertions.assertThrows(SourceException.class, () -> {
            MapShape.builder()
                    .id("ns.foo#bar$baz")
                    .key(MemberShape.builder().id("ns.foo#bar$key").target("ns.foo#bam").build())
                    .value(MemberShape.builder().id("ns.foo#bar$value").target("ns.foo#bam").build())
                    .build();
        });
    }

    @Test
    public void addMemberWithTarget() {
        MapShape shape = MapShape.builder()
                .id("ns.foo#bar")
                .key(ShapeId.from("ns.foo#bam"))
                .value(ShapeId.from("ns.foo#bam"))
                .build();

        assertEquals(shape.getKey(),
                     MemberShape.builder().id(shape.getId().withMember("key")).target("ns.foo#bam").build());
        assertEquals(shape.getValue(),
                     MemberShape.builder().id(shape.getId().withMember("value")).target("ns.foo#bam").build());
    }

    @Test
    public void addMemberWithConsumer() {
        MapShape shape = MapShape.builder()
                .id("ns.foo#bar")
                .key(ShapeId.from("ns.foo#bam"), builder -> builder.addTrait(new SensitiveTrait()))
                .value(ShapeId.from("ns.foo#bam"), builder -> builder.addTrait(new SensitiveTrait()))
                .build();

        assertEquals(shape.getKey(),
                     MemberShape.builder()
                             .id(shape.getId().withMember("key"))
                             .target("ns.foo#bam")
                             .addTrait(new SensitiveTrait())
                             .build());
        assertEquals(shape.getValue(),
                     MemberShape.builder()
                             .id(shape.getId().withMember("value"))
                             .target("ns.foo#bam")
                             .addTrait(new SensitiveTrait())
                             .build());
    }

    @Test
    public void returnsMembers() {
        MapShape shape = MapShape.builder()
                .id("ns.foo#bar")
                .key(ShapeId.from("ns.foo#bam"))
                .value(ShapeId.from("ns.foo#bam"))
                .build();

        assertThat(shape.members(), hasSize(2));
    }
}
