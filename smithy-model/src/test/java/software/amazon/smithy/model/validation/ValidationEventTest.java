/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.model.validation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StringShape;

public class ValidationEventTest {

    @Test
    public void requiresMessage() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            ValidationEvent.builder()
                    .severity(Severity.ERROR)
                    .id("foo")
                    .build();
        });
    }

    @Test
    public void requiresSeverity() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            ValidationEvent.builder()
                    .message("test")
                    .id("foo")
                    .build();
        });
    }

    @Test
    public void requiresId() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            ValidationEvent.builder()
                    .severity(Severity.ERROR)
                    .message("test")
                    .build();
        });
    }

    @Test
    public void suppressionIsOnlyValidWithSuppress() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            ValidationEvent.builder()
                    .severity(Severity.ERROR)
                    .message("test")
                    .id("foo")
                    .suppressionReason("Some reason")
                    .build();
        });
    }

    @Test
    public void loadsWithFromNode() {
        ShapeId id = ShapeId.from("ns.foo#baz");
        ValidationEvent event = ValidationEvent.fromNode(Node.parse(
                "{\"id\": \"abc.foo\", \"severity\": \"SUPPRESSED\", \"suppressionReason\": \"my reason\", "
                + "\"shapeId\": \"ns.foo#baz\", \"message\": \"The message\", "
                + "\"filename\": \"/path/to/file.smithy\", \"line\": 7, \"column\": 2}"));

        assertThat(event.getSeverity(), equalTo(Severity.SUPPRESSED));
        assertThat(event.getMessage(), equalTo("The message"));
        assertThat(event.getId(), equalTo("abc.foo"));
        assertThat(event.getSuppressionReason().get(), equalTo("my reason"));
        assertThat(event.getShapeId().get(), is(id));
    }

    @Test
    public void loadsWithFromNodeWithHint() {
        ShapeId id = ShapeId.from("ns.foo#baz");
        ValidationEvent event = ValidationEvent.fromNode(Node.parse(
            "{\"id\": \"abc.foo\", \"severity\": \"SUPPRESSED\", \"suppressionReason\": \"my reason\", "
            + "\"shapeId\": \"ns.foo#baz\", \"message\": \"The message\", "
            + "\"hint\": \"The hint\", \"filename\": \"/path/to/file.smithy\", \"line\": 7, \"column\": 2}"));

        assertThat(event.getSeverity(), equalTo(Severity.SUPPRESSED));
        assertThat(event.getMessage(), equalTo("The message"));
        assertThat(event.getId(), equalTo("abc.foo"));
        assertThat(event.getSuppressionReason().get(), equalTo("my reason"));
        assertThat(event.getShapeId().get(), is(id));
        assertThat(event.getHint().get(),equalTo("The hint"));
    }


    @Test
    public void hasGetters() {
        ShapeId id = ShapeId.from("ns.foo#baz");
        ValidationEvent event = ValidationEvent.builder()
                .message("The message")
                .severity(Severity.SUPPRESSED)
                .shapeId(id)
                .id("abc.foo")
                .suppressionReason("my reason")
                .hint("The hint")
                .build();

        assertThat(event.getSeverity(), equalTo(Severity.SUPPRESSED));
        assertThat(event.getMessage(), equalTo("The message"));
        assertThat(event.getId(), equalTo("abc.foo"));
        assertThat(event.getSuppressionReason().get(), equalTo("my reason"));
        assertThat(event.getShapeId().get(), is(id));
        assertThat(event.getHint().get(), equalTo("The hint"));
        assertThat(event.getSeverity(), is(Severity.SUPPRESSED));
    }

    @Test
    public void usesShapeSourceWhenPresent() {
        StringShape stringShape = StringShape.builder().id("ns.foo#bar").source("file", 1, 2).build();
        ValidationEvent event = ValidationEvent.builder()
                .message("The message")
                .severity(Severity.ERROR)
                .shape(stringShape)
                .id("abc.foo")
                .build();

        assertThat(event.getSourceLocation(), is(stringShape.getSourceLocation()));
    }

    @Test
    public void usesEmptyLocationWhenNoneSet() {
        ShapeId id = ShapeId.from("ns.foo#baz");
        ValidationEvent event = ValidationEvent.builder()
                .message("The message")
                .severity(Severity.SUPPRESSED)
                .shapeId(id)
                .id("abc.foo")
                .suppressionReason("my reason")
                .build();

        assertThat(event.getSourceLocation(), is(SourceLocation.none()));
    }

    @Test
    public void createsEventBuilderFromEvent() {
        ValidationEvent event = ValidationEvent.builder()
                .message("The message")
                .severity(Severity.SUPPRESSED)
                .shapeId(ShapeId.from("ns.foo#baz"))
                .id("abc.foo")
                .hint("The hint")
                .suppressionReason("my reason")
                .build();
        ValidationEvent other = event.toBuilder().build();

        assertThat(event, equalTo(other));
    }

    @Test
    public void sameInstanceIsEqual() {
        ValidationEvent a = ValidationEvent.builder()
                .message("The message")
                .severity(Severity.SUPPRESSED)
                .shapeId(ShapeId.from("ns.foo#bar"))
                .id("abc.foo")
                .build();

        assertEquals(a, a);
    }

    @Test
    public void differentTypesAreNotEqual() {
        ValidationEvent a = ValidationEvent.builder()
                .message("The message")
                .severity(Severity.SUPPRESSED)
                .shapeId(ShapeId.from("ns.foo#bar"))
                .id("abc.foo")
                .build();

        assertNotEquals(a, "test");
    }

    @Test
    public void differentMessagesAreNotEqual() {
        ValidationEvent a = ValidationEvent.builder()
                .message("The message")
                .severity(Severity.SUPPRESSED)
                .shapeId(ShapeId.from("ns.foo#bar"))
                .id("abc.foo")
                .build();
        ValidationEvent b = a.toBuilder().message("other message").build();

        assertNotEquals(a, b);
        assertNotEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void differentSeveritiesAreNotEqual() {
        ValidationEvent a = ValidationEvent.builder()
                .message("The message")
                .severity(Severity.SUPPRESSED)
                .shapeId(ShapeId.from("ns.foo#bar"))
                .id("abc.foo")
                .build();
        ValidationEvent b = a.toBuilder().severity(Severity.ERROR).build();

        assertNotEquals(a, b);
        assertNotEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void differentSourceLocationsAreNotEqual() {
        ValidationEvent a = ValidationEvent.builder()
                .message("The message")
                .severity(Severity.SUPPRESSED)
                .shapeId(ShapeId.from("ns.foo#bar"))
                .id("abc.foo")
                .suppressionReason("my reason")
                .sourceLocation(SourceLocation.none())
                .build();
        ValidationEvent b = a.toBuilder().sourceLocation(new SourceLocation("foo", 10, 0)).build();

        assertNotEquals(a, b);
        assertNotEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void differentShapeIdAreNotEqual() {
        ValidationEvent a = ValidationEvent.builder()
                .message("The message")
                .severity(Severity.SUPPRESSED)
                .shapeId(ShapeId.from("ns.foo#bar"))
                .id("abc.foo")
                .suppressionReason("my reason")
                .sourceLocation(SourceLocation.none())
                .build();
        ValidationEvent b = a.toBuilder().shapeId(ShapeId.from("ns.foo#qux")).build();

        assertNotEquals(a, b);
        assertNotEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void differentEventIdAreNotEqual() {
        ValidationEvent a = ValidationEvent.builder()
                .message("The message")
                .severity(Severity.SUPPRESSED)
                .shapeId(ShapeId.from("ns.foo#bar"))
                .id("abc.foo")
                .suppressionReason("my reason")
                .sourceLocation(SourceLocation.none())
                .build();
        ValidationEvent b = a.toBuilder().id("other.id").build();

        assertNotEquals(a, b);
        assertNotEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void differentSuppressionReasonAreNotEqual() {
        ValidationEvent a = ValidationEvent.builder()
                .message("The message")
                .severity(Severity.SUPPRESSED)
                .shapeId(ShapeId.from("ns.foo#bar"))
                .id("abc.foo")
                .suppressionReason("my reason")
                .sourceLocation(SourceLocation.none())
                .build();
        ValidationEvent b = a.toBuilder().suppressionReason("other reason").build();

        assertNotEquals(a, b);
        assertNotEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void differentHintAreNotEqual() {
        ValidationEvent a = ValidationEvent.builder()
                                           .message("The message")
                                           .severity(Severity.SUPPRESSED)
                                           .shapeId(ShapeId.from("ns.foo#bar"))
                                           .id("abc.foo")
                                           .suppressionReason("my reason")
                                           .hint("The hint")
                                           .sourceLocation(SourceLocation.none())
                                           .build();
        ValidationEvent b = a.toBuilder().hint("other hint").build();

        assertNotEquals(a, b);
        assertNotEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void toStringContainsSeverityAndEventId() {
        ValidationEvent a = ValidationEvent.builder()
                .message("The message")
                .severity(Severity.SUPPRESSED)
                .id("abc.foo")
                .build();

        assertEquals(a.toString(), "[SUPPRESSED] -: The message | abc.foo N/A:0:0");
    }

    @Test
    public void toStringContainsShapeId() {
        ValidationEvent a = ValidationEvent.builder()
                .message("The message")
                .severity(Severity.SUPPRESSED)
                .id("abc.foo")
                .shapeId(ShapeId.from("ns.foo#baz"))
                .build();

        assertEquals(a.toString(), "[SUPPRESSED] ns.foo#baz: The message | abc.foo N/A:0:0");
    }

    @Test
    public void toStringContainsSourceLocation() {
        ValidationEvent a = ValidationEvent.builder()
                .message("The message")
                .severity(Severity.SUPPRESSED)
                .id("abc.foo")
                .shapeId(ShapeId.from("ns.foo#baz"))
                .sourceLocation(new SourceLocation("file", 1, 2))
                .build();

        assertEquals(a.toString(), "[SUPPRESSED] ns.foo#baz: The message | abc.foo file:1:2");
    }

    @Test
    public void toStringContainsSuppressionReason() {
        ValidationEvent a = ValidationEvent.builder()
                .message("The message")
                .severity(Severity.SUPPRESSED)
                .id("abc.foo")
                .shapeId(ShapeId.from("ns.foo#baz"))
                .suppressionReason("Foo baz bar")
                .sourceLocation(new SourceLocation("file", 1, 2))
                .build();

        assertEquals(a.toString(), "[SUPPRESSED] ns.foo#baz: The message (Foo baz bar) | abc.foo file:1:2");
    }

    @Test
    public void toStringDoesContainsHint() {
        ValidationEvent a = ValidationEvent.builder()
                                           .message("The message")
                                           .severity(Severity.SUPPRESSED)
                                           .id("abc.foo")
                                           .shapeId(ShapeId.from("ns.foo#baz"))
                                           .suppressionReason("Foo baz bar")
                                           .hint("The hint")
                                           .sourceLocation(new SourceLocation("file", 1, 2))
                                           .build();

        assertEquals(a.toString(), "[SUPPRESSED] ns.foo#baz: The message (Foo baz bar) [The hint] | abc.foo file:1:2");
    }

    @Test
    public void convertsToNode() {
        ValidationEvent a = ValidationEvent.builder()
                .message("The message")
                .severity(Severity.SUPPRESSED)
                .id("abc.foo")
                .shapeId(ShapeId.from("ns.foo#baz"))
                .sourceLocation(new SourceLocation("file", 1, 2))
                .build();

        ObjectNode result = a.toNode().expectObjectNode();
        assertEquals(result.getMember("id").get().asStringNode().get().getValue(), "abc.foo");
        assertEquals(result.getMember("shapeId").get().asStringNode().get().getValue(), "ns.foo#baz");
        assertEquals(result.getMember("filename").get().asStringNode().get().getValue(), "file");
        assertEquals(result.getMember("message").get().asStringNode().get().getValue(), "The message");
    }

    @ParameterizedTest
    @MethodSource("containsIdSupplier")
    public void suppressesEventIds(boolean match, String eventId, String testId) {
        ValidationEvent event = ValidationEvent.builder().id(eventId).severity(Severity.DANGER).message(".").build();

        assertThat(eventId + " contains? " + match + " " + testId, event.containsId(testId), is(match));
    }

    public static Stream<Arguments> containsIdSupplier() {
        return Stream.of(
                Arguments.of(true, "BadThing", "BadThing"),
                Arguments.of(true, "BadThing.Foo", "BadThing"),
                Arguments.of(true, "BadThing.Foo", "BadThing.Foo"),
                Arguments.of(true, "BadThing.Foo.Bar", "BadThing.Foo.Bar"),

                Arguments.of(false, "BadThing.Foo", "BadThing.Foo.Bar"),
                Arguments.of(false, "BadThing.Foo", "BadThing.Foo.Bar.Baz"),
                Arguments.of(false, "BadThing.Fooz", "BadThing.Foo"),
                Arguments.of(false, "BadThing.Foo.Bar", "BadThing.Foo.Bar.Baz"),

                // Tests for strange, but acceptable ids and suppression IDs. Preventing these now is
                // technically backward incompatible, so they're acceptable.
                Arguments.of(true, "BadThing.", "BadThing."),
                Arguments.of(true, "BadThing.", "BadThing"),
                Arguments.of(false, "BadThing", "BadThing."),
                Arguments.of(true, "BadThing.Foo.", "BadThing.Foo"),
                Arguments.of(true, "BadThing.Foo.", "BadThing.Foo."),
                Arguments.of(false, "BadThing.Foo.", "BadThing.Foo.Bar")
        );
    }
}
