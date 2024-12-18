/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.suppressions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;

public class MetadataSuppressionTest {

    @Test
    public void doesNotMatchWhenUsingDifferentNamespace() {
        ObjectNode node = Node.objectNodeBuilder()
                .withMember("id", "Foo")
                .withMember("namespace", "smithy.example.nested")
                .build();
        Shape s = StringShape.builder().id("smithy.example#String").build();
        Suppression suppression = Suppression.fromMetadata(node);
        ValidationEvent event = ValidationEvent.builder()
                .id("Foo")
                .shape(s)
                .severity(Severity.DANGER)
                .message("test")
                .build();

        assertThat(suppression.test(event), is(false));
    }

    @ParameterizedTest
    @MethodSource("suppressions")
    public void suppressesEventIds(boolean match, String eventId, String suppressionId) {
        ObjectNode node = Node.objectNodeBuilder()
                .withMember("id", suppressionId)
                .withMember("namespace", "*")
                .build();
        Suppression suppression = Suppression.fromMetadata(node);
        ValidationEvent event = ValidationEvent.builder()
                .id(eventId)
                .severity(Severity.DANGER)
                .message("test")
                .build();

        assertThat(eventId + " is " + match + " match for " + suppressionId, suppression.test(event), is(match));
    }

    // See tests for ValidationEvent#containsId for exhaustive test cases.
    public static Stream<Arguments> suppressions() {
        return Stream.of(
                Arguments.of(true, "BadThing", "BadThing"),
                Arguments.of(true, "BadThing.Foo", "BadThing"),
                Arguments.of(false, "BadThing.Foo", "BadThing.Foo.Bar"),
                Arguments.of(false, "BadThing.Foo", "BadThing.Foo.Bar.Baz"),
                Arguments.of(false, "BadThing.Fooz", "BadThing.Foo"));
    }
}
