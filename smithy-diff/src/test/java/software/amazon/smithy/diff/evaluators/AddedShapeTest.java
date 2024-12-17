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
import software.amazon.smithy.model.shapes.EnumShape;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.traits.EnumDefinition;
import software.amazon.smithy.model.traits.EnumTrait;
import software.amazon.smithy.model.validation.ValidationEvent;

public class AddedShapeTest {
    @Test
    public void detectsShapeAdded() {
        Shape shapeA = StringShape.builder().id("foo.baz#Baz").build();
        Shape shapeB = StringShape.builder().id("foo.baz#Bam").build();
        Model modelA = Model.assembler().addShapes(shapeA).assemble().unwrap();
        Model modelB = Model.assembler().addShapes(shapeA, shapeB).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "AddedShape").size(), equalTo(1));
    }

    @Test
    public void doesNotEmitForMembersOfAddedContainerShapes() {
        Shape string = StringShape.builder().id("foo.baz#Baz").build();
        MemberShape member = MemberShape.builder().id("foo.baz#Bam$member").target(string).build();
        ListShape list = ListShape.builder().id("foo.baz#Bam").addMember(member).build();
        Model modelA = Model.assembler().addShapes(string).assemble().unwrap();
        Model modelB = Model.assembler().addShapes(list, string).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "AddedShape").size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, list.getId()).size(), equalTo(1));
    }

    @Test
    public void doesNotEmitForMembersOfConvertedEnumShape() {
        Shape stringWithEnumTrait = StringShape.builder()
                .id("foo.baz#Baz")
                .addTrait(EnumTrait.builder()
                        .addEnum(EnumDefinition.builder()
                                .name("FOO")
                                .value("FOO")
                                .build())
                        .build())
                .build();
        Shape enumShape = EnumShape.builder()
                .id("foo.baz#Baz")
                .addMember("FOO", "FOO")
                .build();
        Model modelA = Model.assembler().addShapes(stringWithEnumTrait).assemble().unwrap();
        Model modelB = Model.assembler().addShapes(enumShape).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "AddedShape").size(), equalTo(0));
    }

    @Test
    public void doesNotEmitForEnumShapeToEnumTrait() {
        Shape enumShape = EnumShape.builder()
                .id("foo.baz#Baz")
                .addMember("FOO", "FOO")
                .build();
        Shape stringWithEnumTrait = StringShape.builder()
                .id("foo.baz#Baz")
                .addTrait(EnumTrait.builder()
                        .addEnum(EnumDefinition.builder()
                                .name("FOO")
                                .value("FOO")
                                .build())
                        .build())
                .build();
        Model modelA = Model.assembler().addShapes(enumShape).assemble().unwrap();
        Model modelB = Model.assembler().addShapes(stringWithEnumTrait).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "AddedShape").size(), equalTo(0));
    }

    @Test
    public void doesNotEmitForEnumTraitToEnumTraitAddedEnum() {
        Shape stringWithEnumTraitA = StringShape.builder()
                .id("foo.baz#Baz")
                .addTrait(EnumTrait.builder()
                        .addEnum(EnumDefinition.builder()
                                .name("FOO")
                                .value("FOO")
                                .build())
                        .build())
                .build();
        Shape stringWithEnumTraitB = StringShape.builder()
                .id("foo.baz#Baz")
                .addTrait(EnumTrait.builder()
                        .addEnum(EnumDefinition.builder()
                                .name("FOO")
                                .value("FOO")
                                .build())
                        .addEnum(EnumDefinition.builder()
                                .name("BAR")
                                .value("BAR")
                                .build())
                        .build())
                .build();
        Model modelA = Model.assembler().addShapes(stringWithEnumTraitA).assemble().unwrap();
        Model modelB = Model.assembler().addShapes(stringWithEnumTraitB).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "AddedShape").size(), equalTo(0));
    }

    @Test
    public void doesEmitForEnumShapeToEnumShapeAddedMember() {
        Shape enumShapeA = EnumShape.builder()
                .id("foo.baz#Baz")
                .addMember("FOO", "FOO")
                .build();
        Shape enumShapeB = EnumShape.builder()
                .id("foo.baz#Baz")
                .addMember("FOO", "FOO")
                .addMember("BAR", "BAR")
                .build();
        Model modelA = Model.assembler().addShapes(enumShapeA).assemble().unwrap();
        Model modelB = Model.assembler().addShapes(enumShapeB).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "AddedShape").size(), equalTo(1));
        assertThat(enumShapeB.getMember("BAR").isPresent(), equalTo(true));
        assertThat(TestHelper.findEvents(events, enumShapeB.getMember("BAR").get().toShapeId()).size(),
                equalTo(1));
    }
}
