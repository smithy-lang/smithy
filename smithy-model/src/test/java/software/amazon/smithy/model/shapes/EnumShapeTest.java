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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.traits.DeprecatedTrait;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.model.traits.EnumDefinition;
import software.amazon.smithy.model.traits.EnumTrait;
import software.amazon.smithy.model.traits.EnumValueTrait;
import software.amazon.smithy.model.traits.InternalTrait;
import software.amazon.smithy.model.traits.TagsTrait;
import software.amazon.smithy.model.traits.UnitTypeTrait;
import software.amazon.smithy.model.traits.synthetic.SyntheticEnumTrait;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.MapUtils;

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
                .tags(ListUtils.of("internal"))
                .build();
        EnumShape shape = builder.setMembersFromEnumTrait(EnumTrait.builder().addEnum(enumDefinition).build()).build();

        assertEquals(shape.getMember("foo").get(),
                MemberShape.builder()
                        .id(shape.getId().withMember("foo"))
                        .target(UnitTypeTrait.UNIT)
                        .addTrait(EnumValueTrait.builder().stringValue("bar").build())
                        .addTrait(new InternalTrait())
                        .addTrait(TagsTrait.builder().addValue("internal").build())
                        .build());

        assertTrue(shape.hasTrait(EnumTrait.class));
        assertEquals(shape.expectTrait(EnumTrait.class).getValues(), ListUtils.of(enumDefinition));
    }

    @Test
    public void convertsDocsFromEnumTrait() {
        EnumShape.Builder builder = (EnumShape.Builder) EnumShape.builder().id("ns.foo#bar");
        String docs = "docs";
        EnumDefinition enumDefinition = EnumDefinition.builder()
                .name("foo")
                .value("bar")
                .documentation(docs)
                .build();
        EnumShape shape = builder.setMembersFromEnumTrait(EnumTrait.builder().addEnum(enumDefinition).build()).build();

        assertEquals(shape.getMember("foo").get(),
                MemberShape.builder()
                        .id(shape.getId().withMember("foo"))
                        .target(UnitTypeTrait.UNIT)
                        .addTrait(EnumValueTrait.builder().stringValue("bar").build())
                        .addTrait(new DocumentationTrait(docs))
                        .build());

        assertTrue(shape.hasTrait(EnumTrait.class));
        assertEquals(shape.expectTrait(EnumTrait.class).getValues(), ListUtils.of(enumDefinition));
    }

    @Test
    public void convertsTagsFromEnumTrait() {
        EnumShape.Builder builder = (EnumShape.Builder) EnumShape.builder().id("ns.foo#bar");
        String tag = "tag";
        EnumDefinition enumDefinition = EnumDefinition.builder()
                .name("foo")
                .value("bar")
                .addTag(tag)
                .build();
        EnumShape shape = builder.setMembersFromEnumTrait(EnumTrait.builder().addEnum(enumDefinition).build()).build();

        assertEquals(shape.getMember("foo").get(),
                MemberShape.builder()
                        .id(shape.getId().withMember("foo"))
                        .target(UnitTypeTrait.UNIT)
                        .addTrait(EnumValueTrait.builder().stringValue("bar").build())
                        .addTrait(TagsTrait.builder().addValue(tag).build())
                        .build());

        assertTrue(shape.hasTrait(EnumTrait.class));
        assertEquals(shape.expectTrait(EnumTrait.class).getValues(), ListUtils.of(enumDefinition));
    }

    @Test
    public void convertsDeprecatedFromEnumTrait() {
        EnumShape.Builder builder = (EnumShape.Builder) EnumShape.builder().id("ns.foo#bar");
        EnumDefinition enumDefinition = EnumDefinition.builder()
                .name("foo")
                .value("bar")
                .deprecated(true)
                .build();
        EnumShape shape = builder.setMembersFromEnumTrait(EnumTrait.builder().addEnum(enumDefinition).build()).build();

        assertEquals(shape.getMember("foo").get(),
                MemberShape.builder()
                        .id(shape.getId().withMember("foo"))
                        .target(UnitTypeTrait.UNIT)
                        .addTrait(EnumValueTrait.builder().stringValue("bar").build())
                        .addTrait(DeprecatedTrait.builder().build())
                        .build());

        assertTrue(shape.hasTrait(EnumTrait.class));
        assertEquals(shape.expectTrait(EnumTrait.class).getValues(), ListUtils.of(enumDefinition));
    }

    @Test
    public void convertsEnumUnchanged() {
        EnumShape.Builder builder = (EnumShape.Builder) EnumShape.builder().id("ns.foo#bar");
        EnumShape shape = builder.addMember("foo", "bar").build();
        Optional<EnumShape> converted = EnumShape.fromStringShape(shape);
        assertEquals(shape, converted.get());
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
            builder.setMembersFromEnumTrait(trait);
        });
    }

    @Test
    public void givenEnumTraitMaySynthesizeNames() {
        EnumShape.Builder builder = (EnumShape.Builder) EnumShape.builder().id("ns.foo#bar");
        EnumTrait trait = EnumTrait.builder()
                .addEnum(EnumDefinition.builder()
                        .value("foo:bar")
                        .build())
                .build();
        EnumShape shape = builder.setMembersFromEnumTrait(trait, true).build();

        assertEquals(shape.getMember("foo_bar").get(),
                MemberShape.builder()
                        .id(shape.getId().withMember("foo_bar"))
                        .target(UnitTypeTrait.UNIT)
                        .addTrait(EnumValueTrait.builder().stringValue("foo:bar").build())
                        .build());

        assertTrue(shape.hasTrait(EnumTrait.class));

        EnumDefinition expectedDefinition = EnumDefinition.builder()
                .name("foo_bar")
                .value("foo:bar")
                .build();
        assertEquals(shape.expectTrait(EnumTrait.class).getValues(), ListUtils.of(expectedDefinition));
    }

    @Test
    public void givenEnumTraitMayOnlySynthesizeNamesFromValidValues() {
        EnumShape.Builder builder = (EnumShape.Builder) EnumShape.builder().id("ns.foo#bar");
        EnumTrait trait = EnumTrait.builder()
                .addEnum(EnumDefinition.builder()
                        .value("foo&bar")
                        .build())
                .build();

        Assertions.assertThrows(IllegalStateException.class, () -> {
            builder.setMembersFromEnumTrait(trait, true);
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
                .source("bar.smithy", 1, 1)
                .build();
        Optional<EnumShape> optionalEnum = EnumShape.fromStringShape(string);
        assertTrue(optionalEnum.isPresent());
        SyntheticEnumTrait syntheticEnumTrait = optionalEnum.get().expectTrait(SyntheticEnumTrait.class);
        assertEquals(trait.getValues(), syntheticEnumTrait.getValues());
        assertNotNull(syntheticEnumTrait.getSourceLocation());

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

    @Test
    public void getEnumValues() {
        EnumShape.Builder builder = (EnumShape.Builder) EnumShape.builder().id("ns.foo#bar");
        EnumShape shape = builder.addMember("BAR", "bar").build();

        Map<String, String> expected = MapUtils.of("BAR", "bar");
        assertEquals(expected, shape.getEnumValues());
    }

    @Test
    public void canConvertToEnumWithNamedEnumTrait() {
        EnumTrait trait = EnumTrait.builder()
                .addEnum(EnumDefinition.builder()
                        .value("foo&bar") // even if this value has invalid characters for a synthesized name
                        .name("BAR")
                        .build())
                .build();
        StringShape string = StringShape.builder()
                .id("ns.foo#bar")
                .addTrait(trait)
                .build();
        assertTrue(EnumShape.canConvertToEnum(string, false));
        assertTrue(EnumShape.canConvertToEnum(string, true));
    }

    @Test
    public void canConvertToEnumWithNamelessEnumTrait() {
        EnumTrait trait = EnumTrait.builder()
                .addEnum(EnumDefinition.builder()
                        .value("bar")
                        .build())
                .build();
        StringShape string = StringShape.builder()
                .id("ns.foo#bar")
                .addTrait(trait)
                .build();
        assertFalse(EnumShape.canConvertToEnum(string, false));
        assertTrue(EnumShape.canConvertToEnum(string, true));
    }

    @Test
    public void canConvertToEnumWithNonConvertableNamelessEnumTrait() {
        EnumTrait trait = EnumTrait.builder()
                .addEnum(EnumDefinition.builder()
                        .value("foo&bar")
                        .build())
                .build();
        StringShape string = StringShape.builder()
                .id("ns.foo#bar")
                .addTrait(trait)
                .build();
        assertFalse(EnumShape.canConvertToEnum(string, false));
        assertFalse(EnumShape.canConvertToEnum(string, true));
    }
}
