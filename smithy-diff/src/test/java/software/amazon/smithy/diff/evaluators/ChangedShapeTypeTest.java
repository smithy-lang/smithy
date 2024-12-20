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
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.EnumShape;
import software.amazon.smithy.model.shapes.ModelSerializer;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.TimestampShape;
import software.amazon.smithy.model.traits.EnumDefinition;
import software.amazon.smithy.model.traits.EnumTrait;
import software.amazon.smithy.model.validation.ValidationEvent;

public class ChangedShapeTypeTest {
    @Test
    public void detectsTypeChanges() {
        Shape shapeA1 = StringShape.builder().id("foo.baz#Baz").build();
        Shape shapeA2 = TimestampShape.builder().id("foo.baz#Baz").build();
        Shape shapeB1 = StringShape.builder().id("foo.baz#Bam").build();
        Model modelA = Model.assembler().addShapes(shapeA1, shapeB1).assemble().unwrap();
        Model modelB = Model.assembler().addShapes(shapeA2, shapeB1).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "ChangedShapeType").size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, shapeA1.getId()).size(), equalTo(1));
    }

    @Test
    public void ignoresExpectedSetToListMigration() {
        String rawModel = "$version: \"1.0\"\nnamespace smithy.example\nset Foo { member: String }\n";
        Model oldModel = Model.assembler()
                .addUnparsedModel("example.smithy", rawModel)
                .assemble()
                .unwrap();
        Node serialized = ModelSerializer.builder().build().serialize(oldModel);
        Model newModel = Model.assembler()
                .addDocumentNode(serialized)
                .assemble()
                .unwrap();

        List<ValidationEvent> events = ModelDiff.compare(oldModel, newModel);

        assertThat(TestHelper.findEvents(events, "ChangedShapeType"), empty());
    }

    @Test
    public void ignoresExpectedEnumTraitToEnumMigration() {
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

        assertThat(TestHelper.findEvents(events, "ChangedShapeType"), empty());
    }

    @Test
    public void ignoresEnumTraitToEnumTraitMigration() {
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
                        .build())
                .build();
        Model modelA = Model.assembler().addShapes(stringWithEnumTraitA).assemble().unwrap();
        Model modelB = Model.assembler().addShapes(stringWithEnumTraitB).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "ChangedShapeType"), empty());
    }

    @Test
    public void ignoresEnumToEnumMigration() {
        Shape enumShapeA = EnumShape.builder()
                .id("foo.baz#Baz")
                .addMember("FOO", "FOO")
                .build();
        Shape enumShapeB = EnumShape.builder()
                .id("foo.baz#Baz")
                .addMember("FOO", "FOO")
                .build();
        Model modelA = Model.assembler().addShapes(enumShapeA).assemble().unwrap();
        Model modelB = Model.assembler().addShapes(enumShapeB).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "ChangedShapeType"), empty());
    }

    @Test
    public void doesNotIgnoreEnumTraitToEnumMigration() {
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

        assertThat(TestHelper.findEvents(events, "ChangedShapeType").size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, stringWithEnumTrait.toShapeId()).size(), equalTo(1));
    }
}
