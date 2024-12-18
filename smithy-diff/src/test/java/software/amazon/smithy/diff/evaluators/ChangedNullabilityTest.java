/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.diff.evaluators;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.diff.ModelDiff;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ModelSerializer;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.model.traits.BoxTrait;
import software.amazon.smithy.model.traits.ClientOptionalTrait;
import software.amazon.smithy.model.traits.DefaultTrait;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.model.traits.InputTrait;
import software.amazon.smithy.model.traits.RequiredTrait;
import software.amazon.smithy.model.transform.ModelTransformer;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.ListUtils;

public class ChangedNullabilityTest {
    @Test
    public void replacingRequiredTraitWithDefaultIsOk() {
        StringShape s = StringShape.builder().id("smithy.example#Str").build();
        StructureShape a = StructureShape.builder()
                .id("smithy.example#A")
                .addMember("foo", s.getId(), b1 -> b1.addTrait(new RequiredTrait()))
                .build();
        StructureShape b = StructureShape.builder()
                .id("smithy.example#A")
                .addMember("foo", s.getId(), b2 -> b2.addTrait(new DefaultTrait(Node.from(""))))
                .build();
        Model model1 = Model.builder().addShapes(s, a).build();
        Model model2 = Model.builder().addShapes(s, b).build();
        ModelDiff.Result result = ModelDiff.builder().oldModel(model1).newModel(model2).compare();

        assertThat(result.getDiffEvents()
                .stream()
                .filter(event -> event.getId().equals("ChangedNullability"))
                .count(), equalTo(0L));
    }

    @Test
    public void addingDefaultWithRequiredTraitIsOk() {
        StringShape s = StringShape.builder().id("smithy.example#Str").build();
        StructureShape a = StructureShape.builder()
                .id("smithy.example#A")
                .addMember("foo", s.getId(), b1 -> b1.addTrait(new DefaultTrait(Node.from(""))))
                .build();
        StructureShape b = StructureShape.builder()
                .id("smithy.example#A")
                .addMember("foo", s.getId(), b2 -> {
                    b2.addTrait(new RequiredTrait());
                    b2.addTrait(new DefaultTrait(Node.from("")));
                })
                .build();
        Model model1 = Model.builder().addShapes(s, a).build();
        Model model2 = Model.builder().addShapes(s, b).build();
        ModelDiff.Result result = ModelDiff.builder().oldModel(model1).newModel(model2).compare();

        assertThat(result.getDiffEvents()
                .stream()
                .filter(event -> event.getId().equals("ChangedNullability"))
                .count(), equalTo(0L));
    }

    @Test
    public void detectsInvalidAdditionOfDefaultTrait() {
        SourceLocation source = new SourceLocation("a.smithy", 5, 6);
        StringShape s = StringShape.builder().id("smithy.example#Str").build();
        StructureShape a = StructureShape.builder()
                .id("smithy.example#A")
                .addMember("foo", s.getId(), builder -> builder.source(source))
                .source(source)
                .build();
        StructureShape b = StructureShape.builder()
                .id("smithy.example#A")
                .addMember("foo", s.getId(), builder -> builder.addTrait(new DefaultTrait(Node.from(""))))
                .build();
        Model model1 = Model.builder().addShapes(s, a).build();
        Model model2 = Model.builder().addShapes(s, b).build();
        ModelDiff.Result result = ModelDiff.builder().oldModel(model1).newModel(model2).compare();

        assertThat(result.isDiffBreaking(), is(true));
        assertThat(result.getDiffEvents()
                .stream()
                .filter(event -> event.getSeverity() == Severity.ERROR)
                .filter(event -> event.getId().equals("ChangedNullability.AddedDefaultTrait"))
                .filter(event -> event.getShapeId().get().equals(a.getAllMembers().get("foo").getId()))
                .filter(event -> event.getSourceLocation().equals(source))
                .filter(event -> event.getMessage()
                        .contains("The @default trait was added to a member that "
                                + "was not previously @required"))
                .count(), equalTo(1L));
    }

