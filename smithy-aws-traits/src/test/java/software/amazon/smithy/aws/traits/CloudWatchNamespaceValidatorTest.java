/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.traits;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;

public class CloudWatchNamespaceValidatorTest {
    @Test
    public void validatesLongNonPascalCaseNamespaceInLinearTime() {
        List<ValidationEvent> events = assertTimeoutPreemptively(
                Duration.ofSeconds(5),
                () -> validate("AWS/" + "A".repeat(200_000) + "-"));

        assertEquals(1, events.size());
        assertEquals(Severity.DANGER, events.get(0).getSeverity());
        assertTrue(events.get(0).getMessage().contains("should be PascalCase"));
    }

    @Test
    public void acceptsLongPascalCaseNamespace() {
        List<ValidationEvent> events = assertTimeoutPreemptively(
                Duration.ofSeconds(5),
                () -> validate("AWS/" + "Ab".repeat(100_000)));

        assertTrue(events.isEmpty());
    }

    private static List<ValidationEvent> validate(String namespace) {
        ServiceTrait trait = ServiceTrait.builder()
                .sdkId("Foo")
                .arnNamespace("foo")
                .cloudTrailEventSource("foo.amazonaws.com")
                .cloudFormationName("Foo")
                .cloudWatchNamespace(namespace)
                .build();
        ServiceShape service = ServiceShape.builder()
                .id("smithy.example#Foo")
                .version("123")
                .addTrait(trait)
                .build();
        Model model = Model.builder().addShape(service).build();
        return new CloudWatchNamespaceValidator().validate(model);
    }
}
