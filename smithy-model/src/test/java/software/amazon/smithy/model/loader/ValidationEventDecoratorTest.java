/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.ValidatedResult;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidationEventDecorator;
import software.amazon.smithy.model.validation.Validator;
import software.amazon.smithy.model.validation.ValidatorFactory;
import software.amazon.smithy.utils.SetUtils;

public class ValidationEventDecoratorTest {

    static final String HINT = "We had to deprecate this";
    static final String SHAPE_EVENT_ID = "DeprecatedShape";
    static final Set<ShapeId> MATCHING_SHAPE_IDS = SetUtils.of(ShapeId.from("smithy.example#Foo$a"));

    @Test
    public void canDecorateValidationEvents() {
        ValidatedResult<Model> result = Model.assembler()
                .addImport(getClass().getResource("validation-event-decorator-test.smithy"))
                .validatorFactory(testFactory(new TestValidationEventDecorator()))
                .assemble();
        for (ValidationEvent event : result.getValidationEvents()) {
            ShapeId eventShapeId = event.getShapeId().orElse(null);
            if (MATCHING_SHAPE_IDS.contains(eventShapeId)) {
                assertThat(event.getHint().isPresent(), equalTo(true));
                assertThat(event.getHint().get(), equalTo(HINT));
            } else {
                assertThat(event.getHint().isPresent(), equalTo(false));
            }
        }
    }

    @Test
    public void exceptionsAreNotCaughtWhenDecoratorsThrow() {
        assertThrows(RuntimeException.class, () -> {
            Model.assembler()
                    .addImport(getClass().getResource("validation-event-decorator-test.smithy"))
                    .validatorFactory(testFactory(new ThrowingValidationEventDecorator()))
                    .assemble();
        });
    }

    static ValidatorFactory testFactory(ValidationEventDecorator decorator) {
        ValidatorFactory defaultValidatorFactory = ModelValidator.defaultValidationFactory();
        return new ValidatorFactory() {
            @Override
            public List<Validator> loadBuiltinValidators() {
                return defaultValidatorFactory.loadBuiltinValidators();
            }

            @Override
            public List<ValidationEventDecorator> loadDecorators() {
                return Arrays.asList(decorator);
            }

            @Override
            public Optional<Validator> createValidator(String name, ObjectNode configuration) {
                return defaultValidatorFactory.createValidator(name, configuration);
            }
        };
    }

    static class TestValidationEventDecorator implements ValidationEventDecorator {
        @Override
        public boolean canDecorate(ValidationEvent ev) {
            return ev.containsId(SHAPE_EVENT_ID);
        }

        @Override
        public ValidationEvent decorate(ValidationEvent ev) {
            return ev.toBuilder().hint(HINT).build();
        }
    }

    static class ThrowingValidationEventDecorator implements ValidationEventDecorator {
        @Override
        public boolean canDecorate(ValidationEvent ev) {
            return ev.containsId(SHAPE_EVENT_ID);
        }

        @Override
        public ValidationEvent decorate(ValidationEvent ev) {
            throw new RuntimeException("oops!");
        }
    }
}
