/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *   http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.model.shapes;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.traits.EnumDefinition;
import software.amazon.smithy.model.traits.EnumTrait;
import software.amazon.smithy.model.traits.EnumValueTrait;
import software.amazon.smithy.model.traits.UnitTypeTrait;
import software.amazon.smithy.model.traits.synthetic.SyntheticEnumTrait;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.SetUtils;

public class EnumShapeTest {
    @Test
    public void returnsAppropriateType() {
        EnumShape.Builder builder = (EnumShape.Builder) EnumShape.builder().id("ns.foo#bar");
        EnumShape shape = builder.addMember("foo", "bar").build();

        assertEquals(shape.getType(), ShapeType.ENUM);
        assertTrue(shape.isEnumShape());
        assertTrue(shape.isStringShape());
        assertTrue(shape.asEnumShape().isPresent());
        assertTrue(shape.asStringShape().isPresent());
    }

    @Test
    public void mustNotContainMembersInShapeId() {
        Assertions.assertThrows(SourceException.class, () -> {
            EnumShape.builder().id("ns.foo#bar$baz").build();
        });
    }

    @Test
    public void addMember() {
        EnumShape.Builder builder = (EnumShape.Builder) EnumShape.builder().id("ns.foo#bar");
        EnumShape shape = builder.addMember("foo", "bar").build();
        assertEquals(shape.getMember("foo").get(),
                MemberShape.builder()
                        .id(shape.getId().withMember("foo"))
                        .target(UnitTypeTrait.UNIT)
                        .addTrait(EnumValueTrait.builder().stringValue("bar").build())
                        .build());

        assertTrue(shape.hasTrait(EnumTrait.class));
        assertEquals(shape.expectTrait(EnumTrait.class).getValues(), ListUtils.of(
                EnumDefinition.builder()
                        .name("foo")
                        .value("bar")
                        .build()
        ));
    }

    @Test
    public void addMemberShape() {
        EnumShape.Builder builder = (EnumShape.Builder) EnumShape.builder().id("ns.foo#bar");
        MemberShape member = MemberShape.builder()
                .id("ns.foo#bar$foo")
                .target(UnitTypeTrait.UNIT)
                .addTrait(EnumValueTrait.builder().stringValue("bar").build())
                .build();
        EnumShape shape = builder.addMember(member).build();
        assertEquals(shape.getMember("foo").get(), member);

        assertTrue(shape.hasTrait(EnumTrait.class));
        assertEquals(shape.expectTrait(EnumTrait.class).getValues(), ListUtils.of(
                EnumDefinition.builder()
                        .name("foo")
                        .value("bar")
                        .build()
        ));
    }

    @Test
    public void memberValueIsAppliedIfNotPresent() {
        EnumShape.Builder builder = (EnumShape.Builder) EnumShape.builder().id("ns.foo#bar");
        MemberShape member = MemberShape.builder()
                .id("ns.foo#bar$foo")
                .target(UnitTypeTrait.UNIT)
                .build();
        EnumShape shape = builder.addMember(member).build();

        MemberShape expected = member.toBuilder()
                .addTrait(EnumValueTrait.builder().stringValue("foo").build())
                .build();
        assertEquals(shape.getMember("foo").get(), expected);

        assertTrue(shape.hasTrait(EnumTrait.class));
        assertEquals(shape.expectTrait(EnumTrait.class).getValues(), ListUtils.of(
                EnumDefinition.builder()
                        .name("foo")
                        .value("foo")
                        .build()
        ));
    }

    @Test
    public void addMemberFromEnumTrait() {
        EnumShape.Builder builder = (EnumShape.Builder) EnumShape.builder().id("ns.foo#bar");
        EnumDefinition enumDefinition = EnumDefinition.builder()
                .name("foo")
                .value("bar")
                .build();
        EnumShape shape = builder.members(EnumTrait.builder().addEnum(enumDefinition).build()).build();

        assertEquals(shape.getMember("foo").get(),
                MemberShape.builder()
                        .id(shape.getId().withMember("foo"))
                        .target(UnitTypeTrait.UNIT)
                        .addTrait(EnumValueTrait.builder().stringValue("bar").build())
                        .build());

        assertTrue(shape.hasTrait(EnumTrait.class));
        assertEquals(shape.expectTrait(EnumTrait.class).getValues(), ListUtils.of(enumDefinition));
    }

    @Test
    public void givenEnumTraitMustUseNames() {
        EnumShape.Builder builder = (EnumShape.Builder) EnumShape.builder().id("ns.foo#bar");
        EnumTrait trait = EnumTrait.builder()
                .addEnum(EnumDefinition.builder()
                        .value("bar")
                        .build())
                .build();

        Assertions.assertThrows(IllegalStateException.class, () -> {
            builder.members(trait);
        });
    }

    @Test
    public void addMultipleMembers() {
        EnumShape.Builder builder = (EnumShape.Builder) EnumShape.builder().id("ns.foo#bar");

        EnumShape shape = builder.members(ListUtils.of(
                MemberShape.builder()
                        .id("ns.foo#bar$foo")
                        .target(UnitTypeTrait.UNIT)
                        .addTrait(EnumValueTrait.builder().stringValue("bar").build())
                        .build(),
                MemberShape.builder()
                        .id("ns.foo#bar$baz")
                        .target(UnitTypeTrait.UNIT)
                        .addTrait(EnumValueTrait.builder().stringValue("bam").build())
                        .build()
        )).build();

        assertEquals(shape.getMember("foo").get(),
                MemberShape.builder()
                        .id(shape.getId().withMember("foo"))
                        .target(UnitTypeTrait.UNIT)
                        .addTrait(EnumValueTrait.builder().stringValue("bar").build())
                        .build());

        assertEquals(shape.getMember("baz").get(),
                MemberShape.builder()
                        .id(shape.getId().withMember("baz"))
                        .target(UnitTypeTrait.UNIT)
                        .addTrait(EnumValueTrait.builder().stringValue("bam").build())
                        .build());

        assertTrue(shape.hasTrait(EnumTrait.class));
        assertEquals(shape.expectTrait(EnumTrait.class).getValues(), ListUtils.of(
                EnumDefinition.builder()
                        .name("foo")
                        .value("bar")
                        .build(),
                EnumDefinition.builder()
                        .name("baz")
                        .value("bam")
                        .build()
        ));
    }

