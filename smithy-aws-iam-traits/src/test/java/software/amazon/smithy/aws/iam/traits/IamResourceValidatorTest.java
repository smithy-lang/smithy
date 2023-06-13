package software.amazon.smithy.aws.iam.traits;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidatedResult;
import software.amazon.smithy.model.validation.ValidationEvent;

import java.util.Optional;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IamResourceValidatorTest {
    @Test
    public void detectsUnknownConditionKeys() {
        ValidatedResult<Model> result = Model.assembler()
            .addImport(getClass().getResource("invalid-iam-resources.smithy"))
            .discoverModels(getClass().getClassLoader())
            .assemble();

        assertTrue(result.isBroken());
        assertEquals(result.getValidationEvents().size(), 3);
        assertThat(result.getValidationEvents(Severity.DANGER).stream()
                .map(ValidationEvent::getId)
                .collect(Collectors.toSet()),
            contains("IamResourceTrait"));
        assertThat(result.getValidationEvents(Severity.DANGER).stream()
                .map(ValidationEvent::getShapeId)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet()),
            containsInAnyOrder(
                ShapeId.from("smithy.example#IncompatibleResourceName"),
                ShapeId.from("smithy.example#InvalidResource"),
                ShapeId.from("smithy.example#BadIamResourceName")
            ));
    }
}
