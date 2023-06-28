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
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ModelSerializer;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;
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

        assertThat(result.getDiffEvents().stream()
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

        assertThat(result.getDiffEvents().stream()
                           .filter(event -> event.getId().equals("ChangedNullability"))
                           .count(), equalTo(0L));
    }

    @Test
    public void detectsInvalidAdditionOfDefaultTrait() {
        StringShape s = StringShape.builder().id("smithy.example#Str").build();
        StructureShape a = StructureShape.builder()
                .id("smithy.example#A")
                .addMember("foo", s.getId())
                .build();
        StructureShape b = StructureShape.builder()
                .id("smithy.example#A")
                .addMember("foo", s.getId(), builder -> builder.addTrait(new DefaultTrait(Node.from(""))))
                .build();
        Model model1 = Model.builder().addShapes(s, a).build();
        Model model2 = Model.builder().addShapes(s, b).build();
        ModelDiff.Result result = ModelDiff.builder().oldModel(model1).newModel(model2).compare();

        assertThat(result.isDiffBreaking(), is(true));
        assertThat(result.getDiffEvents().stream()
                           .filter(event -> event.getSeverity() == Severity.ERROR)
                           .filter(event -> event.getId().equals("ChangedNullability.AddedDefaultTrait"))
                           .filter(event -> event.getShapeId().get().equals(a.getAllMembers().get("foo").getId()))
                           .filter(event -> event.getMessage().contains("The @default trait was added to a member that "
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

        assertThat(result.getDiffEvents().stream()
                           .filter(event -> event.getId().equals("ChangedNullability.RemovedRequiredTrait.StructureOrUnion"))
                           .count(), equalTo(0L));
    }

    @Test
    public void detectsInvalidRemovalOfRequired() {
        StringShape s = StringShape.builder().id("smithy.example#Str").build();
        StructureShape a = StructureShape.builder()
                .id("smithy.example#A")
                .addMember("foo", s.getId(), b1 -> b1.addTrait(new RequiredTrait()))
                .build();
        StructureShape b = StructureShape.builder()
                .id("smithy.example#A")
                .addMember("foo", s.getId())
                .build();
        Model model1 = Model.builder().addShapes(s, a).build();
        Model model2 = Model.builder().addShapes(s, b).build();
        ModelDiff.Result result = ModelDiff.builder().oldModel(model1).newModel(model2).compare();

        assertThat(result.isDiffBreaking(), is(true));
        assertThat(result.getDiffEvents().stream()
                           .filter(event -> event.getSeverity() == Severity.ERROR)
                           .filter(event -> event.getId().equals("ChangedNullability.RemovedRequiredTrait"))
                           .filter(event -> event.getShapeId().get().equals(a.getAllMembers().get("foo").getId()))
                           .filter(event -> event.getMessage().contains("The @required trait was removed and not "
                                                                        + "replaced with the @default trait"))
                           .count(), equalTo(1L));
    }

    @Test
    public void detectAdditionOfRequiredTrait() {
        MemberShape member1 = MemberShape.builder().id("foo.baz#Baz$bam").target("foo.baz#String").build();
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
                           .filter(event -> event.getMessage().contains("The @required trait was added to a member "
                                                                        + "that is not marked as @nullable"))
                           .count(), equalTo(1L));
    }

    @Test
    public void detectAdditionOfNullableTrait() {
        MemberShape member1 = MemberShape.builder()
                .id("foo.baz#Baz$bam")
                .target("foo.baz#String")
                .addTrait(new RequiredTrait())
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
                           .filter(event -> event.getId().equals("ChangedNullability.AddedNullableTrait"))
                           .filter(event -> event.getMessage().contains("The @nullable trait was added to a "
                                                                        + "@required member"))
                           .count(), equalTo(1L));
    }

    @Test
    public void detectsAdditionOfInputTrait() {
        MemberShape member1 = MemberShape.builder()
                .id("foo.baz#Baz$bam")
                .target("foo.baz#String")
                .addTrait(new RequiredTrait())
                .build();
        MemberShape member2 = member1.toBuilder().addTrait(new DocumentationTrait("docs")).build();
        StructureShape shapeA1 = StructureShape.builder().id("foo.baz#Baz").addMember(member1).build();
        StructureShape shapeA2 = StructureShape.builder().id("foo.baz#Baz").addMember(member2)
                                                         .addTrait(new InputTrait()).build();
        StringShape target = StringShape.builder().id("foo.baz#String").build();
        Model modelA = Model.assembler().addShapes(shapeA1, member1, target).assemble().unwrap();
        Model modelB = Model.assembler().addShapes(shapeA2, member2, target).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(events.stream()
                           .filter(event -> event.getSeverity() == Severity.DANGER)
                           .filter(event -> event.getId().equals("ChangedNullability.AddedInputTrait"))
                           .filter(event -> event.getMessage().contains("The @input trait was added to"))
                           .count(), equalTo(1L));
        assertThat(events.stream()
                            .filter(event -> event.getId().contains("ChangedNullability"))
                            .count(), equalTo(1L));
    }

    @Test
    public void detectsRemovalOfInputTrait() {
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
                .build();
        StringShape target = StringShape.builder().id("foo.baz#String").build();
        Model modelA = Model.assembler().addShapes(shapeA1, member1, target).assemble().unwrap();
        Model modelB = Model.assembler().addShapes(shapeA2, member2, target).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(events.stream()
                           .filter(event -> event.getSeverity() == Severity.ERROR)
                           .filter(event -> event.getId().equals("ChangedNullability.RemovedInputTrait"))
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
        String originalModel =
                "$version: \"2.0\"\n"
                + "namespace smithy.example\n"
                + "structure Baz {}\n"
                + "structure Foo {\n"
                + "    @required\n"
                + "    baz: Baz\n"
                + "}\n";
        Model oldModel = Model.assembler().addUnparsedModel("foo.smithy", originalModel).assemble().unwrap();
        Model newModel = ModelTransformer.create().replaceShapes(oldModel, ListUtils.of(
                Shape.shapeToBuilder(oldModel.expectShape(ShapeId.from("smithy.example#Foo$baz")))
                        .removeTrait(RequiredTrait.ID)
                        .build()));

        List<ValidationEvent> events = TestHelper.findEvents(
                ModelDiff.compare(oldModel, newModel), "ChangedNullability");

        assertThat(events, hasSize(1));
        assertThat(events.get(0).getSeverity(), is(Severity.WARNING));
    }

    @Test
    public void specialHandlingForRequiredUnionMembers() {
        String originalModel =
                "$version: \"2.0\"\n"
                + "namespace smithy.example\n"
                + "union Baz {\n"
                + "    a: String\n"
                + "    b: String\n"
                + "}\n"
                + "structure Foo {\n"
                + "    @required\n"
                + "    baz: Baz\n"
                + "}\n";
        Model oldModel = Model.assembler().addUnparsedModel("foo.smithy", originalModel).assemble().unwrap();
        Model newModel = ModelTransformer.create().replaceShapes(oldModel, ListUtils.of(
                Shape.shapeToBuilder(oldModel.expectShape(ShapeId.from("smithy.example#Foo$baz")))
                        .removeTrait(RequiredTrait.ID)
                        .build()));

        List<ValidationEvent> events = TestHelper.findEvents(
                ModelDiff.compare(oldModel, newModel), "ChangedNullability");

        assertThat(events, hasSize(1));
        assertThat(events.get(0).getSeverity(), is(Severity.WARNING));
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
        Model newModel = ModelTransformer.create().replaceShapes(oldModel, ListUtils.of(
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