    @Test
    public void removingTheRequiredTraitOnInputStructureIsOk() {
        StringShape s = StringShape.builder().id("smithy.example#Str").build();
        StructureShape a = StructureShape.builder()
                .addTrait(new InputTrait())
                .id("smithy.example#A")
                .addMember("foo", s.getId(), b1 -> b1.addTrait(new RequiredTrait()))
                .build();
        StructureShape b = StructureShape.builder()
                .addTrait(new InputTrait())
                .id("smithy.example#A")
                .addMember("foo", s.getId())
                .build();
        Model model1 = Model.builder().addShapes(s, a).build();
        Model model2 = Model.builder().addShapes(s, b).build();
        ModelDiff.Result result = ModelDiff.builder().oldModel(model1).newModel(model2).compare();

        assertThat(result.getDiffEvents()
                .stream()
                .filter(event -> event.getId().equals("ChangedNullability.RemovedRequiredTrait.StructureOrUnion"))
                .count(), equalTo(0L));
    }

    @Test
    public void detectsInvalidRemovalOfRequired() {
        SourceLocation memberSource = new SourceLocation("a.smithy", 7, 7);
        StringShape s = StringShape.builder().id("smithy.example#Str").build();
        MemberShape requiredM = MemberShape.builder()
                .id("smithy.example#A$foo")
                .target(s)
                .addTrait(new RequiredTrait())
                .source(memberSource)
                .build();
        MemberShape m = MemberShape.builder().id("smithy.example#A$foo").target(s).source(memberSource).build();
        StructureShape a = StructureShape.builder()
                .id("smithy.example#A")
                .addMember(requiredM)
                .build();
        StructureShape b = StructureShape.builder()
                .id("smithy.example#A")
                .addMember(m)
                .build();
        Model model1 = Model.builder().addShapes(s, a, requiredM).build();
        Model model2 = Model.builder().addShapes(s, b, m).build();
        ModelDiff.Result result = ModelDiff.builder().oldModel(model1).newModel(model2).compare();

        assertThat(result.isDiffBreaking(), is(true));
        assertThat(result.getDiffEvents()
                .stream()
                .filter(event -> event.getSeverity() == Severity.ERROR)
                .filter(event -> event.getId().equals("ChangedNullability.RemovedRequiredTrait"))
                .filter(event -> event.getShapeId().get().equals(a.getAllMembers().get("foo").getId()))
                .filter(event -> event.getSourceLocation().equals(memberSource))
                .filter(event -> event.getMessage()
                        .contains("The @required trait was removed and not "
                                + "replaced with the @default trait"))
                .count(), equalTo(1L));
    }

    @Test
    public void detectAdditionOfRequiredTrait() {
        SourceLocation memberSource = new SourceLocation("a.smithy", 5, 6);
        MemberShape member1 = MemberShape.builder()
                .id("foo.baz#Baz$bam")
                .target("foo.baz#String")
                .source(memberSource)
                .build();
        MemberShape member2 = member1.toBuilder().addTrait(new RequiredTrait()).build();
        StructureShape shapeA1 = StructureShape.builder().id("foo.baz#Baz").addMember(member1).build();
        StructureShape shapeA2 = StructureShape.builder().id("foo.baz#Baz").addMember(member2).build();
        StringShape target = StringShape.builder().id("foo.baz#String").build();
        Model modelA = Model.assembler().addShapes(shapeA1, member1, target).assemble().unwrap();
        Model modelB = Model.assembler().addShapes(shapeA2, member2, target).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(events.stream()
                .filter(event -> event.getSeverity() == Severity.ERROR)
                .filter(event -> event.getId().equals("ChangedNullability.AddedRequiredTrait"))
                .filter(event -> event.getSourceLocation().equals(memberSource))
                .filter(event -> event.getMessage().contains("The @required trait was added to a member"))
                .count(), equalTo(1L));
    }

