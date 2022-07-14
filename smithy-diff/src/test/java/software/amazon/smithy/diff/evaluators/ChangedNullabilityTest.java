package software.amazon.smithy.diff.evaluators;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.diff.ModelDiff;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.DefaultTrait;
import software.amazon.smithy.model.traits.InputTrait;
import software.amazon.smithy.model.traits.ClientOptionalTrait;
import software.amazon.smithy.model.traits.RequiredTrait;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;

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
                           .filter(event -> event.getId().equals("ChangedNullability"))
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
                           .filter(event -> event.getId().equals("ChangedNullability"))
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
                           .filter(event -> event.getId().equals("ChangedNullability"))
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
                           .filter(event -> event.getId().equals("ChangedNullability"))
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
                           .filter(event -> event.getId().equals("ChangedNullability"))
                           .filter(event -> event.getMessage().contains("The @nullable trait was added to a "
                                                                        + "@required member"))
                           .count(), equalTo(1L));
    }

    @Test
    public void detectsAdditionOfInputTrait() {
        MemberShape member = MemberShape.builder()
                .id("foo.baz#Baz$bam")
                .target("foo.baz#String")
                .addTrait(new RequiredTrait())
                .build();
        StructureShape shapeA1 = StructureShape.builder().id("foo.baz#Baz").addMember(member).build();
        StructureShape shapeA2 = shapeA1.toBuilder().addTrait(new InputTrait()).build();
        StringShape target = StringShape.builder().id("foo.baz#String").build();
        Model modelA = Model.assembler().addShapes(shapeA1, member, target).assemble().unwrap();
        Model modelB = Model.assembler().addShapes(shapeA2, member, target).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(events.stream()
                           .filter(event -> event.getSeverity() == Severity.ERROR)
                           .filter(event -> event.getId().equals("ChangedNullability"))
                           .filter(event -> event.getMessage().contains("The @input trait was added to"))
                           .count(), equalTo(1L));
    }

    @Test
    public void detectsRemovalOfInputTrait() {
        MemberShape member = MemberShape.builder()
                .id("foo.baz#Baz$bam")
                .target("foo.baz#String")
                .addTrait(new RequiredTrait())
                .build();
        StructureShape shapeA1 = StructureShape.builder()
                .id("foo.baz#Baz").addMember(member)
                .addTrait(new InputTrait())
                .build();
        StructureShape shapeA2 = shapeA1.toBuilder().removeTrait(InputTrait.ID).build();
        StringShape target = StringShape.builder().id("foo.baz#String").build();
        Model modelA = Model.assembler().addShapes(shapeA1, member, target).assemble().unwrap();
        Model modelB = Model.assembler().addShapes(shapeA2, member, target).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(events.stream()
                           .filter(event -> event.getSeverity() == Severity.ERROR)
                           .filter(event -> event.getId().equals("ChangedNullability"))
                           .filter(event -> event.getMessage().contains("The @input trait was removed from"))
                           .count(), equalTo(1L));
    }
}
