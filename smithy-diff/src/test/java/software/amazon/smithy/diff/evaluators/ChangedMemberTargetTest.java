/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.diff.evaluators;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.diff.ModelDiff;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.TimestampShape;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.model.traits.EnumDefinition;
import software.amazon.smithy.model.traits.EnumTrait;
import software.amazon.smithy.model.traits.SensitiveTrait;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;

public class ChangedMemberTargetTest {
    @Test
    public void detectsIncompatibleTypeChanges() {
        SourceLocation source = new SourceLocation("a.smithy", 2, 3);
        Shape shape1 = StringShape.builder().id("foo.baz#String").build();
        MemberShape member1 = MemberShape.builder().id("foo.baz#List$member").target(shape1.getId()).build();
        ListShape list1 = ListShape.builder().id("foo.baz#List").member(member1).build();
        Shape shape2 = TimestampShape.builder().id("foo.baz#Timestamp").build();
        MemberShape member2 = MemberShape.builder()
                .id("foo.baz#List$member")
                .target(shape2.getId())
                .source(source)
                .build();
        ListShape list2 = ListShape.builder().id("foo.baz#List").member(member2).build();
        Model modelA = Model.assembler().addShapes(shape1, shape2, member1, list1).assemble().unwrap();
        Model modelB = Model.assembler().addShapes(shape1, shape2, member2, list2).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "ChangedMemberTarget").size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, "ChangedMemberTarget").get(0).getSourceLocation(),
                equalTo(source));
        assertThat(TestHelper.findEvents(events, member2.getId()).size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, Severity.ERROR).size(), equalTo(1));
    }

    @Test
    public void detectsIncompatibleTargetChanges() {
        Shape shape1 = StructureShape.builder().id("foo.baz#Shape1").build();
        MemberShape member1 = MemberShape.builder().id("foo.baz#List$member").target(shape1.getId()).build();
        ListShape list1 = ListShape.builder().id("foo.baz#List").member(member1).build();
        Shape shape2 = StructureShape.builder().id("foo.baz#Shape2").build();
        MemberShape member2 = MemberShape.builder().id("foo.baz#List$member").target(shape2.getId()).build();
        ListShape list2 = ListShape.builder().id("foo.baz#List").member(member2).build();
        Model modelA = Model.assembler().addShapes(shape1, shape2, member1, list1).assemble().unwrap();
        Model modelB = Model.assembler().addShapes(shape1, shape2, member2, list2).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "ChangedMemberTarget").size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, member2.getId()).size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, Severity.ERROR).size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, "ChangedMemberTarget").get(0).getMessage(),
                equalTo("The shape targeted by the member `foo.baz#List$member` changed from "
                        + "`foo.baz#Shape1` (structure) to `foo.baz#Shape2` (structure). The name of a "
                        + "structure is significant."));
    }

    @Test
    public void detectsCompatibleChanges() {
        StringShape shape1 = StringShape.builder().id("foo.baz#String1").build();
        MemberShape member1 = MemberShape.builder().id("foo.baz#List$member").target(shape1.getId()).build();
        ListShape list1 = ListShape.builder().id("foo.baz#List").member(member1).build();
        StringShape shape2 = StringShape.builder().id("foo.baz#String2").build();
        MemberShape member2 = MemberShape.builder().id("foo.baz#List$member").target(shape2.getId()).build();
        ListShape list2 = ListShape.builder().id("foo.baz#List").member(member2).build();
        Model modelA = Model.assembler().addShapes(shape1, shape2, member1, list1).assemble().unwrap();
        Model modelB = Model.assembler().addShapes(shape1, shape2, member2, list2).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "ChangedMemberTarget").size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, member2.getId()).size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, Severity.WARNING).size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, "ChangedMemberTarget").get(0).getMessage(),
                equalTo("The shape targeted by the member `foo.baz#List$member` changed from "
                        + "`foo.baz#String1` (string) to `foo.baz#String2` (string). "
                        + "This was determined backward compatible."));
    }

    @Test
    public void detectsIncompatibleEnumTraitTargetChange() {
        EnumTrait enumTrait = EnumTrait.builder()
                .addEnum(EnumDefinition.builder().name("FOO").value("foo").build())
                .build();

        StringShape shape1 = StringShape.builder().id("foo.baz#String1").addTrait(enumTrait).build();
        MemberShape member1 = MemberShape.builder().id("foo.baz#List$member").target(shape1.getId()).build();
        ListShape list1 = ListShape.builder().id("foo.baz#List").member(member1).build();

        StringShape shape2 = StringShape.builder().id("foo.baz#String2").addTrait(enumTrait).build();
        MemberShape member2 = MemberShape.builder().id("foo.baz#List$member").target(shape2.getId()).build();
        ListShape list2 = ListShape.builder().id("foo.baz#List").member(member2).build();

        Model modelA = Model.assembler().addShapes(shape1, shape2, member1, list1).assemble().unwrap();
        Model modelB = Model.assembler().addShapes(shape1, shape2, member2, list2).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "ChangedMemberTarget").size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, member2.getId()).size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, Severity.ERROR).size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, "ChangedMemberTarget").get(0).getMessage(),
                equalTo("The shape targeted by the member `foo.baz#List$member` changed from `foo.baz#String1` "
                        + "(string) to `foo.baz#String2` (string). The `smithy.api#enum` trait was found on the "
                        + "target, so the name of the targeted shape matters for codegen."));
    }

    @Test
    public void detectsTraitRemovalOnMemberTarget() {
        EnumTrait enumTrait = EnumTrait.builder()
                .addEnum(EnumDefinition.builder().name("FOO").value("foo").build())
                .build();

        StringShape shape1 = StringShape.builder().id("foo.baz#String1").addTrait(enumTrait).build();
        MemberShape member1 = MemberShape.builder().id("foo.baz#List$member").target(shape1.getId()).build();
        ListShape list1 = ListShape.builder().id("foo.baz#List").member(member1).build();

        StringShape shape2 = StringShape.builder().id("foo.baz#String2").build();
        MemberShape member2 = MemberShape.builder().id("foo.baz#List$member").target(shape2.getId()).build();
        ListShape list2 = ListShape.builder().id("foo.baz#List").member(member2).build();

        Model modelA = Model.assembler().addShapes(shape1, shape2, member1, list1).assemble().unwrap();
        Model modelB = Model.assembler().addShapes(shape1, shape2, member2, list2).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "ChangedMemberTarget").size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, member2.getId()).size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, Severity.ERROR).size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, "ChangedMemberTarget").get(0).getMessage(),
                equalTo("The shape targeted by the member `foo.baz#List$member` changed from "
                        + "`foo.baz#String1` (string) to `foo.baz#String2` (string). The `smithy.api#enum` trait "
                        + "was found on the target, so the name of the targeted shape matters for codegen."));
    }

    @Test
    public void detectsTraitAddedToMemberTarget() {
        StringShape shape1 = StringShape.builder().id("foo.baz#String1").build();
        MemberShape member1 = MemberShape.builder().id("foo.baz#List$member").target(shape1.getId()).build();
        ListShape list1 = ListShape.builder().id("foo.baz#List").member(member1).build();

        StringShape shape2 = StringShape.builder().id("foo.baz#String2").addTrait(new SensitiveTrait()).build();
        MemberShape member2 = MemberShape.builder().id("foo.baz#List$member").target(shape2.getId()).build();
        ListShape list2 = ListShape.builder().id("foo.baz#List").member(member2).build();

        Model modelA = Model.assembler().addShapes(shape1, shape2, member1, list1).assemble().unwrap();
        Model modelB = Model.assembler().addShapes(shape1, shape2, member2, list2).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "ChangedMemberTarget").size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, member2.getId()).size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, Severity.WARNING).size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, "ChangedMemberTarget").get(0).getMessage(),
                equalTo(String
                        .format("The shape targeted by the member `foo.baz#List$member` changed from `foo.baz#String1` "
                                + "(string) to `foo.baz#String2` (string). This was determined backward compatible. This will "
                                + "result in the following effective differences:%n%n"
                                + "- [NOTE] Added trait `smithy.api#sensitive` with value `{}`")));
    }

    @Test
    public void detectsTraitChangedOnMemberTarget() {
        StringShape shape1 = StringShape.builder().id("foo.baz#String1").addTrait(new DocumentationTrait("a")).build();
        MemberShape member1 = MemberShape.builder().id("foo.baz#List$member").target(shape1.getId()).build();
        ListShape list1 = ListShape.builder().id("foo.baz#List").member(member1).build();

        StringShape shape2 = StringShape.builder().id("foo.baz#String2").addTrait(new DocumentationTrait("b")).build();
        MemberShape member2 = MemberShape.builder().id("foo.baz#List$member").target(shape2.getId()).build();
        ListShape list2 = ListShape.builder().id("foo.baz#List").member(member2).build();

        Model modelA = Model.assembler().addShapes(shape1, shape2, member1, list1).assemble().unwrap();
        Model modelB = Model.assembler().addShapes(shape1, shape2, member2, list2).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "ChangedMemberTarget").size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, member2.getId()).size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, Severity.WARNING).size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, "ChangedMemberTarget").get(0).getMessage(),
                equalTo(String
                        .format("The shape targeted by the member `foo.baz#List$member` changed from `foo.baz#String1` "
                                + "(string) to `foo.baz#String2` (string). This was determined backward compatible. This will "
                                + "result in the following effective differences:%n%n"
                                + "- [NOTE] Changed trait `smithy.api#documentation` from `a` to `b`")));
    }

    @Test
    public void detectsNestedListWithUnchangedMemberTarget() {
        Model modelA = Model.assembler()
                .addImport(getClass().getResource("changed-member-target/nested-list-unchanged-member-a.smithy"))
                .assemble()
                .unwrap();
        Model modelB = Model.assembler()
                .addImport(getClass().getResource("changed-member-target/nested-list-unchanged-member-b.smithy"))
                .assemble()
                .unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "ChangedMemberTarget").size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, Severity.WARNING).size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, "ChangedMemberTarget").get(0).getMessage(),
                equalTo("The shape targeted by the member `smithy.example#A$member` changed from "
                        + "`smithy.example#B1` (list) to `smithy.example#B2` (list). This was determined backward "
                        + "compatible."));
    }

    @Test
    public void detectsNestedMapWithUnchangedValueTarget() {
        Model modelA = Model.assembler()
                .addImport(getClass().getResource("changed-member-target/nested-map-unchanged-member-a.smithy"))
                .assemble()
                .unwrap();
        Model modelB = Model.assembler()
                .addImport(getClass().getResource("changed-member-target/nested-map-unchanged-member-b.smithy"))
                .assemble()
                .unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "ChangedMemberTarget").size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, Severity.ERROR).size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, "ChangedMemberTarget").get(0).getMessage(),
                equalTo("The shape targeted by the member `smithy.example#A$member` changed from "
                        + "`smithy.example#B1` (map) to `smithy.example#B2` (map). This was determined "
                        + "backward compatible."));
    }

    @Test
    public void detectsCompatibleTraitsAddedToNestedListMembers() {
        Model modelA = Model.assembler()
                .addImport(getClass()
                        .getResource("changed-member-target/nested-list-added-compatible-member-trait-a.smithy"))
                .assemble()
                .unwrap();
        Model modelB = Model.assembler()
                .addImport(getClass()
                        .getResource("changed-member-target/nested-list-added-compatible-member-trait-b.smithy"))
                .assemble()
                .unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "ChangedMemberTarget").size(), equalTo(1));
        ValidationEvent event = TestHelper.findEvents(events, "ChangedMemberTarget").get(0);
        assertThat(event.getSeverity(), equalTo(Severity.WARNING));
        assertThat(event.getMessage(),
                equalTo(String.format("The shape targeted by the member `smithy.example#A$member` changed from "
                        + "`smithy.example#B1` (list) to `smithy.example#B2` (list). This was determined backward "
                        + "compatible. This will result in the following effective differences:%n%n"
                        + "- [WARNING] Added trait `smithy.api#pattern` with value `^[a-z]+$`; The @pattern trait "
                        + "should only be added if the string already had adhered to the pattern.")));
    }

    @Test
    public void detectsIncompatibleTraitsAddedToNestedListMembers() {
        Model modelA = Model.assembler()
                .addImport(getClass()
                        .getResource("changed-member-target/nested-list-added-incompatible-member-trait-a.smithy"))
                .assemble()
                .unwrap();
        Model modelB = Model.assembler()
                .addImport(getClass()
                        .getResource("changed-member-target/nested-list-added-incompatible-member-trait-b.smithy"))
                .assemble()
                .unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "ChangedMemberTarget").size(), equalTo(1));
        ValidationEvent event = TestHelper.findEvents(events, "ChangedMemberTarget").get(0);
        assertThat(event.getSeverity(), equalTo(Severity.ERROR));
        assertThat(event.getMessage(),
                equalTo(String.format("The shape targeted by the member `smithy.example#A$member` changed from "
                        + "`smithy.example#B1` (list) to `smithy.example#B2` (list). This will result in the following "
                        + "effective differences:%n%n"
                        + "- [ERROR] Added trait `smithy.example#noAddingTrait`")));
    }

    @Test
    public void detectsNestedListMemberChangedToCompatibleTarget() {
        Model modelA = Model.assembler()
                .addImport(
                        getClass().getResource("changed-member-target/nested-list-compatible-changed-member-a.smithy"))
                .assemble()
                .unwrap();
        Model modelB = Model.assembler()
                .addImport(
                        getClass().getResource("changed-member-target/nested-list-compatible-changed-member-b.smithy"))
                .assemble()
                .unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "ChangedMemberTarget").size(), equalTo(1));
        ValidationEvent event = TestHelper.findEvents(events, "ChangedMemberTarget").get(0);
        assertThat(event.getSeverity(), equalTo(Severity.WARNING));
        assertThat(event.getMessage(),
                equalTo(String.format("The shape targeted by the member `smithy.example#A$member` changed from "
                        + "`smithy.example#B1` (list) to `smithy.example#B2` (list). This was determined backward "
                        + "compatible. This will result in the following effective differences:%n%n"
                        + "- [WARNING] The shape targeted by the member `smithy.example#B1$member` changed from "
                        + "`smithy.example#MyString` (string) to `smithy.example#MyString2` (string). This was "
                        + "determined backward compatible.")));
    }

    @Test
    public void detectsNestedListMemberChangedToIncompatibleTarget() {
        Model modelA = Model.assembler()
                .addImport(
                        getClass()
                                .getResource("changed-member-target/nested-list-incompatible-changed-member-a.smithy"))
                .assemble()
                .unwrap();
        Model modelB = Model.assembler()
                .addImport(
                        getClass()
                                .getResource("changed-member-target/nested-list-incompatible-changed-member-b.smithy"))
                .assemble()
                .unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "ChangedMemberTarget").size(), equalTo(1));
        ValidationEvent event = TestHelper.findEvents(events, "ChangedMemberTarget").get(0);
        assertThat(event.getSeverity(), equalTo(Severity.ERROR));
        assertThat(event.getMessage(),
                equalTo(String.format("The shape targeted by the member `smithy.example#A$member` changed from "
                        + "`smithy.example#B1` (list) to `smithy.example#B2` (list). This will result in the following "
                        + "effective differences:%n%n"
                        + "- [ERROR] The shape targeted by the member `smithy.example#B1$member` changed from "
                        + "`smithy.example#MyString` (string) to `smithy.example#MyInteger` (integer). The type of the "
                        + "targeted shape changed from string to integer.")));
    }

    @Test
    public void detectsCompatibleTraitsAddedToNestedMapKey() {
        Model modelA = Model.assembler()
                .addImport(
                        getClass().getResource("changed-member-target/nested-map-key-added-compatible-trait-a.smithy"))
                .assemble()
                .unwrap();
        Model modelB = Model.assembler()
                .addImport(
                        getClass().getResource("changed-member-target/nested-map-key-added-compatible-trait-b.smithy"))
                .assemble()
                .unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "ChangedMemberTarget").size(), equalTo(1));
        ValidationEvent event = TestHelper.findEvents(events, "ChangedMemberTarget").get(0);
        assertThat(event.getSeverity(), equalTo(Severity.WARNING));
        assertThat(event.getMessage(),
                equalTo(String.format("The shape targeted by the member `smithy.example#A$member` changed from "
                        + "`smithy.example#B1` (map) to `smithy.example#B2` (map). This was determined backward "
                        + "compatible. This will result in the following effective differences:%n%n"
                        + "- [WARNING] Added trait `smithy.api#pattern` with value `^[a-z]+$`; The @pattern trait "
                        + "should only be added if the string already had adhered to the pattern.")));
    }

    @Test
    public void detectsIncompatibleTraitsAddedToNestedMapKey() {
        Model modelA = Model.assembler()
                .addImport(
                        getClass()
                                .getResource("changed-member-target/nested-map-key-added-incompatible-trait-a.smithy"))
                .assemble()
                .unwrap();
        Model modelB = Model.assembler()
                .addImport(
                        getClass()
                                .getResource("changed-member-target/nested-map-key-added-incompatible-trait-b.smithy"))
                .assemble()
                .unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "ChangedMemberTarget").size(), equalTo(1));
        ValidationEvent event = TestHelper.findEvents(events, "ChangedMemberTarget").get(0);
        assertThat(event.getSeverity(), equalTo(Severity.ERROR));
        assertThat(event.getMessage(),
                equalTo(String.format("The shape targeted by the member `smithy.example#A$member` changed from "
                        + "`smithy.example#B1` (map) to `smithy.example#B2` (map). This will result in the following "
                        + "effective differences:%n%n"
                        + "- [ERROR] Added trait `smithy.example#noAddingTrait`")));
    }

    @Test
    public void detectsNestedMapKeyChangedToCompatibleTarget() {
        Model modelA = Model.assembler()
                .addImport(getClass()
                        .getResource("changed-member-target/nested-map-key-compatible-changed-target-a.smithy"))
                .assemble()
                .unwrap();
        Model modelB = Model.assembler()
                .addImport(getClass()
                        .getResource("changed-member-target/nested-map-key-compatible-changed-target-b.smithy"))
                .assemble()
                .unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "ChangedMemberTarget").size(), equalTo(1));
        ValidationEvent event = TestHelper.findEvents(events, "ChangedMemberTarget").get(0);
        assertThat(event.getSeverity(), equalTo(Severity.WARNING));
        assertThat(event.getMessage(),
                equalTo(String.format("The shape targeted by the member `smithy.example#A$member` changed from "
                        + "`smithy.example#B1` (map) to `smithy.example#B2` (map). This was determined backward "
                        + "compatible. This will result in the following effective differences:%n%n"
                        + "- [WARNING] The shape targeted by the member `smithy.example#B1$key` changed from "
                        + "`smithy.example#MyString` (string) to `smithy.example#MyString2` (string). This was "
                        + "determined backward compatible.")));
    }

    @Test
    public void detectsNestedMapKeyChangedToIncompatibleTarget() {
        Model modelA = Model.assembler()
                .addImport(getClass()
                        .getResource("changed-member-target/nested-map-key-incompatible-changed-target-a.smithy"))
                .assemble()
                .unwrap();
        Model modelB = Model.assembler()
                .addImport(getClass()
                        .getResource("changed-member-target/nested-map-key-incompatible-changed-target-b.smithy"))
                .assemble()
                .unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "ChangedMemberTarget").size(), equalTo(1));
        ValidationEvent event = TestHelper.findEvents(events, "ChangedMemberTarget").get(0);
        assertThat(event.getSeverity(), equalTo(Severity.ERROR));
        assertThat(event.getMessage(),
                equalTo(String.format("The shape targeted by the member `smithy.example#A$member` changed from "
                        + "`smithy.example#B1` (map) to `smithy.example#B2` (map). This will result in the following "
                        + "effective differences:%n%n"
                        + "- [ERROR] The shape targeted by the member `smithy.example#B1$key` changed from "
                        + "`smithy.example#MyString` (string) to `smithy.example#MyEnum` (enum). The type of the "
                        + "targeted shape changed from string to enum.")));
    }

    @Test
    public void detectsCompatibleTraitsAddedToNestedMapValue() {
        Model modelA = Model.assembler()
                .addImport(getClass()
                        .getResource("changed-member-target/nested-map-value-added-compatible-trait-a.smithy"))
                .assemble()
                .unwrap();
        Model modelB = Model.assembler()
                .addImport(getClass()
                        .getResource("changed-member-target/nested-map-value-added-compatible-trait-b.smithy"))
                .assemble()
                .unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "ChangedMemberTarget").size(), equalTo(1));
        ValidationEvent event = TestHelper.findEvents(events, "ChangedMemberTarget").get(0);
        assertThat(event.getSeverity(), equalTo(Severity.WARNING));
        assertThat(event.getMessage(),
                equalTo(String.format("The shape targeted by the member `smithy.example#A$member` changed from "
                        + "`smithy.example#B1` (map) to `smithy.example#B2` (map). This was determined backward "
                        + "compatible. This will result in the following effective differences:%n%n"
                        + "- [WARNING] Added trait `smithy.api#pattern` with value `^[a-z]+$`; The @pattern trait "
                        + "should only be added if the string already had adhered to the pattern.")));
    }

    @Test
    public void detectsIncompatibleTraitsAddedToNestedMapValue() {
        Model modelA = Model.assembler()
                .addImport(getClass()
                        .getResource("changed-member-target/nested-map-value-added-incompatible-trait-a.smithy"))
                .assemble()
                .unwrap();
        Model modelB = Model.assembler()
                .addImport(getClass()
                        .getResource("changed-member-target/nested-map-value-added-incompatible-trait-b.smithy"))
                .assemble()
                .unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "ChangedMemberTarget").size(), equalTo(1));
        ValidationEvent event = TestHelper.findEvents(events, "ChangedMemberTarget").get(0);
        assertThat(event.getSeverity(), equalTo(Severity.ERROR));
        assertThat(event.getMessage(),
                equalTo(String.format("The shape targeted by the member `smithy.example#A$member` changed from "
                        + "`smithy.example#B1` (map) to `smithy.example#B2` (map). This will result in the following "
                        + "effective differences:%n%n"
                        + "- [ERROR] Added trait `smithy.example#noAddingTrait`")));
    }

    @Test
    public void detectsNestedMapValueChangedToCompatibleTarget() {
        Model modelA = Model.assembler()
                .addImport(getClass()
                        .getResource("changed-member-target/nested-map-value-compatible-changed-target-a.smithy"))
                .assemble()
                .unwrap();
        Model modelB = Model.assembler()
                .addImport(getClass()
                        .getResource("changed-member-target/nested-map-value-compatible-changed-target-b.smithy"))
                .assemble()
                .unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "ChangedMemberTarget").size(), equalTo(1));
        ValidationEvent event = TestHelper.findEvents(events, "ChangedMemberTarget").get(0);
        assertThat(event.getSeverity(), equalTo(Severity.WARNING));
        assertThat(event.getMessage(),
                equalTo(String.format("The shape targeted by the member `smithy.example#A$member` changed from "
                        + "`smithy.example#B1` (map) to `smithy.example#B2` (map). This was determined backward "
                        + "compatible. This will result in the following effective differences:%n%n"
                        + "- [WARNING] The shape targeted by the member `smithy.example#B1$value` changed from "
                        + "`smithy.example#MyString` (string) to `smithy.example#MyString2` (string). This was "
                        + "determined backward compatible.")));
    }

    @Test
    public void detectsNestedMapValueChangedToIncompatibleTarget() {
        Model modelA = Model.assembler()
                .addImport(getClass()
                        .getResource("changed-member-target/nested-map-value-incompatible-changed-target-a.smithy"))
                .assemble()
                .unwrap();
        Model modelB = Model.assembler()
                .addImport(getClass()
                        .getResource("changed-member-target/nested-map-value-incompatible-changed-target-b.smithy"))
                .assemble()
                .unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "ChangedMemberTarget").size(), equalTo(1));
        ValidationEvent event = TestHelper.findEvents(events, "ChangedMemberTarget").get(0);
        assertThat(event.getSeverity(), equalTo(Severity.ERROR));
        assertThat(event.getMessage(),
                equalTo(String.format("The shape targeted by the member `smithy.example#A$member` changed from "
                        + "`smithy.example#B1` (map) to `smithy.example#B2` (map). This will result in the following "
                        + "effective differences:%n%n"
                        + "- [ERROR] The shape targeted by the member `smithy.example#B1$value` changed from "
                        + "`smithy.example#MyString` (string) to `smithy.example#MyInteger` (integer). The type of the "
                        + "targeted shape changed from string to integer.")));
    }

    @Test
    public void handlesDeeplyNestedDiffs() {
        Model modelA = Model.assembler()
                .addImport(getClass()
                        .getResource("changed-member-target/double-nesting-a.smithy"))
                .assemble()
                .unwrap();
        Model modelB = Model.assembler()
                .addImport(getClass()
                        .getResource("changed-member-target/double-nesting-b.smithy"))
                .assemble()
                .unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "ChangedMemberTarget").size(), equalTo(1));
        ValidationEvent event = TestHelper.findEvents(events, "ChangedMemberTarget").get(0);
        assertThat(event.getSeverity(), equalTo(Severity.WARNING));
        assertThat(event.getMessage(),
                equalTo(String.format("The shape targeted by the member `smithy.example#A$member` changed from "
                        + "`smithy.example#B` (list) to `smithy.example#NewB` (list). This was determined backward "
                        + "compatible. This will result in the following effective differences:%n%n" +
                        "- [WARNING] The shape targeted by the member `smithy.example#B$member` changed from "
                        + "`smithy.example#C` (list) to `smithy.example#NewC` (list). This was determined backward "
                        + "compatible. This will result in the following effective differences:%n"
                        + "  %n"
                        + "  - [WARNING] Added trait `smithy.api#pattern` with value `foo:.*`; The @pattern trait "
                        + "should only be added if the string already had adhered to the pattern.")));
    }
}
