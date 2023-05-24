/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.model.loader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.NodeValidationVisitorTest;
import software.amazon.smithy.model.validation.ValidatedResult;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidationEventDecorator;
import software.amazon.smithy.model.validation.Validator;
import software.amazon.smithy.model.validation.ValidatorFactory;
import software.amazon.smithy.utils.SetUtils;

public class ValidationEventDecoratorTest {

    static final String HINT = "Consider connecting this structure to a service";
    static final String UNREFERENCED_SHAPE_EVENT_ID = "UnreferencedShape";
    static final Set<ShapeId> STRUCT_SHAPE_IDS = SetUtils.of(ShapeId.from("ns.foo#Structure"),
                                                             ShapeId.from("ns.foo#Structure2"),
                                                             ShapeId.from("ns.foo#Structure3"));

    @Test
    public void canDecorateValidationEvents() {
        ValidatedResult<Model> result = Model.assembler()
                                             .addImport(NodeValidationVisitorTest.class.getResource("node-validator"
                                                                                                    + ".json"))
                                             .validatorFactory(testFactory(new DummyHintValidationEventDecorator()))
                                             .assemble();
        for (ValidationEvent event : result.getValidationEvents()) {
            ShapeId eventShapeId = event.getShapeId().orElse(null);
            if (STRUCT_SHAPE_IDS.contains(eventShapeId)) {
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
                 .addImport(NodeValidationVisitorTest.class.getResource("node-validator"
                                                                        + ".json"))
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

    static class DummyHintValidationEventDecorator implements ValidationEventDecorator {

        @Override
        public boolean canDecorate(ValidationEvent ev) {
            return ev.containsId(UNREFERENCED_SHAPE_EVENT_ID) && ev.getMessage().contains("The structure ");
        }

        @Override
        public ValidationEvent decorate(ValidationEvent ev) {
            return ev.toBuilder().hint(HINT).build();
        }
    }

    static class ThrowingValidationEventDecorator implements ValidationEventDecorator {

        @Override
        public boolean canDecorate(ValidationEvent ev) {
            return ev.containsId(UNREFERENCED_SHAPE_EVENT_ID) && ev.getMessage().contains("The structure ");
        }

        @Override
        public ValidationEvent decorate(ValidationEvent ev) {
            throw new RuntimeException("ups");
        }
    }
}
