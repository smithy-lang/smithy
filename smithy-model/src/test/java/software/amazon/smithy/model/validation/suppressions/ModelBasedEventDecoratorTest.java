package software.amazon.smithy.model.validation.suppressions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidatedResult;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidationEventDecorator;
import software.amazon.smithy.model.validation.Validator;
import software.amazon.smithy.model.validation.ValidatorFactory;

public class ModelBasedEventDecoratorTest {
    @Test
    public void erroneousSuppressionsEmitEvents() {
        ValidatedResult<Model> result = Model.assembler()
                .addImport(getClass().getResource("bad-suppressions.smithy"))
                // Ensure that events found while loading the model are decorated too.
                .validatorFactory(testFactory(event -> event.toBuilder().hint("hi").build()))
                .assemble();

        assertThat(result.getValidationEvents(Severity.ERROR), not(empty()));
        assertThat(result.getValidationEvents(Severity.ERROR).get(0).getMessage(), containsString("member `id`"));
        assertThat(result.getValidationEvents(Severity.ERROR).get(0).getHint(), equalTo(Optional.of("hi")));
    }

    @Test
    public void erroneousOverridesEmitEvents() {
        ValidatedResult<Model> result = Model.assembler()
                .addImport(getClass().getResource("bad-severityOverrides.smithy"))
                // Ensure that events found while loading the model are decorated too.
                .validatorFactory(testFactory(event -> event.toBuilder().hint("hi").build()))
                .assemble();

        assertThat(result.getValidationEvents(Severity.ERROR), not(empty()));
        assertThat(result.getValidationEvents(Severity.ERROR).get(0).getMessage(), containsString("member `id`"));
        assertThat(result.getValidationEvents(Severity.ERROR).get(0).getHint(), equalTo(Optional.of("hi")));
    }

    @Test
    public void loadsSuppressionsAndOverrides() {
        ValidatedResult<Model> result = Model.assembler()
                .addImport(Model.class.getResource(
                        "errorfiles/validators/severityOverrides/suppressions-take-precedence.smithy"))
                .validatorFactory(testFactory(event -> event.toBuilder().hint("hi").build()))
                .assemble();

        assertThat(result.getValidationEvents(Severity.ERROR), empty());
        assertThat(result.getValidationEvents(), not(empty()));

        // Every event should have the applied hint.
        for (ValidationEvent event : result.getValidationEvents()) {
            assertThat(event.getHint(), equalTo(Optional.of("hi")));
        }
    }

    private static ValidatorFactory testFactory(Function<ValidationEvent, ValidationEvent> d) {
        return new ValidatorFactory() {
            @Override
            public List<Validator> loadBuiltinValidators() {
                return Collections.emptyList();
            }

            @Override
            public Optional<Validator> createValidator(String name, ObjectNode configuration) {
                return Optional.empty();
            }

            @Override
            public List<ValidationEventDecorator> loadDecorators() {
                return Collections.singletonList(new ValidationEventDecorator() {
                    @Override
                    public boolean canDecorate(ValidationEvent ev) {
                        return true;
                    }

                    @Override
                    public ValidationEvent decorate(ValidationEvent ev) {
                        return d.apply(ev);
                    }
                });
            }
        };
    }
}
