package software.amazon.smithy.model.loader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.Validator;
import software.amazon.smithy.model.validation.ValidatorFactory;

public class ValidatorDefinitionTest {
    @Test
    public void mapsAndFiltersOverShapes() {
        List<ValidationEvent> events = Model.assembler()
                .addImport(getClass().getResource("validator-filtering-and-mapping.smithy"))
                .validatorFactory(new ValidatorFactory() {
                    @Override
                    public List<Validator> loadBuiltinValidators() {
                        return Collections.emptyList();
                    }

                    @Override
                    public Optional<Validator> createValidator(String name, ObjectNode configuration) {
                        return name.equals("hello")
                               ? Optional.of(
                                       model -> model.shapes()
                                               .map(shape -> ValidationEvent.builder()
                                                       .id("hello.subpart")
                                                       .shape(shape)
                                                       .severity(Severity.WARNING)
                                                       .message("Hello!")
                                                       .build())
                                               .collect(Collectors.toList()))
                               : Optional.empty();
                    }
                })
                .assemble()
                .getValidationEvents();

        assertThat(events, not(empty()));
        Assertions.assertEquals(2, events.stream().filter(e -> e.getId().equals("hello.subpart")).count());
        Assertions.assertEquals(4, events.stream().filter(e -> e.getId().equals("customHello.subpart")).count());

        // Ensure that template expansion works.
        for (ValidationEvent event : events) {
            if (event.getId().equals("customHello.subpart")) {
                assertThat(event.getMessage(), equalTo("Test Hello!"));
            }
        }
    }
}
