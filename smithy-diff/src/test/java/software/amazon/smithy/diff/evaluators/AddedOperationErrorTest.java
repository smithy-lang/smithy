/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.diff.evaluators;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.diff.ModelDiff;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.ErrorTrait;
import software.amazon.smithy.model.validation.ValidationEvent;

public class AddedOperationErrorTest {
    @Test
    public void detectsAddedErrors() {
        SourceLocation s1 = new SourceLocation("main.smithy", 1, 2);
        SourceLocation s2 = new SourceLocation("main.smithy", 3, 4);
        Shape e1 = StructureShape.builder()
                .id("foo.baz#E1")
                .addTrait(new ErrorTrait("client"))
                .source(s1)
                .build();
        Shape e2 = StructureShape.builder()
                .id("foo.baz#E2")
                .addTrait(new ErrorTrait("client"))
                .source(s2)
                .build();
        OperationShape operation1 = OperationShape.builder().id("foo.baz#Operation").build();
        Shape operation2 = operation1.toBuilder().addError(e1.getId()).addError(e2.getId()).build();
        Model modelA = Model.assembler().addShapes(operation1, e1, e2).assemble().unwrap();
        Model modelB = Model.assembler().addShapes(operation2, e1, e2).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "AddedOperationError").size(), equalTo(2));
        assertThat(TestHelper.findEvents(events, "AddedOperationError.E1").size(), equalTo(1));
        assertThat(
                TestHelper.findEvents(events, "AddedOperationError.E1").stream().findFirst().get().getSourceLocation(),
                equalTo(s1));
        assertThat(TestHelper.findEvents(events, "AddedOperationError.E2").size(), equalTo(1));
        assertThat(
                TestHelper.findEvents(events, "AddedOperationError.E2").stream().findFirst().get().getSourceLocation(),
                equalTo(s2));
    }
}
