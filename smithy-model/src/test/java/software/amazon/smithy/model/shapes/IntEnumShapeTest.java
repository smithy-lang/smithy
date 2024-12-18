/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.shapes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.traits.EnumValueTrait;
import software.amazon.smithy.model.traits.UnitTypeTrait;
import software.amazon.smithy.utils.MapUtils;

public class IntEnumShapeTest {
    @Test
    public void returnsAppropriateType() {
        IntEnumShape.Builder builder = (IntEnumShape.Builder) IntEnumShape.builder().id("ns.foo#bar");
        IntEnumShape shape = builder.addMember("foo", 1).build();

        assertEquals(shape.getType(), ShapeType.INT_ENUM);
        assertTrue(shape.isIntEnumShape());
        assertTrue(shape.isIntegerShape());
        assertTrue(shape.asIntEnumShape().isPresent());
        assertTrue(shape.asIntegerShape().isPresent());
    }

    @Test
    public void mustNotContainMembersInShapeId() {
        Assertions.assertThrows(SourceException.class, () -> {
            IntEnumShape.builder().id("ns.foo#bar$baz").build();
        });
    }

    @Test
    public void addMember() {
        IntEnumShape.Builder builder = (IntEnumShape.Builder) IntEnumShape.builder().id("ns.foo#bar");
        IntEnumShape shape = builder.addMember("foo", 1).build();
        assertEquals(shape.getMember("foo").get(),
                MemberShape.builder()
                        .id(shape.getId().withMember("foo"))
                        .target(UnitTypeTrait.UNIT)
                        .addTrait(EnumValueTrait.builder().intValue(1).build())
                        .build());
    }

    @Test
    public void addMemberShape() {
        IntEnumShape.Builder builder = (IntEnumShape.Builder) IntEnumShape.builder().id("ns.foo#bar");
        MemberShape member = MemberShape.builder()
                .id("ns.foo#bar$foo")
                .target(UnitTypeTrait.UNIT)
                .addTrait(EnumValueTrait.builder().intValue(1).build())
                .build();
        IntEnumShape shape = builder.addMember(member).build();
        assertEquals(shape.getMember("foo").get(), member);
    }

    @Test
    public void addMultipleMembers() {
        IntEnumShape.Builder builder = (IntEnumShape.Builder) IntEnumShape.builder().id("ns.foo#bar");
        IntEnumShape shape = builder
                .addMember("foo", 1)
                .addMember("bar", 2)
                .build();
        assertEquals(shape.getMember("foo").get(),
                MemberShape.builder()
                        .id(shape.getId().withMember("foo"))
                        .target(UnitTypeTrait.UNIT)
                        .addTrait(EnumValueTrait.builder().intValue(1).build())
                        .build());
        assertEquals(shape.getMember("bar").get(),
                MemberShape.builder()
                        .id(shape.getId().withMember("bar"))
                        .target(UnitTypeTrait.UNIT)
                        .addTrait(EnumValueTrait.builder().intValue(2).build())
                        .build());
    }

    @Test
    public void removeMember() {
        IntEnumShape.Builder builder = (IntEnumShape.Builder) IntEnumShape.builder().id("ns.foo#bar");
        IntEnumShape shape = builder
                .addMember("foo", 1)
                .removeMember("foo")
                .addMember("bar", 2)
                .build();
        assertFalse(shape.getMember("foo").isPresent());
        assertEquals(shape.getMember("bar").get(),
                MemberShape.builder()
                        .id(shape.getId().withMember("bar"))
                        .target(UnitTypeTrait.UNIT)
                        .addTrait(EnumValueTrait.builder().intValue(2).build())
                        .build());
    }

    @Test
    public void clearMembers() {
        IntEnumShape.Builder builder = (IntEnumShape.Builder) IntEnumShape.builder().id("ns.foo#bar");
        IntEnumShape shape = builder
                .addMember("foo", 1)
                .clearMembers()
                .addMember("bar", 2)
                .build();
        assertFalse(shape.getMember("foo").isPresent());
        assertEquals(shape.getMember("bar").get(),
                MemberShape.builder()
                        .id(shape.getId().withMember("bar"))
                        .target(UnitTypeTrait.UNIT)
                        .addTrait(EnumValueTrait.builder().intValue(2).build())
                        .build());
    }

    @Test
    public void idMustBeSetFirst() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            IntEnumShape.builder().addMember("foo", 1).build();
        });
    }

    @Test
    public void membersMustTargetUnit() {
        IntEnumShape.Builder builder = (IntEnumShape.Builder) IntEnumShape.builder().id("ns.foo#bar");
        MemberShape member = MemberShape.builder()
                .id("ns.foo#bar$foo")
                .target("smithy.api#Integer")
                .addTrait(EnumValueTrait.builder().intValue(1).build())
                .build();
        Assertions.assertThrows(SourceException.class, () -> {
            builder.addMember(member);
        });
    }

    @Test
    public void getEnumValues() {
        IntEnumShape.Builder builder = (IntEnumShape.Builder) IntEnumShape.builder().id("ns.foo#bar");
        IntEnumShape shape = builder.addMember("FOO", 0).addMember("BAR", 1).build();

        Map<String, Integer> expected = MapUtils.of("FOO", 0, "BAR", 1);
        assertEquals(expected, shape.getEnumValues());
    }
}
