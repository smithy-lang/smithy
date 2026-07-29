/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.protocol.traits;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidatedResult;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidationEventDecorator;
import software.amazon.smithy.model.validation.ValidatorFactory;

public class IndexedTraitValidatorTest {

    @Test
    public void validationEventsCanBeDecorated() {
        ValidationEventDecorator decorator = new ValidationEventDecorator() {
            @Override
            public boolean canDecorate(ValidationEvent event) {
                return event.getId().equals("IndexedTrait");
            }

            @Override
            public ValidationEvent decorate(ValidationEvent event) {
                return event.toBuilder()
                        .severity(Severity.SUPPRESSED)
                        .suppressionReason("Replaced by specialized index validation")
                        .build();
            }
        };
        ValidatorFactory validatorFactory = ValidatorFactory.createServiceFactory(
                Collections.singletonList(new IndexedTraitValidator()),
                Collections.emptyList(),
                Collections.singletonList(decorator));

        ValidatedResult<Model> result = Model.assembler()
                .discoverModels()
                .validatorFactory(validatorFactory)
                .addUnparsedModel("decorated.smithy",
                        "$version: \"2.0\"\n"
                                + "namespace smithy.example\n"
                                + "use smithy.protocols#idx\n"
                                + "structure Indexed {\n"
                                + "    @idx(1)\n"
                                + "    first: String\n"
                                + "    @idx(1)\n"
                                + "    second: String\n"
                                + "}\n")
                .assemble();

        Assertions.assertFalse(result.isBroken());
        List<ValidationEvent> events = result.getValidationEvents(Severity.SUPPRESSED);
        Assertions.assertEquals(1, events.size());
        Assertions.assertEquals(
                ShapeId.from("smithy.example#Indexed$second"),
                events.get(0).getShapeId().get());
        Assertions.assertEquals(
                "Replaced by specialized index validation",
                events.get(0).getSuppressionReason().get());
    }
}
