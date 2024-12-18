/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.diff.evaluators;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;

import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.diff.ModelDiff;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;

public class ChangedMemberOrderTest {
    @Test
    public void detectsOrderChanges() {
        Shape shape1 = StringShape.builder().id("foo.baz#String").build();
        MemberShape member1 = MemberShape.builder().id("foo.baz#Struct$member1").target(shape1).build();
        MemberShape member2 = MemberShape.builder().id("foo.baz#Struct$member2").target(shape1).build();
        StructureShape oldStruct = StructureShape.builder()
                .id("foo.baz#Struct")
                .addMember(member1)
                .addMember(member2)
                .build();

        StructureShape newStruct = StructureShape.builder()
                .id("foo.baz#Struct")
                .addMember(member2)
                .addMember(member1)
                .build();

        Model modelA = Model.assembler().addShapes(shape1, oldStruct).assemble().unwrap();
        Model modelB = Model.assembler().addShapes(shape1, newStruct).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "ChangedMemberOrder").size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, oldStruct.getId()).size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, Severity.DANGER).size(), equalTo(1));
    }

    @Test
    public void detectsOrderInsertionChanges() {
        Shape shape1 = StringShape.builder().id("foo.baz#String").build();
        MemberShape member1 = MemberShape.builder().id("foo.baz#Struct$member1").target(shape1).build();
        MemberShape member2 = MemberShape.builder().id("foo.baz#Struct$member2").target(shape1).build();
        StructureShape oldStruct = StructureShape.builder()
                .id("foo.baz#Struct")
                .addMember(member1)
                .addMember(member2)
                .build();

        MemberShape member3 = MemberShape.builder().id("foo.baz#Struct$member3").target(shape1).build();
        StructureShape newStruct = StructureShape.builder()
                .id("foo.baz#Struct")
                .addMember(member1)
                .addMember(member3)
                .addMember(member2)
                .build();

        Model modelA = Model.assembler().addShapes(shape1, oldStruct).assemble().unwrap();
        Model modelB = Model.assembler().addShapes(shape1, newStruct).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "ChangedMemberOrder").size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, oldStruct.getId()).size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, Severity.DANGER).size(), equalTo(1));
    }

    @Test
    public void detectsCompatibleChanges() {
        Shape shape1 = StringShape.builder().id("foo.baz#String").build();
        MemberShape member1 = MemberShape.builder().id("foo.baz#Struct$member1").target(shape1).build();
        MemberShape member2 = MemberShape.builder().id("foo.baz#Struct$member2").target(shape1).build();
        StructureShape oldStruct = StructureShape.builder()
                .id("foo.baz#Struct")
                .addMember(member1)
                .addMember(member2)
                .build();

        MemberShape member3 = MemberShape.builder().id("foo.baz#Struct$member3").target(shape1).build();
        StructureShape newStruct = StructureShape.builder()
                .id("foo.baz#Struct")
                .addMember(member1)
                .addMember(member2)
                .addMember(member3)
                .build();

        Model modelA = Model.assembler().addShapes(shape1, oldStruct).assemble().unwrap();
        Model modelB = Model.assembler().addShapes(shape1, newStruct).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "ChangedMemberOrder"), empty());
    }

    // Ignore the fact that a member was removed.
    @Test
    public void ignoresOtherBreakingChanges() {
        Shape shape1 = StringShape.builder().id("foo.baz#String").build();
        MemberShape member1 = MemberShape.builder().id("foo.baz#Struct$member1").target(shape1).build();
        MemberShape member2 = MemberShape.builder().id("foo.baz#Struct$member2").target(shape1).build();
        StructureShape oldStruct = StructureShape.builder()
                .id("foo.baz#Struct")
                .addMember(member1)
                .addMember(member2)
                .build();

        StructureShape newStruct = StructureShape.builder()
                .id("foo.baz#Struct")
                .addMember(member1)
                .build();

        Model modelA = Model.assembler().addShapes(shape1, oldStruct).assemble().unwrap();
        Model modelB = Model.assembler().addShapes(shape1, newStruct).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "ChangedMemberOrder"), empty());
    }
}
