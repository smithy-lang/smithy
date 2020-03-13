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
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.traits.SensitiveTrait;

public class ListShapeTest {
    @Test
    public void returnsAppropriateType() {
        ListShape shape = ListShape.builder()
                .id("ns.foo#bar")
                .member(MemberShape.builder().id("ns.foo#bar$member").target("ns.foo#bam").build())
                .build();

        assertEquals(shape.getType(), ShapeType.LIST);
        assertThat(shape, is(shape.expectListShape()));
    }

    @Test
    public void mustNotContainMembersInShapeId() {
        Assertions.assertThrows(SourceException.class, () -> {
            ListShape.builder()
                    .id("ns.foo#bar$baz")
                    .member(MemberShape.builder().id("ns.foo#bar$member").target("ns.foo#bam").build())
                    .build();
        });
    }

    @Test
    public void requiresMember() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            ListShape.builder().id("ns.foo#bar").build();
        });
    }

    @Test
    public void hasMember() {
        MemberShape member = MemberShape.builder().id("ns.foo#bar$member").target("ns.foo#bam").build();
        ListShape shape = ListShape.builder()
                .id("ns.foo#bar")
                .member(member)
                .build();

        assertEquals(member, shape.getMember());
    }

    @Test
    public void differentTypesAreNotEqual() {
        MemberShape member = MemberShape.builder().id("ns.foo#bar$member").target("ns.foo#bam").build();
        ListShape shape = ListShape.builder().id("ns.foo#bar").member(member).build();

        assertNotEquals(shape, "");
    }

    @Test
    public void sameInstanceIsEqual() {
        MemberShape member = MemberShape.builder().id("ns.foo#bar$member").target("ns.foo#bam").build();
        ListShape shape = ListShape.builder().id("ns.foo#bar").member(member).build();

        assertEquals(shape, shape);
    }

    @Test
    public void sameValueIsEqual() {
        MemberShape member = MemberShape.builder().id("ns.foo#bar$member").target("ns.foo#bam").build();
        ListShape shape1 = ListShape.builder().id("ns.foo#bar").member(member).build();
        ListShape shape2 = shape1.toBuilder().build();

        assertEquals(shape1, shape2);
        assertEquals(shape1.hashCode(), shape2.hashCode());
    }

    @Test
    public void differentIdIsNotEqual() {
        MemberShape member1 = MemberShape.builder().id("ns.foo#bar$member").target("ns.foo#bam").build();
        ListShape shape1 = ListShape.builder().id("ns.foo#bar").member(member1).build();
        MemberShape member2 = MemberShape.builder().id("ns.a#b$member").target("ns.foo#bam").build();
        ListShape shape2 = shape1.toBuilder().id("ns.a#b").member(member2).build();

        assertNotEquals(shape1, shape2);
        assertNotEquals(shape1.hashCode(), shape2.hashCode());
    }

    @Test
    public void differentMemberAreNotEqual() {
        MemberShape member1 = MemberShape.builder().id("ns.foo#bar$member").target("ns.foo#bam").build();
        ListShape shape1 = ListShape.builder().id("ns.foo#bar").member(member1).build();
        MemberShape member2 = MemberShape.builder().id("ns.foo#bar$member").target("ns.foo#qux").build();
        ListShape shape2 = ListShape.builder().id("ns.foo#bar").member(member2).build();

        assertNotEquals(shape1, shape2);
    }

    @Test
    public void addMemberWithTarget() {
        ListShape shape = ListShape.builder()
                .id("ns.foo#bar")
                .member(ShapeId.from("ns.foo#bam"))
                .build();

        assertEquals(shape.getMember(),
                     MemberShape.builder().id(shape.getId().withMember("member")).target("ns.foo#bam").build());
    }

    @Test
    public void addMemberWithConsumer() {
        ListShape shape = ListShape.builder()
                .id("ns.foo#bar")
                .member(ShapeId.from("ns.foo#bam"), builder -> builder.addTrait(new SensitiveTrait()))
                .build();

        assertEquals(shape.getMember(),
                     MemberShape.builder()
                             .id(shape.getId().withMember("member"))
                             .target("ns.foo#bam")
                             .addTrait(new SensitiveTrait())
                             .build());
    }

    @Test
    public void returnsMembers() {
        ListShape shape = ListShape.builder().id("ns.foo#bar").member(ShapeId.from("ns.foo#bam")).build();

        assertThat(shape.members(), hasSize(1));
    }
}
