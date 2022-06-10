/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.diff.evaluators;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.diff.ModelDiff;
import software.amazon.smithy.model.Model;
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
        Shape shape1 = StringShape.builder().id("foo.baz#String").build();
        MemberShape member1 = MemberShape.builder().id("foo.baz#List$member").target(shape1.getId()).build();
        ListShape list1 = ListShape.builder().id("foo.baz#List").member(member1).build();
        Shape shape2 = TimestampShape.builder().id("foo.baz#Timestamp").build();
        MemberShape member2 = MemberShape.builder().id("foo.baz#List$member").target(shape2.getId()).build();
        ListShape list2 = ListShape.builder().id("foo.baz#List").member(member2).build();
        Model modelA = Model.assembler().addShapes(shape1, shape2, member1, list1).assemble().unwrap();
        Model modelB = Model.assembler().addShapes(shape1, shape2, member2, list2).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "ChangedMemberTarget").size(), equalTo(1));
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
                           + "was found on the target, so the name of the targeted shape matters for codegen. "
                           + "The targeted shape no longer has the following traits: [smithy.api#enum]."));
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
        assertThat(TestHelper.findEvents(events, Severity.ERROR).size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, "ChangedMemberTarget").get(0).getMessage(),
                   equalTo("The shape targeted by the member `foo.baz#List$member` changed from `foo.baz#String1` "
                           + "(string) to `foo.baz#String2` (string). The newly targeted shape now has the "
                           + "following additional traits: [smithy.api#sensitive]."));
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
        assertThat(TestHelper.findEvents(events, Severity.ERROR).size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, "ChangedMemberTarget").get(0).getMessage(),
                   equalTo("The shape targeted by the member `foo.baz#List$member` changed from `foo.baz#String1` "
                           + "(string) to `foo.baz#String2` (string). The newly targeted shape has traits that "
                           + "differ from the previous shape: [smithy.api#documentation]."));
    }

    @Test
    public void detectsAcceptableListMemberChangesInNestedTargets() {
        Model modelA = Model.assembler()
                .addImport(getClass().getResource("changed-member-target-valid-nested1-a.smithy"))
                .assemble()
                .unwrap();
        Model modelB = Model.assembler()
                .addImport(getClass().getResource("changed-member-target-valid-nested1-b.smithy"))
                .assemble()
                .unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "ChangedMemberTarget").size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, Severity.WARNING).size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, "ChangedMemberTarget").get(0).getMessage(),
                   equalTo("The shape targeted by the member `smithy.example#A$member` changed from "
                           + "`smithy.example#B1` (list) to `smithy.example#B2` (list). This was determined "
                           + "backward compatible."));
    }

    @Test
    public void detectsInvalidListMemberChangesInNestedTargets() {
        Model modelA = Model.assembler()
                .addImport(getClass().getResource("changed-member-target-invalid-nested1-a.smithy"))
                .assemble()
                .unwrap();
        Model modelB = Model.assembler()
                .addImport(getClass().getResource("changed-member-target-invalid-nested1-b.smithy"))
                .assemble()
                .unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "ChangedMemberTarget").size(), equalTo(1));
        ValidationEvent event = TestHelper.findEvents(events, "ChangedMemberTarget").get(0);
        assertThat(event.getSeverity(), equalTo(Severity.ERROR));
        assertThat(event.getMessage(),
                   equalTo("The shape targeted by the member `smithy.example#A$member` changed from "
                           + "`smithy.example#B1` (list) to `smithy.example#B2` (list). Both the old and new "
                           + "shapes are a list, but their members have differing traits. The newly targeted "
                           + "shape now has the following additional traits: [smithy.api#pattern]."));
    }

    @Test
    public void detectsInvalidListMemberTargetChange() {
        Model modelA = Model.assembler()
                .addImport(getClass().getResource("changed-member-target-invalid-nested2-a.smithy"))
                .assemble()
                .unwrap();
        Model modelB = Model.assembler()
                .addImport(getClass().getResource("changed-member-target-invalid-nested2-b.smithy"))
                .assemble()
                .unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "ChangedMemberTarget").size(), equalTo(1));
        ValidationEvent event = TestHelper.findEvents(events, "ChangedMemberTarget").get(0);
        assertThat(event.getSeverity(), equalTo(Severity.ERROR));
        assertThat(event.getMessage(),
                   equalTo("The shape targeted by the member `smithy.example#A$member` changed from "
                           + "`smithy.example#B1` (list) to `smithy.example#B2` (list). Both the old and new "
                           + "shapes are a list, but the old shape targeted `smithy.example#MyString` while "
                           + "the new shape targets `smithy.example#MyString2`."));
    }
}
