package software.amazon.smithy.diff.evaluators.configurable;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.diff.ModelDiff;
import software.amazon.smithy.diff.evaluators.TestHelper;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;

public class ConfigurableEvaluatorTest {
    @Test
    public void configurableEvaluatorsEmitEvents() {
        Model oldModel = Model.assembler()
                .addImport(getClass().getResource("configurable-a.smithy"))
                .assemble()
                .unwrap();
        Model newModel = Model.assembler()
                .addImport(getClass().getResource("configurable-b.smithy"))
                .assemble()
                .unwrap();

        ModelDiff.Result result = ModelDiff.builder()
                .oldModel(oldModel)
                .newModel(newModel)
                .compare();
        List<ValidationEvent> events = result.getDiffEvents();

        assertThat(TestHelper.findEvents(events, "AddedInternalOperation").size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, "AddedInternalOperation").get(0).getSeverity(),
                equalTo(Severity.NOTE));
        assertThat(TestHelper.findEvents(events, "AddedInternalOperation").get(0).getShapeId(),
                equalTo(Optional.of(ShapeId.from("smithy.example#InternalOperation"))));

        assertThat(TestHelper.findEvents(events, "AddedOnlyPrimitiveNumbersAndBools").size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, "AddedOnlyPrimitiveNumbersAndBools").get(0).getShapeId(),
                equalTo(Optional.empty()));

        assertThat(TestHelper.findEvents(events, "AddedMemberWithoutClientOptional").size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, "AddedMemberWithoutClientOptional").get(0).getSeverity(),
                equalTo(Severity.WARNING));

        assertThat(TestHelper.findEvents(events, "RemovedRootShape").size(), equalTo(2));
    }
}