    @Test
    public void detectAdditionOfClientOptionalTrait() {
        SourceLocation memberSource = new SourceLocation("a.smithy", 5, 6);
        MemberShape member1 = MemberShape.builder()
                .id("foo.baz#Baz$bam")
                .target("foo.baz#String")
                .addTrait(new RequiredTrait())
                .source(memberSource)
                .build();
        MemberShape member2 = member1.toBuilder().addTrait(new ClientOptionalTrait()).build();
        StructureShape shapeA1 = StructureShape.builder().id("foo.baz#Baz").addMember(member1).build();
        StructureShape shapeA2 = StructureShape.builder().id("foo.baz#Baz").addMember(member2).build();
        StringShape target = StringShape.builder().id("foo.baz#String").build();
        Model modelA = Model.assembler().addShapes(shapeA1, member1, target).assemble().unwrap();
        Model modelB = Model.assembler().addShapes(shapeA2, member2, target).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(events.stream()
                .filter(event -> event.getSeverity() == Severity.ERROR)
                .filter(event -> event.getId().equals("ChangedNullability.AddedClientOptionalTrait"))
                .filter(event -> event.getSourceLocation().equals(memberSource))
                .filter(event -> event.getMessage()
                        .contains("The @clientOptional trait was added to a "
                                + "@required member"))
                .count(), equalTo(1L));
    }

    @Test
    public void detectsAdditionOfInputTrait() {
        SourceLocation structureSource = new SourceLocation("a.smithy", 5, 6);
        MemberShape member1 = MemberShape.builder()
                .id("foo.baz#Baz$bam")
                .target("foo.baz#String")
                .addTrait(new RequiredTrait())
                .build();
        MemberShape member2 = member1.toBuilder().addTrait(new DocumentationTrait("docs")).build();
        StructureShape shapeA1 = StructureShape.builder().id("foo.baz#Baz").addMember(member1).build();
        StructureShape shapeA2 = StructureShape.builder()
                .id("foo.baz#Baz")
                .addMember(member2)
                .addTrait(new InputTrait())
                .source(structureSource)
                .build();
        StringShape target = StringShape.builder().id("foo.baz#String").build();
        Model modelA = Model.assembler().addShapes(shapeA1, member1, target).assemble().unwrap();
        Model modelB = Model.assembler().addShapes(shapeA2, member2, target).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(events.stream()
                .filter(event -> event.getSeverity() == Severity.DANGER)
                .filter(event -> event.getId().equals("ChangedNullability.AddedInputTrait"))
                .filter(event -> event.getSourceLocation().equals(structureSource))
                .filter(event -> event.getMessage().contains("The @input trait was added to"))
                .count(), equalTo(1L));
        assertThat(events.stream()
                .filter(event -> event.getId().contains("ChangedNullability"))
                .filter(event -> event.getSourceLocation().equals(structureSource))
                .count(), equalTo(1L));
    }

    @Test
    public void detectsRemovalOfInputTrait() {
        SourceLocation structureSource = new SourceLocation("a.smithy", 5, 6);
        MemberShape member1 = MemberShape.builder()
                .id("foo.baz#Baz$bam")
                .target("foo.baz#String")
                .addTrait(new RequiredTrait())
                .build();
        MemberShape member2 = member1.toBuilder()
                .addTrait(new DocumentationTrait("docs"))
                .build();
        StructureShape shapeA1 = StructureShape.builder()
                .id("foo.baz#Baz")
                .addMember(member1)
                .addTrait(new InputTrait())
                .build();
        StructureShape shapeA2 = StructureShape.builder()
                .id("foo.baz#Baz")
                .addMember(member2)
                .source(structureSource)
                .build();
        StringShape target = StringShape.builder().id("foo.baz#String").build();
        Model modelA = Model.assembler().addShapes(shapeA1, member1, target).assemble().unwrap();
        Model modelB = Model.assembler().addShapes(shapeA2, member2, target).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(events.stream()
                .filter(event -> event.getSeverity() == Severity.ERROR)
                .filter(event -> event.getId().equals("ChangedNullability.RemovedInputTrait"))
                .filter(event -> event.getSourceLocation().equals(structureSource))
                .filter(event -> event.getMessage().contains("The @input trait was removed from"))
                .count(), equalTo(1L));
    }

