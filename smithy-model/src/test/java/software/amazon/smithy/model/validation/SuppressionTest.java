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
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.shapes.ShapeId;

public class SuppressionTest {

    @Test
    public void hasGetters() {
        Suppression suppression = Suppression.builder()
                .addValidatorId("*")
                .reason("the reason")
                .addShape("ns.foo#bar")
                .addShape("ns.foo#")
                .build();

        assertThat(suppression.getReason().get(), equalTo("the reason"));
        assertThat(suppression.getValidatorIds(), hasSize(1));
        assertThat(suppression.getShapes(), hasSize(1));
        assertThat(suppression.getNamespaceNames(), hasSize(1));
        assertTrue(suppression.getNamespaceNames().contains("ns.foo"));
        assertTrue(suppression.getValidatorIds().contains("*"));
        assertTrue(suppression.getShapes().contains(ShapeId.from("ns.foo#bar")));
    }

    @Test
    public void doesNotSuppressEventsWhenNoMatches() {
        List<Suppression> suppressions = new ArrayList<>();
        suppressions.add(Suppression.builder().addValidatorId("foo").build());
        suppressions.add(Suppression.builder().addValidatorId("baz").build());
        suppressions.add(Suppression.builder().addValidatorId("bar").build());
        ValidationEvent input = ValidationEvent.builder()
                .eventId("qux")
                .message("test")
                .severity(Severity.WARNING)
                .build();
        ValidationEvent output = Suppression.suppressEvent(input, suppressions);

        assertThat(output.getSeverity(), is(Severity.WARNING));
    }

    @Test
    public void suppressesEventsWhenThereAreMatches() {
        List<Suppression> suppressions = new ArrayList<>();
        suppressions.add(Suppression.builder().addValidatorId("foo").build());
        suppressions.add(Suppression.builder().addValidatorId("baz").build());
        suppressions.add(Suppression.builder().addValidatorId("bar").build());
        ValidationEvent input = ValidationEvent.builder()
                .eventId("foo")
                .message("test")
                .severity(Severity.WARNING)
                .build();
        ValidationEvent output = Suppression.suppressEvent(input, suppressions);

        assertThat(output.getSeverity(), is(Severity.SUPPRESSED));
    }

    @Test
    public void ruleNameCannotBeEmpty() {
        Assertions.assertThrows(SourceException.class, () -> Suppression.builder().addValidatorId("").build());
    }

    @Test
    public void ruleNameCannotContainMultipleWildcards() {
        Assertions.assertThrows(SourceException.class, () -> {
            Suppression.builder().addValidatorId("foo.*.bar.*").build();
        });
    }

    @Test
    public void wildcardsMustOnlyBeAtEndOfString() {
        Assertions.assertThrows(SourceException.class, () -> {
            Suppression.builder().addValidatorId("foo.*.bar").build();
        });
    }

    @Test
    public void convertsToString() {
        Suppression suppression = Suppression.builder().addValidatorId("foo").addValidatorId("baz").build();

        assertEquals(suppression.toString(), "suppression of `foo`, `baz`");
    }
}
