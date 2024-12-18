/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.traits;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;

public class EventSourceValidatorTest {
    @Test
    public void detectsWhenEventSourceIsUnexpected() {
        ServiceTrait trait = ServiceTrait.builder()
                .sdkId("Foo")
                .arnNamespace("foo")
                .cloudTrailEventSource("REPLACE_ME_LATER")
                .cloudFormationName("AWS::Foo")
                .build();
        ServiceShape service = ServiceShape.builder()
                .id("smithy.example#Foo")
                .version("123")
                .addTrait(trait)
                .build();
        Model model = Model.builder().addShape(service).build();
        EventSourceValidator validator = new EventSourceValidator();
        List<ValidationEvent> events = validator.validate(model);
        ValidationEvent event = getMatchingEvent(events, validator.getName());

        assertThat(event.getSeverity(), is(Severity.WARNING));
        assertThat(event.getShapeId().get(), equalTo(service.getId()));
        assertThat(event.getMessage(), containsString("must not use placeholders"));
    }

    @Test
    public void detectsWhenEventSourceIsPlaceholder() {
        ServiceTrait trait = ServiceTrait.builder()
                .sdkId("Foo")
                .arnNamespace("foo")
                .cloudTrailEventSource("notfoo.amazonaws.com")
                .cloudFormationName("AWS::Foo")
                .build();
        ServiceShape service = ServiceShape.builder()
                .id("smithy.example#Foo")
                .version("123")
                .addTrait(trait)
                .build();
        Model model = Model.builder().addShape(service).build();
        EventSourceValidator validator = new EventSourceValidator();
        List<ValidationEvent> events = validator.validate(model);
        ValidationEvent event = getMatchingEvent(events, validator.getName());

        assertThat(event.getSeverity(), is(Severity.WARNING));
        assertThat(event.getShapeId().get(), equalTo(service.getId()));
        assertThat(event.getMessage(),
                containsString("Expected 'foo.amazonaws.com', but found 'notfoo.amazonaws.com'"));
    }

    @Test
    public void ignoresKnownExceptions() {
        ServiceTrait trait = ServiceTrait.builder()
                .sdkId("Foo")
                .arnNamespace("cloudwatch")
                .cloudTrailEventSource("monitoring.amazonaws.com")
                .cloudFormationName("AWS::Foo")
                .build();
        ServiceShape service = ServiceShape.builder()
                .id("smithy.example#Foo")
                .version("123")
                .addTrait(trait)
                .build();
        Model model = Model.builder().addShape(service).build();
        EventSourceValidator validator = new EventSourceValidator();
        List<ValidationEvent> events = validator.validate(model);

        Assertions.assertThrows(RuntimeException.class, () -> getMatchingEvent(events, validator.getName()));
    }

    private ValidationEvent getMatchingEvent(List<ValidationEvent> events, String eventId) {
        for (ValidationEvent event : events) {
            if (event.getId().equals(eventId)) {
                return event;
            }
        }

        throw new RuntimeException("Expected validation event not found: " + eventId);
    }
}
