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
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.ErrorTrait;
import software.amazon.smithy.model.validation.ValidationEvent;

public class RemovedServiceErrorTest {
    @Test
    public void detectsRemovedErrors() {
        Shape e1 = StructureShape.builder()
                .id("foo.baz#E1")
                .addTrait(new ErrorTrait("client"))
                .build();
        Shape e2 = StructureShape.builder()
                .id("foo.baz#E2")
                .addTrait(new ErrorTrait("client"))
                .build();
        ServiceShape service1 = ServiceShape.builder()
                .id("foo.baz#S")
                .version("X")
                .addError(e1)
                .addError(e2)
                .build();
        ServiceShape service2 = service1.toBuilder().clearErrors().build();
        Model modelA = Model.assembler().addShapes(service1, e1, e2).assemble().unwrap();
        Model modelB = Model.assembler().addShapes(service2, e1, e2).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        // Emits one even for both removals.
        assertThat(TestHelper.findEvents(events, "RemovedServiceError").size(), equalTo(2));
    }
}
