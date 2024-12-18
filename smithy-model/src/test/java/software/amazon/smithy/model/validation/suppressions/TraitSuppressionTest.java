/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.suppressions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.traits.SuppressTrait;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.ListUtils;

public class TraitSuppressionTest {

    @Test
    public void doesNotMatchWhenUsingDifferentShape() {
        Shape s1 = StringShape.builder()
                .id("smithy.example#String1")
                .addTrait(SuppressTrait.builder().values(ListUtils.of("Foo")).build())
                .build();
        Shape s2 = StringShape.builder().id("smithy.example#String2").build();
        Suppression suppression = Suppression.fromSuppressTrait(s1);
        ValidationEvent event = ValidationEvent.builder()
                .id("Foo")
                .shape(s2)
                .severity(Severity.DANGER)
                .message("test")
                .build();

        assertThat(suppression.test(event), is(false));
    }

    @ParameterizedTest
    @MethodSource("suppressions")
    public void suppressesEventIds(boolean match, String eventId, List<String> suppressions) {
        SuppressTrait trait = SuppressTrait.builder().values(suppressions).build();
        Shape s = StringShape.builder().id("smithy.example#String").addTrait(trait).build();
        Suppression suppression = Suppression.fromSuppressTrait(s);
        ValidationEvent event = ValidationEvent.builder()
                .id(eventId)
                .shapeId(s)
                .severity(Severity.DANGER)
                .message("test")
                .build();

        assertThat(eventId + " is " + match + " match for " + suppressions, suppression.test(event), is(match));
    }

    // See tests for ValidationEvent#containsId for exhaustive test cases.
    public static Stream<Arguments> suppressions() {
        return Stream.of(
                Arguments.of(true, "BadThing", ListUtils.of("BadThing")),
                Arguments.of(true, "BadThing", ListUtils.of("BadThing", "NotBadThing")),
                Arguments.of(true, "BadThing", ListUtils.of("NotBadThing", "BadThing")),
                Arguments.of(true, "BadThing.Foo", ListUtils.of("BadThing")),
                Arguments.of(false, "BadThing", ListUtils.of("NotBadThing")),
                Arguments.of(false, "BadThing.Foo", ListUtils.of("BadThing.Foo.Bar")),
                Arguments.of(false, "BadThing.Foo", ListUtils.of("BadThing.Foo.Bar.Baz")),
                Arguments.of(false, "BadThing.Fooz", ListUtils.of("BadThing.Foo")));
    }
}
