/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.diff.evaluators;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.stringContainsInOrder;

import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.diff.ModelDiff;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.EnumShape;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.traits.EnumDefinition;
import software.amazon.smithy.model.traits.EnumTrait;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;

public class ChangedEnumTraitTest {
    @Test
    public void detectsAppendedEnums() {
        SourceLocation source = new SourceLocation("main.smithy", 1, 2);
        StringShape s1 = StringShape.builder()
                .id("foo.baz#Baz")
                .addTrait(EnumTrait.builder()
                        .addEnum(EnumDefinition.builder().value("foo").build())
                        .build())
                .build();
        StringShape s2 = StringShape.builder()
                .id("foo.baz#Baz")
                .addTrait(EnumTrait.builder()
                        .addEnum(EnumDefinition.builder().value("foo").build())
                        .addEnum(EnumDefinition.builder().value("baz").build())
                        .addEnum(EnumDefinition.builder().value("bar").build())
                        .sourceLocation(source)
                        .build())
                .build();
        Model modelA = Model.assembler().addShape(s1).assemble().unwrap();
        Model modelB = Model.assembler().addShape(s2).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "ChangedEnumTrait").size(), equalTo(2));
        assertThat(TestHelper.findEvents(events, "ChangedEnumTrait.Appended").size(), equalTo(2));
        assertThat(TestHelper.findEvents(events, "ChangedEnumTrait.Appended.1").size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, "ChangedEnumTrait.Appended.2").size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, "ChangedEnumTrait.Appended")
                .stream()
                .allMatch(e -> e.getSeverity() == Severity.NOTE), equalTo(true));
        assertThat(TestHelper.findEvents(events, "ChangedEnumTrait.Appended")
                .stream()
                .allMatch(e -> source.equals(e.getSourceLocation())), equalTo(true));
    }

    @Test
    public void detectsAppendedEnumsEnumTraitNoNameToEnumShape() {
        SourceLocation source = new SourceLocation("main.smithy", 1, 2);
        StringShape s1 = StringShape.builder()
                .id("foo.baz#Baz")
                .addTrait(EnumTrait.builder()
                        .addEnum(EnumDefinition.builder()
                                .value("foo")
                                .build())
                        .build())
                .build();
        EnumShape s2 = EnumShape.builder()
                .id("foo.baz#Baz")
                .addMember("foo", "foo")
                .addMember("baz", "baz")
                .source(source)
                .build();
        Model modelA = Model.assembler().addShape(s1).assemble().unwrap();
        Model modelB = Model.assembler().addShape(s2).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "ChangedEnumTrait").size(), equalTo(2));
        assertThat(TestHelper.findEvents(events, "ChangedEnumTrait.NameChanged.0").size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, "ChangedEnumTrait.NameChanged.0").get(0).getSeverity(),
                equalTo(Severity.ERROR));
        assertThat(TestHelper.findEvents(events, "ChangedEnumTrait.NameChanged.0").get(0).getSourceLocation(),
                equalTo(source));
        assertThat(TestHelper.findEvents(events, "ChangedEnumTrait.Appended.1").get(0).getSeverity(),
                equalTo(Severity.NOTE));
        assertThat(TestHelper.findEvents(events, "ChangedEnumTrait.Appended.1").get(0).getSourceLocation(),
                equalTo(source));
    }

    @Test
    public void detectsAppendedEnumsEnumTraitWithNameToEnumShape() {
        SourceLocation source = new SourceLocation("main.smithy", 1, 2);
        StringShape s1 = StringShape.builder()
                .id("foo.baz#Baz")
                .addTrait(EnumTrait.builder()
                        .addEnum(EnumDefinition.builder()
                                .name("foo")
                                .value("foo")
                                .build())
                        .sourceLocation(source)
                        .build())
                .build();
        EnumShape s2 = EnumShape.builder()
                .id("foo.baz#Baz")
                .addMember("foo", "foo")
                .addMember("baz", "baz")
                .source(source)
                .build();
        Model modelA = Model.assembler().addShape(s1).assemble().unwrap();
        Model modelB = Model.assembler().addShape(s2).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "ChangedEnumTrait.Appended.1").size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, "ChangedEnumTrait").get(0).getSourceLocation(),
                equalTo(source));
        assertThat(TestHelper.findEvents(events, "ChangedEnumTrait.Appended")
                .stream()
                .allMatch(e -> e.getSeverity() == Severity.NOTE), equalTo(true));
    }

    @Test
    public void detectsRemovedEnums() {
        SourceLocation source = new SourceLocation("main.smithy", 1, 2);
        StringShape s1 = StringShape.builder()
                .id("foo.baz#Baz")
                .addTrait(EnumTrait.builder()
                        .addEnum(EnumDefinition.builder().value("foo").build())
                        .addEnum(EnumDefinition.builder().value("baz").build())
                        .addEnum(EnumDefinition.builder().value("bat").build())
                        .sourceLocation(source)
                        .build())
                .build();
        StringShape s2 = StringShape.builder()
                .id("foo.baz#Baz")
                .addTrait(EnumTrait.builder()
                        .addEnum(EnumDefinition.builder().value("foo").build())
                        .build())
                .build();
        Model modelA = Model.assembler().addShape(s1).assemble().unwrap();
        Model modelB = Model.assembler().addShape(s2).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "ChangedEnumTrait.Removed.1").size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, "ChangedEnumTrait.Removed.1").get(0).getSourceLocation(),
                equalTo(source));
        assertThat(TestHelper.findEvents(events, "ChangedEnumTrait.Removed.2").size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, "ChangedEnumTrait.Removed.2").get(0).getSourceLocation(),
                equalTo(source));
        assertThat(TestHelper.findEvents(events, Severity.ERROR).size(), equalTo(2));
    }

    @Test
    public void detectsRemovedEnumsEnumTraitNoNameToEnumShape() {
        SourceLocation beforeSource = new SourceLocation("before.smithy", 1, 2);
        SourceLocation afterSource = new SourceLocation("after.smithy", 1, 2);
        StringShape s1 = StringShape.builder()
                .id("foo.baz#Baz")
                .addTrait(EnumTrait.builder()
                        .addEnum(EnumDefinition.builder()
                                .value("foo")
                                .build())
                        .addEnum(EnumDefinition.builder()
                                .value("baz")
                                .build())
                        .sourceLocation(beforeSource)
                        .build())
                .build();
        EnumShape s2 = EnumShape.builder()
                .id("foo.baz#Baz")
                .addMember("foo", "foo")
                .source(afterSource)
                .build();
        Model modelA = Model.assembler().addShape(s1).assemble().unwrap();
        Model modelB = Model.assembler().addShape(s2).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "ChangedEnumTrait.Removed.1").size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, "ChangedEnumTrait.Removed.1").get(0).getSourceLocation(),
                equalTo(beforeSource));
        assertThat(TestHelper.findEvents(events, "ChangedEnumTrait.NameChanged.0").size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, "ChangedEnumTrait.NameChanged.0").get(0).getSourceLocation(),
                equalTo(afterSource));
        assertThat(TestHelper.findEvents(events, "ChangedEnumTrait")
                .stream()
                .allMatch(e -> e.getSeverity() == Severity.ERROR), equalTo(true));
    }

    @Test
    public void detectsRemovedEnumsEnumTraitWithNameToEnumShape() {
        SourceLocation source = new SourceLocation("main.smithy", 1, 2);
        StringShape s1 = StringShape.builder()
                .id("foo.baz#Baz")
                .addTrait(EnumTrait.builder()
                        .addEnum(EnumDefinition.builder()
                                .name("foo")
                                .value("foo")
                                .build())
                        .addEnum(EnumDefinition.builder()
                                .name("baz")
                                .value("baz")
                                .build())
                        .sourceLocation(source)
                        .build())
                .build();
        EnumShape s2 = EnumShape.builder()
                .id("foo.baz#Baz")
                .addMember("foo", "foo")
                .build();
        Model modelA = Model.assembler().addShape(s1).assemble().unwrap();
        Model modelB = Model.assembler().addShape(s2).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "ChangedEnumTrait.Removed.1").size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, "ChangedEnumTrait.Removed.1").get(0).getSourceLocation(),
                equalTo(source));
        assertThat(TestHelper.findEvents(events, "ChangedEnumTrait.Removed.1")
                .stream()
                .allMatch(e -> e.getSeverity() == Severity.ERROR), equalTo(true));
    }

    @Test
    public void detectsRenamedEnums() {
        SourceLocation source = new SourceLocation("main.smithy", 1, 2);
        StringShape s1 = StringShape.builder()
                .id("foo.baz#Baz")
                .addTrait(EnumTrait.builder()
                        .addEnum(EnumDefinition.builder().value("foo").name("OLD1").build())
                        .addEnum(EnumDefinition.builder().value("baz").name("OLD2").build())
                        .build())
                .build();
        StringShape s2 = StringShape.builder()
                .id("foo.baz#Baz")
                .addTrait(EnumTrait.builder()
                        .addEnum(EnumDefinition.builder().value("foo").name("NEW1").build())
                        .addEnum(EnumDefinition.builder().value("baz").name("NEW2").build())
                        .build())
                .source(source)
                .build();
        Model modelA = Model.assembler().addShape(s1).assemble().unwrap();
        Model modelB = Model.assembler().addShape(s2).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "ChangedEnumTrait.NameChanged.0").size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, "ChangedEnumTrait.NameChanged.0").get(0).getSourceLocation(),
                equalTo(source));
        assertThat(TestHelper.findEvents(events, "ChangedEnumTrait.NameChanged.1").size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, "ChangedEnumTrait.NameChanged.1").get(0).getSourceLocation(),
                equalTo(source));
        assertThat(TestHelper.findEvents(events, Severity.ERROR).size(), equalTo(2));
    }

    @Test
    public void detectsRenamedEnumsEnumTraitNoNameToEnumShape() {
        SourceLocation source = new SourceLocation("main.smithy", 1, 2);
        StringShape s1 = StringShape.builder()
                .id("foo.baz#Baz")
                .addTrait(EnumTrait.builder()
                        .addEnum(EnumDefinition.builder()
                                .value("foo")
                                .build())
                        .build())
                .build();
        EnumShape s2 = EnumShape.builder()
                .id("foo.baz#Baz")
                .addMember("NEW", "foo")
                .source(source)
                .build();
        Model modelA = Model.assembler().addShape(s1).assemble().unwrap();
        Model modelB = Model.assembler().addShape(s2).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "ChangedEnumTrait.NameChanged.0").size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, "ChangedEnumTrait.NameChanged.0").get(0).getSourceLocation(),
                equalTo(source));
        assertThat(TestHelper.findEvents(events, "ChangedEnumTrait.NameChanged.0").get(0).getSeverity(),
                equalTo(Severity.ERROR));
    }

    @Test
    public void detectsRenamedEnumsEnumTraitWithNameToEnumShape() {
        SourceLocation source = new SourceLocation("main.smithy", 1, 2);
        StringShape s1 = StringShape.builder()
                .id("foo.baz#Baz")
                .addTrait(EnumTrait.builder()
                        .addEnum(EnumDefinition.builder()
                                .name("OLD")
                                .value("foo")
                                .build())
                        .build())
                .build();
        EnumShape s2 = EnumShape.builder()
                .id("foo.baz#Baz")
                .addMember("NEW", "foo")
                .source(source)
                .build();
        Model modelA = Model.assembler().addShape(s1).assemble().unwrap();
        Model modelB = Model.assembler().addShape(s2).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "ChangedEnumTrait.NameChanged.0").size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, "ChangedEnumTrait.NameChanged.0").get(0).getSourceLocation(),
                equalTo(source));
        assertThat(TestHelper.findEvents(events, "ChangedEnumTrait.NameChanged.0").get(0).getSeverity(),
                equalTo(Severity.ERROR));
    }

    @Test
    public void detectsInsertedEnums() {
        SourceLocation source = new SourceLocation("main.smithy", 1, 2);
        StringShape s1 = StringShape.builder()
                .id("foo.baz#Baz")
                .addTrait(EnumTrait.builder()
                        .addEnum(EnumDefinition.builder().value("foo").build())
                        .sourceLocation(source)
                        .build())
                .build();
        StringShape s2 = StringShape.builder()
                .id("foo.baz#Baz")
                .addTrait(EnumTrait.builder()
                        .addEnum(EnumDefinition.builder().value("baz").build())
                        .addEnum(EnumDefinition.builder().value("bat").build())
                        .addEnum(EnumDefinition.builder().value("foo").build())
                        .build())
                .build();
        Model modelA = Model.assembler().addShape(s1).assemble().unwrap();
        Model modelB = Model.assembler().addShape(s2).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "ChangedEnumTrait.OrderChanged.0").size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, "ChangedEnumTrait.OrderChanged.0").get(0).getSourceLocation(),
                equalTo(source));
        assertThat(TestHelper.findEvents(events, "ChangedEnumTrait.OrderChanged.1").size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, "ChangedEnumTrait.OrderChanged.1").get(0).getSourceLocation(),
                equalTo(source));
        assertThat(TestHelper.findEvents(events, Severity.ERROR).size(), equalTo(2));
    }

    @Test
    public void detectsInsertedEnumsBeforeAppendedEnums() {
        SourceLocation source = new SourceLocation("main.smithy", 1, 2);
        StringShape s1 = StringShape.builder()
                .id("foo.baz#Baz")
                .addTrait(EnumTrait.builder()
                        .addEnum(EnumDefinition.builder().value("foo").build())
                        .sourceLocation(source)
                        .build())
                .build();
        StringShape s2 = StringShape.builder()
                .id("foo.baz#Baz")
                .addTrait(EnumTrait.builder()
                        .addEnum(EnumDefinition.builder().value("baz").build())
                        .addEnum(EnumDefinition.builder().value("bat").build())
                        .addEnum(EnumDefinition.builder().value("foo").build())
                        .addEnum(EnumDefinition.builder().value("bar").build())
                        .build())
                .build();
        Model modelA = Model.assembler().addShape(s1).assemble().unwrap();
        Model modelB = Model.assembler().addShape(s2).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "ChangedEnumTrait.OrderChanged.0").size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, "ChangedEnumTrait.OrderChanged.0")
                .get(0)
                .getSourceLocation(), equalTo(source));
        assertThat(TestHelper.findEvents(events, "ChangedEnumTrait.OrderChanged.1").size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, "ChangedEnumTrait.OrderChanged.1")
                .get(0)
                .getSourceLocation(), equalTo(source));
        assertThat(TestHelper.findEvents(events, Severity.ERROR).size(), equalTo(2));
    }

    @Test
    public void detectsInsertedEnumsEnumTraitNoNameToEnumShape() {
        SourceLocation source = new SourceLocation("main.smithy", 1, 2);
        StringShape s1 = StringShape.builder()
                .id("foo.baz#Baz")
                .addTrait(EnumTrait.builder()
                        .addEnum(EnumDefinition.builder()
                                .value("foo")
                                .build())
                        .sourceLocation(source)
                        .build())
                .build();
        EnumShape s2 = EnumShape.builder()
                .id("foo.baz#Baz")
                .addMember("baz", "baz")
                .addMember("foo", "foo")
                .build();
        Model modelA = Model.assembler().addShape(s1).assemble().unwrap();
        Model modelB = Model.assembler().addShape(s2).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "ChangedEnumTrait").size(), equalTo(2));
        assertThat(TestHelper.findEvents(events, "ChangedEnumTrait.OrderChanged.0").size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, "ChangedEnumTrait.OrderChanged.0").get(0).getSourceLocation(),
                equalTo(source));
        assertThat(TestHelper.findEvents(events, "ChangedEnumTrait")
                .stream()
                .allMatch(e -> e.getSeverity() == Severity.ERROR), equalTo(true));
    }

    @Test
    public void detectsInsertedEnumsEnumTraitWithNameToEnumShape() {
        SourceLocation source = new SourceLocation("main.smithy", 1, 2);
        StringShape s1 = StringShape.builder()
                .id("foo.baz#Baz")
                .addTrait(EnumTrait.builder()
                        .addEnum(EnumDefinition.builder()
                                .name("foo")
                                .value("foo")
                                .build())
                        .sourceLocation(source)
                        .build())
                .build();
        EnumShape s2 = EnumShape.builder()
                .id("foo.baz#Baz")
                .addMember("baz", "baz")
                .addMember("foo", "foo")
                .build();
        Model modelA = Model.assembler().addShape(s1).assemble().unwrap();
        Model modelB = Model.assembler().addShape(s2).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "ChangedEnumTrait.OrderChanged.0").size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, "ChangedEnumTrait.OrderChanged.0").get(0).getSourceLocation(),
                equalTo(source));
        assertThat(TestHelper.findEvents(events, "ChangedEnumTrait.OrderChanged.0")
                .stream()
                .allMatch(e -> e.getSeverity() == Severity.ERROR), equalTo(true));
    }

    @Test
    public void detectsAppendedEnumsAfterRemovedEnums() {
        SourceLocation beforeSource = new SourceLocation("before.smithy", 1, 2);
        SourceLocation afterSource = new SourceLocation("after.smithy", 1, 2);
        StringShape s1 = StringShape.builder()
                .id("foo.baz#Baz")
                .addTrait(EnumTrait.builder()
                        .addEnum(EnumDefinition.builder().value("old1").build())
                        .addEnum(EnumDefinition.builder().value("old2").build())
                        .addEnum(EnumDefinition.builder().value("old3").build())
                        .sourceLocation(beforeSource)
                        .build())
                .build();
        StringShape s2 = StringShape.builder()
                .id("foo.baz#Baz")
                .addTrait(EnumTrait.builder()
                        .addEnum(EnumDefinition.builder().value("old1").build())
                        .addEnum(EnumDefinition.builder().value("old3").build())
                        .addEnum(EnumDefinition.builder().value("new1").build())
                        .sourceLocation(afterSource)
                        .build())
                .build();
        Model modelA = Model.assembler().addShape(s1).assemble().unwrap();
        Model modelB = Model.assembler().addShape(s2).assemble().unwrap();
        List<ValidationEvent> allEvents = ModelDiff.compare(modelA, modelB);

        List<ValidationEvent> changeEvents = TestHelper.findEvents(allEvents, "ChangedEnumTrait");
        assertThat(changeEvents.size(), equalTo(2));

        assertThat(TestHelper.findEvents(changeEvents, "ChangedEnumTrait.Removed.1").size(), equalTo(1));
        ValidationEvent removedEvent = TestHelper.findEvents(changeEvents, "ChangedEnumTrait.Removed.1").get(0);
        assertThat(removedEvent.getSeverity(), equalTo(Severity.ERROR));
        assertThat(removedEvent.getMessage(), stringContainsInOrder("Enum value `old2` was removed"));
        assertThat(removedEvent.getSourceLocation(), equalTo(beforeSource));

        assertThat(TestHelper.findEvents(changeEvents, "ChangedEnumTrait.Appended.2").size(), equalTo(1));
        ValidationEvent appendedEvent = TestHelper.findEvents(changeEvents, "ChangedEnumTrait.Appended.2").get(0);
        assertThat(appendedEvent.getSeverity(), equalTo(Severity.NOTE));
        assertThat(appendedEvent.getMessage(), stringContainsInOrder("Enum value `new1` was appended"));
        assertThat(appendedEvent.getSourceLocation(), equalTo(afterSource));
    }

    @Test
    public void detectsAppendedEnumsAfterRemovedEnumsEnumTraitNoNameToEnumShape() {
        SourceLocation beforeSource = new SourceLocation("before.smithy", 1, 2);
        SourceLocation afterSource = new SourceLocation("after.smithy", 1, 2);
        StringShape s1 = StringShape.builder()
                .id("foo.baz#Baz")
                .addTrait(EnumTrait.builder()
                        .addEnum(EnumDefinition.builder()
                                .value("old1")
                                .build())
                        .addEnum(EnumDefinition.builder()
                                .value("old2")
                                .build())
                        .addEnum(EnumDefinition.builder()
                                .value("old3")
                                .build())
                        .sourceLocation(beforeSource)
                        .build())
                .build();
        EnumShape s2 = EnumShape.builder()
                .id("foo.baz#Baz")
                .addMember("old1", "old1")
                .addMember("old3", "old3")
                .addMember("new1", "new1")
                .source(afterSource)
                .build();
        Model modelA = Model.assembler().addShape(s1).assemble().unwrap();
        Model modelB = Model.assembler().addShape(s2).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "ChangedEnumTrait").size(), equalTo(4));
        assertThat(TestHelper.findEvents(events, "ChangedEnumTrait.NameChanged.0").size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, "ChangedEnumTrait.NameChanged.0").get(0).getSourceLocation(),
                equalTo(afterSource));
        assertThat(TestHelper.findEvents(events, "ChangedEnumTrait.Removed.1").size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, "ChangedEnumTrait.Removed.1").get(0).getSourceLocation(),
                equalTo(beforeSource));
        assertThat(TestHelper.findEvents(events, "ChangedEnumTrait.NameChanged.2").size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, "ChangedEnumTrait.NameChanged.2").get(0).getSourceLocation(),
                equalTo(afterSource));
        assertThat(TestHelper.findEvents(events, "ChangedEnumTrait")
                .subList(0, 3)
                .stream()
                .allMatch(e -> e.getSeverity() == Severity.ERROR), equalTo(true));
        assertThat(TestHelper.findEvents(events, "ChangedEnumTrait.Appended.2")
                .stream()
                .allMatch(e -> e.getSeverity() == Severity.NOTE), equalTo(true));
    }

    @Test
    public void detectsAppendedEnumsAfterRemovedEnumsEnumTraitWithNameToEnumShape() {
        SourceLocation source = new SourceLocation("main.smithy", 1, 2);
        StringShape s1 = StringShape.builder()
                .id("foo.baz#Baz")
                .addTrait(EnumTrait.builder()
                        .addEnum(EnumDefinition.builder()
                                .name("old1")
                                .value("old1")
                                .build())
                        .addEnum(EnumDefinition.builder()
                                .name("old2")
                                .value("old2")
                                .build())
                        .addEnum(EnumDefinition.builder()
                                .name("old3")
                                .value("old3")
                                .build())
                        .sourceLocation(source)
                        .build())
                .build();
        EnumShape s2 = EnumShape.builder()
                .id("foo.baz#Baz")
                .addMember("old1", "old1")
                .addMember("old3", "old3")
                .addMember("new1", "new1")
                .build();
        Model modelA = Model.assembler().addShape(s1).assemble().unwrap();
        Model modelB = Model.assembler().addShape(s2).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "ChangedEnumTrait").size(), equalTo(2));
        assertThat(TestHelper.findEvents(events, "ChangedEnumTrait.Removed.1").size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, "ChangedEnumTrait.Removed.1").get(0).getSourceLocation(),
                equalTo(source));
        assertThat(TestHelper.findEvents(events, "ChangedEnumTrait.Removed.1")
                .stream()
                .allMatch(e -> e.getSeverity() == Severity.ERROR), equalTo(true));
        assertThat(TestHelper.findEvents(events, "ChangedEnumTrait.Appended.2")
                .stream()
                .allMatch(e -> e.getSeverity() == Severity.NOTE), equalTo(true));
    }
}
