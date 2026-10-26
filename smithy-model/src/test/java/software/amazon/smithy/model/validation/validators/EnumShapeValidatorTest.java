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
import software.amazon.smithy.model.shapes.EnumShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;

public class EnumShapeValidatorTest {
    private static final String LONG_NAME = "A".repeat(200_000);

    @Test
    public void validatesLongNonMatchingNameInLinearTime() {
        String name = LONG_NAME + "a";
        List<ValidationEvent> events = assertTimeoutPreemptively(Duration.ofSeconds(5), () -> validate(name));

        assertEquals(1, events.size());
        ValidationEvent event = events.get(0);
        assertEquals(Severity.WARNING, event.getSeverity());
        assertEquals(ShapeId.from("ns.foo#Bar$" + name), event.getShapeId().get());
        assertTrue(event.getMessage().contains("does not match the recommended enum name format"));
    }

    @Test
    public void acceptsLongMatchingName() {
        List<ValidationEvent> events = assertTimeoutPreemptively(Duration.ofSeconds(5), () -> validate(LONG_NAME));

        assertTrue(events.isEmpty());
    }

    private static List<ValidationEvent> validate(String memberName) {
        EnumShape shape = (EnumShape) EnumShape.builder()
                .id("ns.foo#Bar")
                .addMember(memberName, "v")
                .build();
        Model model = Model.builder().addShape(shape).build();
        return new EnumShapeValidator().validate(model);
    }

}
