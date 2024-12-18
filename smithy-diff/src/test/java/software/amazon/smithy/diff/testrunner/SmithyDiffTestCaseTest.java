/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.diff.testrunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Paths;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.ListUtils;

public class SmithyDiffTestCaseTest {
    @Test
    public void validatesThatEventsAreValid() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> SmithyDiffTestCase.parseValidationEvent("[ERROR] - m", "filename"));

        assertTrue(e.getMessage().contains("`filename`"));
        assertTrue(e.getMessage().contains("SUPPRESSED|NOTE|WARNING|DANGER|ERROR"));
    }

    @Test
    public void parsesValidEvents() {
        SmithyDiffTestCase.parseValidationEvent("[ERROR] -: message | EventId /filename:0:0", "filename");
    }

    @Test
    public void throwsOnNonExistentFiles() {
        assertThrows(Exception.class, () -> SmithyDiffTestCase.from(Paths.get("."), "nonexistent"));
    }

    @Test
    public void matchesMessageUsingPrefix() {
        ValidationEvent actual = ValidationEvent.builder()
                .id("FooBar")
                .severity(Severity.DANGER)
                .message("This is a test")
                .build();
        ValidationEvent expected = actual.toBuilder().message("This is").build();
        SmithyDiffTestCase testCase =
                new SmithyDiffTestCase(Paths.get("."), "test", Collections.singletonList(expected));
        SmithyDiffTestCase.Result result = testCase.createResult(Collections.singletonList(actual));

        assertThat(result.isInvalid(), is(false));
    }

    @Test
    public void failsWhenMessageDoesNotMatchPrefix() {
        ValidationEvent actual = ValidationEvent.builder()
                .id("FooBar")
                .severity(Severity.DANGER)
                .message("Not a test")
                .build();
        ValidationEvent expected = actual.toBuilder().message("This is").build();
        SmithyDiffTestCase testCase =
                new SmithyDiffTestCase(Paths.get("."), "test", Collections.singletonList(expected));
        SmithyDiffTestCase.Result result = testCase.createResult(Collections.singletonList(actual));

        assertThat(result.isInvalid(), is(true));
    }

    @Test
    public void matchesOnShapeId() {
        ValidationEvent actual = ValidationEvent.builder()
                .id("FooBar")
                .severity(Severity.DANGER)
                .message("abc")
                .shapeId(ShapeId.from("foo.baz#Bar"))
                .build();
        SmithyDiffTestCase testCase = new SmithyDiffTestCase(Paths.get("."), "test", Collections.singletonList(actual));
        SmithyDiffTestCase.Result result = testCase.createResult(Collections.singletonList(actual));

        assertThat(result.isInvalid(), is(false));
    }

    @Test
    public void failsWhenShapeIdDoesNotMatch() {
        ValidationEvent actual = ValidationEvent.builder()
                .id("FooBar")
                .severity(Severity.DANGER)
                .message("abc")
                .shapeId(ShapeId.from("foo.baz#Bar"))
                .build();
        ValidationEvent expected = actual.toBuilder().shapeId(null).build();
        SmithyDiffTestCase testCase =
                new SmithyDiffTestCase(Paths.get("."), "test", Collections.singletonList(expected));
        SmithyDiffTestCase.Result result = testCase.createResult(Collections.singletonList(actual));

        assertThat(result.isInvalid(), is(true));
    }

    @Test
    public void multilineEventsPrintedWhenFormatting() {
        ValidationEvent e1 = ValidationEvent.builder()
                .id("FooBar")
                .severity(Severity.DANGER)
                .message(
                        "1: first line\n"
                                + "1: second line\n"
                                + "1: third line\n")
                .shapeId(ShapeId.from("foo.baz#Bar"))
                .build();
        ValidationEvent e2 = ValidationEvent.builder()
                .id("FooBar")
                .severity(Severity.DANGER)
                .message(
                        "2: first line\n"
                                + "2: second line\n"
                                + "2: third line\n")
                .shapeId(ShapeId.from("foo.baz#Bar"))
                .build();

        SmithyDiffTestCase.Result result = new SmithyDiffTestCase.Result(
                "test",
                ListUtils.of(e1, e2),
                ListUtils.of(e1, e2));

        assertThat(result.toString(),
                equalTo("============================\n"
                        + "Model Diff Validation Result\n"
                        + "============================\n"
                        + "test\n"
                        + "\n"
                        + "Did not match the following events\n"
                        + "----------------------------------\n"
                        + "[DANGER] foo.baz#Bar: 1: first line\n"
                        + "1: second line\n"
                        + "1: third line\n"
                        + " | FooBar N/A:0:0\n"
                        + "\n"
                        + "[DANGER] foo.baz#Bar: 2: first line\n"
                        + "2: second line\n"
                        + "2: third line\n"
                        + " | FooBar N/A:0:0\n"
                        + "\n"
                        + "\n"
                        + "Encountered unexpected events\n"
                        + "-----------------------------\n"
                        + "[DANGER] foo.baz#Bar: 1: first line\n"
                        + "1: second line\n"
                        + "1: third line\n"
                        + " | FooBar N/A:0:0\n"
                        + "\n"
                        + "[DANGER] foo.baz#Bar: 2: first line\n"
                        + "2: second line\n"
                        + "2: third line\n"
                        + " | FooBar N/A:0:0\n"
                        + "\n"));
    }
}