    @Test
    public void removeMember() {
        EnumShape.Builder builder = (EnumShape.Builder) EnumShape.builder().id("ns.foo#bar");

        builder.members(ListUtils.of(
                MemberShape.builder()
                        .id("ns.foo#bar$foo")
                        .target(UnitTypeTrait.UNIT)
                        .addTrait(EnumValueTrait.builder().stringValue("bar").build())
                        .build(),
                MemberShape.builder()
                        .id("ns.foo#bar$baz")
                        .target(UnitTypeTrait.UNIT)
                        .addTrait(EnumValueTrait.builder().stringValue("bam").build())
                        .build()
        ));

        EnumShape shape = builder.removeMember("foo").build();

        assertFalse(shape.getMember("foo").isPresent());

        assertEquals(shape.getMember("baz").get(),
                MemberShape.builder()
                        .id(shape.getId().withMember("baz"))
                        .target(UnitTypeTrait.UNIT)
                        .addTrait(EnumValueTrait.builder().stringValue("bam").build())
                        .build());

        assertTrue(shape.hasTrait(EnumTrait.class));
        assertEquals(shape.expectTrait(EnumTrait.class).getValues(), ListUtils.of(
                EnumDefinition.builder()
                        .name("baz")
                        .value("bam")
                        .build()
        ));
    }

    @Test
    public void clearMembers() {
        EnumShape.Builder builder = (EnumShape.Builder) EnumShape.builder().id("ns.foo#bar");

        EnumShape shape = builder.addMember("foo", "bar")
                .clearMembers()
                .addMember("baz", "bam")
                .build();

        assertEquals(1, shape.members().size());

        assertEquals(shape.getMember("baz").get(),
                MemberShape.builder()
                        .id(shape.getId().withMember("baz"))
                        .target(UnitTypeTrait.UNIT)
                        .addTrait(EnumValueTrait.builder().stringValue("bam").build())
                        .build());

        assertTrue(shape.hasTrait(EnumTrait.class));
        assertEquals(shape.expectTrait(EnumTrait.class).getValues(), ListUtils.of(
                EnumDefinition.builder()
                        .name("baz")
                        .value("bam")
                        .build()
        ));
    }

    @Test
    public void membersMustHaveEnumValueWithStringSet() {
        EnumShape.Builder builder = (EnumShape.Builder) EnumShape.builder().id("ns.foo#bar");
        MemberShape member = MemberShape.builder()
                .id("ns.foo#bar$foo")
                .target(UnitTypeTrait.UNIT)
                .addTrait(EnumValueTrait.builder().intValue(1).build())
                .build();
        Assertions.assertThrows(SourceException.class, () -> {
            builder.addMember(member);
        });
    }

    @Test
    public void membersMustTargetUnit() {
        EnumShape.Builder builder = (EnumShape.Builder) EnumShape.builder().id("ns.foo#bar");
        MemberShape member = MemberShape.builder()
                .id("ns.foo#bar$foo")
                .target("smithy.api#String")
                .addTrait(EnumValueTrait.builder().stringValue("bar").build())
                .build();
        Assertions.assertThrows(SourceException.class, () -> {
            builder.addMember(member);
        });
    }

    @Test
    public void canConvertBaseString() {
        EnumTrait trait = EnumTrait.builder()
                .addEnum(EnumDefinition.builder()
                        .name("foo")
                        .value("bar")
                        .build())
                .build();
        StringShape string = StringShape.builder()
                .id("ns.foo#bar")
                .addTrait(trait)
                .build();
        Optional<EnumShape> optionalEnum = EnumShape.fromStringShape(string);
        assertTrue(optionalEnum.isPresent());
        assertEquals(trait.getValues(), optionalEnum.get().expectTrait(SyntheticEnumTrait.class).getValues());

        assertEquals(optionalEnum.get().getMember("foo").get(),
                MemberShape.builder()
                        .id(optionalEnum.get().getId().withMember("foo"))
                        .target(UnitTypeTrait.UNIT)
                        .addTrait(EnumValueTrait.builder().stringValue("bar").build())
                        .build());
    }

    @Test
    public void cantConvertBaseStringWithoutEnumTrait() {
        StringShape string = StringShape.builder()
                .id("ns.foo#bar")
                .build();
        Optional<EnumShape> optionalEnum = EnumShape.fromStringShape(string);
        assertFalse(optionalEnum.isPresent());
    }

    @Test
    public void cantConvertBaseStringWithNamelessEnumTrait() {
        EnumTrait trait = EnumTrait.builder()
                .addEnum(EnumDefinition.builder()
                        .value("bar")
                        .build())
                .build();
        StringShape string = StringShape.builder()
                .id("ns.foo#bar")
                .addTrait(trait)
                .build();
        Optional<EnumShape> optionalEnum = EnumShape.fromStringShape(string);
        assertFalse(optionalEnum.isPresent());
    }
}
