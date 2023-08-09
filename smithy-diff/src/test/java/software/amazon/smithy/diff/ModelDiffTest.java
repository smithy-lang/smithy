package software.amazon.smithy.diff;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.traits.SensitiveTrait;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidatedResult;
import software.amazon.smithy.model.validation.ValidationEvent;

public class ModelDiffTest {
    @Test
    public void providesValidationResult() {
        Model oldModel = Model.builder().build();
        Model newModel = Model.builder().build();
        List<ValidationEvent> oldEvents = Collections.singletonList(
                ValidationEvent.builder()
                        .id("x")
                        .severity(Severity.ERROR)
                        .message("Hello")
                        .build());
        List<ValidationEvent> newEvents = Collections.singletonList(
                ValidationEvent.builder()
                        .id("y")
                        .severity(Severity.ERROR)
                        .message("Hello")
                        .build());

        ValidatedResult<Model> oldResult = new ValidatedResult<>(oldModel, oldEvents);
        ValidatedResult<Model> newResult = new ValidatedResult<>(newModel, newEvents);

        ModelDiff.Result result = ModelDiff.builder()
                .oldModel(oldResult)
                .newModel(newResult)
                .compare();

        assertThat(result.getOldModelEvents(), equalTo(oldEvents));
        assertThat(result.getNewModelEvents(), equalTo(newEvents));
        assertThat(result.getDifferences().addedShapes().count(), is(0L));

        assertThat(result, equalTo(result));
        assertThat(result.hashCode(), equalTo(result.hashCode()));

        assertThat(result.determineResolvedEvents(), contains(oldEvents.get(0)));
        assertThat(result.determineIntroducedEvents(), contains(newEvents.get(0)));
    }

    @Test
    public void testsEquality() {
        Model oldModel = Model.builder()
                .putMetadataProperty("foo", Node.from("baz"))
                .addShape(StringShape.builder().id("smithy.example#Str").build())
                .build();
        Model newModel = Model.builder()
                .putMetadataProperty("foo", Node.from("bar"))
                .addShape(StringShape.builder().id("smithy.example#Str").addTrait(new SensitiveTrait()).build())
                .build();
        ModelDiff.Result result1 = ModelDiff.builder().oldModel(oldModel).newModel(newModel).compare();
        ModelDiff.Result result2 = ModelDiff.builder().oldModel(oldModel).newModel(newModel).compare();

        // Same instance equality.
        assertThat(result1, equalTo(result1));
        assertThat(result1.hashCode(), equalTo(result1.hashCode()));

        // .equals equality.
        assertThat(result1, equalTo(result2));
    }

    @Test
    public void findsBreakingChanges() {
        Model oldModel = Model.builder()
                .addShape(StringShape.builder().id("smithy.example#Str").build())
                .build();
        Model newModel = Model.builder().build();
        ModelDiff.Result result = ModelDiff.builder().oldModel(oldModel).newModel(newModel).compare();

        assertThat(result.isDiffBreaking(), is(true));
    }

    @Test
    public void detectsWhenNoBreakingChanges() {
        Model model = Model.builder()
                .addShape(StringShape.builder().id("smithy.example#Str").build())
                .build();
        ModelDiff.Result result = ModelDiff.builder().oldModel(model).newModel(model).compare();

        assertThat(result.isDiffBreaking(), is(false));
    }

    @Test
    public void appliesSuppressionsToDiff() {
        Model oldModel = Model.assembler()
                .addImport(getClass().getResource("suppressions-a.smithy"))
                .assemble()
                .unwrap();
        Model newModel = Model.assembler()
                .addImport(getClass().getResource("suppressions-b.smithy"))
                .assemble()
                .unwrap();

        ModelDiff.Result result = ModelDiff.builder()
                .oldModel(oldModel)
                .newModel(newModel)
                .compare();

        assertThat(result.isDiffBreaking(), is(false));

        boolean found = false;
        for (ValidationEvent event : result.getDiffEvents()) {
            if (event.getId().equals("ChangedMemberOrder")) {
                assertThat(event.getSeverity(), equalTo(Severity.SUPPRESSED));
                found = true;
            }
        }

        assertThat(found, is(true));
    }
}