    @Test
    public void doesNotEmitForBackwardCompatibleBoxTraitChanges() {
        Model old = Model.assembler()
                .addImport(getClass().getResource("box-added-to-member.smithy"))
                .assemble()
                .unwrap();

        ShapeId bazId = ShapeId.from("smithy.example#Example$baz");
        ShapeId bamId = ShapeId.from("smithy.example#Example$bam");
        Model newModel = ModelTransformer.create().mapShapes(old, shape -> {
            // Add the box trait to both shapes.
            if (shape.isMemberShape() && shape.getId().equals(bazId) || shape.getId().equals(bamId)) {
                MemberShape.Builder b = ((MemberShape) shape).toBuilder();
                b.addTrait(new BoxTrait());
                return b.build();
            }
            return shape;
        });

        // First, spot check that the transform worked and the models are different.
        assertThat(old.expectShape(bazId).hasTrait(BoxTrait.class), is(false));
        assertThat(newModel.expectShape(bazId).hasTrait(BoxTrait.class), is(true));
        assertThat(old.expectShape(bamId).hasTrait(BoxTrait.class), is(false));
        assertThat(newModel.expectShape(bamId).hasTrait(BoxTrait.class), is(true));

        List<ValidationEvent> events = ModelDiff.compare(old, newModel);

        // No events should have been emitted for the addition of the backward compatible box trait.
        assertThat(events, empty());
    }

    @Test
    public void doesNotEmitForBackwardCompatibleBoxTraitChangesFromRoundTripping() {
        Model old = Model.assembler()
                .addImport(getClass().getResource("box-added-to-member.smithy"))
                .assemble()
                .unwrap();

        Model newModel = Model.assembler()
                .addDocumentNode(ModelSerializer.builder().build().serialize(old))
                .assemble()
                .unwrap();

        List<ValidationEvent> events = ModelDiff.compare(old, newModel);

        // No events should have been emitted for the addition of the backward compatible box trait.
        assertThat(events, empty());
    }

    @Test
    public void roundTrippedV1ModelHasNoEvents() {
        String originalModel =
                "$version: \"1.0\"\n"
                        + "namespace smithy.example\n"
                        + "integer MyPrimitiveInteger\n"
                        + "@box\n"
                        + "integer MyBoxedInteger\n"
                        + "structure Foo {\n"
                        + "    a: MyPrimitiveInteger,\n"
                        + "    @box\n"
                        + "    b: MyPrimitiveInteger,\n"
                        + "    c: MyBoxedInteger,\n"
                        + "    @box\n"
                        + "    d: MyBoxedInteger,\n"
                        + "}\n";
        Model oldModel = Model.assembler().addUnparsedModel("test.smithy", originalModel).assemble().unwrap();

        // Round trip the v1 model and make sure there are no diff events.
        String unparsedNew = Node.prettyPrintJson(ModelSerializer.builder().build().serialize(oldModel));
        Model newModel = Model.assembler()
                .addUnparsedModel("test.json", unparsedNew)
                .assemble()
                .unwrap();

        List<ValidationEvent> events = ModelDiff.compare(oldModel, newModel);

        assertThat(events, empty());
    }

