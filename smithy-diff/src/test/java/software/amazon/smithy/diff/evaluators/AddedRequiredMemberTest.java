package software.amazon.smithy.diff.evaluators;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.diff.ModelDiff;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.DefaultTrait;
import software.amazon.smithy.model.traits.RequiredTrait;
import software.amazon.smithy.model.validation.Severity;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class AddedRequiredMemberTest {
    @Test
    public void addingRequiredTraitWithoutDefaultIsAnError() {
        StringShape s = StringShape.builder().id("smithy.example#Str").build();
        StructureShape a = StructureShape.builder().id("smithy.example#A")
                .build();
        SourceLocation source = new SourceLocation("main.smithy", 1, 2);
        MemberShape member = MemberShape.builder()
                .id(a.getId().withMember("foo"))
                .target(s.getId())
                .addTrait(new RequiredTrait())
                .source(source)
                .build();
        StructureShape b = StructureShape.builder().id("smithy.example#A")
                .addMember(member)
                .build();
        Model model1 = Model.builder().addShapes(s, a).build();
        Model model2 = Model.builder().addShapes(s, b).build();
        ModelDiff.Result result = ModelDiff.builder().oldModel(model1).newModel(model2).compare();

        assertThat(TestHelper.findEvents(result.getDiffEvents(), Severity.ERROR).size(), equalTo(1));
        assertThat(TestHelper.findEvents(result.getDiffEvents(), "AddedRequiredMember").size(), equalTo(1));
        assertThat(TestHelper.findEvents(result.getDiffEvents(), "AddedRequiredMember").get(0).getShapeId().get().toString(),
                equalTo("smithy.example#A$foo"));
        assertThat(TestHelper.findEvents(result.getDiffEvents(), "AddedRequiredMember").get(0).getMessage(),
                equalTo("Adding a new member with the `required` trait " +
                        "but not the `default` trait is backwards-incompatible."));
        assertThat(TestHelper.findEvents(result.getDiffEvents(), "AddedRequiredMember").get(0).getSourceLocation(),
                equalTo(source));
    }

    @Test
    public void addingRequiredTraitWithDefaultIsOk() {
        StringShape s = StringShape.builder().id("smithy.example#Str").build();
        StructureShape a = StructureShape.builder().id("smithy.example#A")
                .build();
        StructureShape b = StructureShape.builder().id("smithy.example#A")
                .addMember("foo", s.getId(), b2 -> {
                    b2.addTrait(new RequiredTrait());
                    b2.addTrait(new DefaultTrait(new StringNode("default", SourceLocation.NONE)));
                })
                .build();
        Model model1 = Model.builder().addShapes(s, a).build();
        Model model2 = Model.builder().addShapes(s, b).build();
        ModelDiff.Result result = ModelDiff.builder().oldModel(model1).newModel(model2).compare();

        assertThat(TestHelper.findEvents(result.getDiffEvents(), "AddedRequiredMember").size(), equalTo(0));
    }

    @Test
    public void addingRequiredTraitToExistingMember() {
        StringShape s = StringShape.builder().id("smithy.example#Str").build();
        StructureShape a = StructureShape.builder().id("smithy.example#A")
                .addMember("foo", s.getId())
                .build();
        StructureShape b = StructureShape.builder().id("smithy.example#A")
                .addMember("foo", s.getId(),
                        b2 -> b2.addTrait(new RequiredTrait()))
                .build();
        Model model1 = Model.builder().addShapes(s, a).build();
        Model model2 = Model.builder().addShapes(s, b).build();
        ModelDiff.Result result = ModelDiff.builder().oldModel(model1).newModel(model2).compare();

        assertThat(TestHelper.findEvents(result.getDiffEvents(), "AddedRequiredMember").size(), equalTo(0));
    }

    @Test
    public void addingNewStructureWithRequiredMemberIsOk() {
        StringShape s = StringShape.builder().id("smithy.example#Str").build();
        StructureShape b = StructureShape.builder().id("smithy.example#A")
                .addMember("foo", s.getId(), b2 -> b2.addTrait(new RequiredTrait()))
                .build();
        Model model1 = Model.builder().addShapes(s).build();
        Model model2 = Model.builder().addShapes(s, b).build();
        ModelDiff.Result result = ModelDiff.builder().oldModel(model1).newModel(model2).compare();

        assertThat(TestHelper.findEvents(result.getDiffEvents(), "AddedRequiredMember").size(), equalTo(0));
    }
}
