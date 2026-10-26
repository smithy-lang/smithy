/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.traits.EnumDefinition;
import software.amazon.smithy.model.traits.EnumTrait;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;

public class EnumTraitValidatorTest {
    @Test
    public void validatesLongNonMatchingNameInLinearTime() {
        String name = "A".repeat(200_000) + "a";
        StringShape shape = StringShape.builder()
                .id("ns.foo#Bar")
                .addTrait(EnumTrait.builder()
                        .addEnum(EnumDefinition.builder().name(name).value("v").build())
                        .build())
                .build();
        Model model = Model.builder().addShape(shape).build();

        List<ValidationEvent> events = assertTimeoutPreemptively(
                Duration.ofSeconds(5),
                () -> new EnumTraitValidator().validate(model));

        assertEquals(1, events.size());
        assertEquals(Severity.WARNING, events.get(0).getSeverity());
        assertTrue(events.get(0).getMessage().contains("does not match the recommended enum name format"));
    }
}
