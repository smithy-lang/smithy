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

package software.amazon.smithy.model.validation.testrunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidatedResult;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.ListUtils;

public class SmithyTestCaseTest {
    @Test
    public void validatesThatEventsAreValid() {
        IllegalArgumentException e = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> SmithyTestCase.parseValidationEvent("[ERROR] - m", "filename"));

        assertTrue(e.getMessage().contains("`filename`"));
        assertTrue(e.getMessage().contains("SUPPRESSED|NOTE|WARNING|DANGER|ERROR"));
    }

    @Test
    public void parsesValidEvents() {
        SmithyTestCase.parseValidationEvent("[ERROR] -: message | EventId /filename:0:0", "filename");
    }

    @Test
    public void requiresFileExtension() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> SmithyTestCase.fromModelFile("/foo/bar"));
    }

    @Test
    public void matchesMessageUsingPrefix() {
        ValidationEvent actual = ValidationEvent.builder()
                .id("FooBar")
                .severity(Severity.DANGER)
                .message("This is a test")
                .build();
        ValidationEvent expected = actual.toBuilder().message("This is").build();
        SmithyTestCase testCase = new SmithyTestCase("/foo/bar.json", Collections.singletonList(expected));
        ValidatedResult<Model> validated = ValidatedResult.fromErrors(Collections.singleton(actual));
        SmithyTestCase.Result result = testCase.createResult(validated);

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
        SmithyTestCase testCase = new SmithyTestCase("/foo/bar.json", Collections.singletonList(expected));
        ValidatedResult<Model> validated = ValidatedResult.fromErrors(Collections.singleton(actual));
        SmithyTestCase.Result result = testCase.createResult(validated);

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
        SmithyTestCase testCase = new SmithyTestCase("/foo/bar.json", Collections.singletonList(actual));
        ValidatedResult<Model> validated = ValidatedResult.fromErrors(Collections.singleton(actual));
        SmithyTestCase.Result result = testCase.createResult(validated);

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
        SmithyTestCase testCase = new SmithyTestCase("/foo/bar.json", Collections.singletonList(expected));
        ValidatedResult<Model> validated = ValidatedResult.fromErrors(Collections.singleton(actual));
        SmithyTestCase.Result result = testCase.createResult(validated);

        assertThat(result.isInvalid(), is(true));
    }

    @Test
    public void newlinesAreBetweenEventsWhenFormatting() {
        ValidationEvent e1 = ValidationEvent.builder()
                .id("FooBar")
                .severity(Severity.DANGER)
                .message("a")
                .shapeId(ShapeId.from("foo.baz#Bar"))
                .build();
        ValidationEvent e2 = ValidationEvent.builder()
                .id("FooBar")
                .severity(Severity.DANGER)
                .message("b")
                .shapeId(ShapeId.from("foo.baz#Bar"))
                .build();

        SmithyTestCase.Result result = new SmithyTestCase.Result(
                "/foo/bar.json",
                ListUtils.of(e1, e2),
                ListUtils.of(e1, e2));

        assertThat(result.toString(), equalTo("=======================\n"
                                              + "Model Validation Result\n"
                                              + "=======================\n"
                                              + "/foo/bar.json\n"
                                              + "\n"
                                              + "Did not match the following events\n"
                                              + "----------------------------------\n"
                                              + "[DANGER] foo.baz#Bar: a | FooBar N/A:0:0\n"
                                              + "[DANGER] foo.baz#Bar: b | FooBar N/A:0:0\n"
                                              + "\n"
                                              + "\n"
                                              + "Encountered unexpected events\n"
                                              + "-----------------------------\n"
                                              + "[DANGER] foo.baz#Bar: a | FooBar N/A:0:0\n"
                                              + "[DANGER] foo.baz#Bar: b | FooBar N/A:0:0\n\n"));
    }
}
