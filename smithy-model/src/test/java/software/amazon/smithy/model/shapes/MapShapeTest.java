/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.shapes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
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

    @Test
    public void builderUpdatesMembers() {
        MapShape shape = MapShape.builder()
                .id("ns.foo#bar")
                .key(ShapeId.from("ns.foo#bam"))
                .value(ShapeId.from("ns.foo#bam"))
                .id("ns.bar#bar")
                .build();

        assertThat(shape.getKey().getId(), equalTo(ShapeId.from("ns.bar#bar$key")));
        assertThat(shape.getKey().getTarget(), equalTo(ShapeId.from("ns.foo#bam")));
        assertThat(shape.getValue().getId(), equalTo(ShapeId.from("ns.bar#bar$value")));
        assertThat(shape.getValue().getTarget(), equalTo(ShapeId.from("ns.foo#bam")));
    }
}