    @Test
    public void specialHandlingForRequiredStructureMembers() {
        SourceLocation memberSource = new SourceLocation("a.smithy", 8, 2);
        StructureShape structBaz = StructureShape.builder().id("smithy.example#Baz").build();
        MemberShape memberBaz = MemberShape.builder()
                .id("smithy.example#Foo$baz")
                .addTrait(new RequiredTrait())
                .target(structBaz)
                .source(memberSource)
                .build();
        StructureShape structFoo = StructureShape.builder().id("smithy.example#Foo").addMember(memberBaz).build();
        Model oldModel = Model.assembler().addShapes(structFoo, structBaz, memberBaz).assemble().unwrap();
        Model newModel = ModelTransformer.create()
                .replaceShapes(oldModel,
                        ListUtils.of(
                                Shape.shapeToBuilder(oldModel.expectShape(ShapeId.from("smithy.example#Foo$baz")))
                                        .removeTrait(RequiredTrait.ID)
                                        .build()));

        List<ValidationEvent> events = TestHelper.findEvents(
                ModelDiff.compare(oldModel, newModel),
                "ChangedNullability");

        assertThat(events, hasSize(1));
        assertThat(events.get(0).getSeverity(), is(Severity.WARNING));
        assertThat(events.get(0).getSourceLocation(), equalTo(memberSource));
    }

    @Test
    public void specialHandlingForRequiredUnionMembers() {
        SourceLocation memberSource = new SourceLocation("b.smithy", 3, 4);
        MemberShape memberA = MemberShape.builder().id("smithy.example#Baz$a").target("smithy.api#String").build();
        MemberShape memberB = MemberShape.builder().id("smithy.example#Baz$B").target("smithy.api#String").build();
        UnionShape union = UnionShape.builder().id("smithy.example#Baz").addMember(memberA).addMember(memberB).build();
        MemberShape memberBaz = MemberShape.builder()
                .id("smithy.example#Foo$baz")
                .addTrait(new RequiredTrait())
                .target(union)
                .source(memberSource)
                .build();
        StructureShape struct = StructureShape.builder().id("smithy.example#Foo").addMember(memberBaz).build();
        Model oldModel = Model.assembler().addShapes(union, struct, memberA, memberB, memberBaz).assemble().unwrap();
        Model newModel = ModelTransformer.create()
                .replaceShapes(oldModel,
                        ListUtils.of(
                                Shape.shapeToBuilder(oldModel.expectShape(ShapeId.from("smithy.example#Foo$baz")))
                                        .removeTrait(RequiredTrait.ID)
                                        .build()));

        List<ValidationEvent> events = TestHelper.findEvents(
                ModelDiff.compare(oldModel, newModel),
                "ChangedNullability");

        assertThat(events, hasSize(1));
        assertThat(events.get(0).getSeverity(), is(Severity.WARNING));
        assertThat(events.get(0).getSourceLocation(), equalTo(memberSource));
    }

    @Test
    public void doesNotWarnWhenExtraneousDefaultNullTraitRemoved() {
        String originalModel =
                "$version: \"2.0\"\n"
                        + "namespace smithy.example\n"
                        + "structure Foo {\n"
                        + "    @required\n"
                        + "    baz: Integer = null\n"
                        + "}\n";
        Model oldModel = Model.assembler().addUnparsedModel("foo.smithy", originalModel).assemble().unwrap();
        Model newModel = ModelTransformer.create()
                .replaceShapes(oldModel,
                        ListUtils.of(
                                Shape.shapeToBuilder(oldModel.expectShape(ShapeId.from("smithy.example#Foo$baz")))
                                        .removeTrait(DefaultTrait.ID)
                                        .build()));

        // The only emitted even should be a warning about the removal of the default trait, which can be ignored
        // given the effective nullability of the member is unchanged.
        List<ValidationEvent> events = ModelDiff.compare(oldModel, newModel);
        assertThat(TestHelper.findEvents(events, "ChangedNullability"), empty());
        assertThat(TestHelper.findEvents(events, "ChangedDefault"), empty());
        assertThat(TestHelper.findEvents(events, "ModifiedTrait"), not(empty()));
    }
}
