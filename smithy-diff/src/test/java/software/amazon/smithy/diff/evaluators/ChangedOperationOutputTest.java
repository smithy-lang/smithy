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
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.validation.ValidationEvent;

public class ChangedOperationOutputTest {
    @Test
    public void detectWhenOperationOutputIsChanged() {
        OperationShape o1 = OperationShape.builder().id("foo.baz#Bar").output(ShapeId.from("foo.baz#A")).build();
        OperationShape o2 = OperationShape.builder().id("foo.baz#Bar").output(ShapeId.from("foo.baz#B")).build();
        StructureShape a = StructureShape.builder().id("foo.baz#A").build();
        StructureShape b = StructureShape.builder().id("foo.baz#B").build();
        Model modelA = Model.assembler().addShapes(o1, a).assemble().unwrap();
        Model modelB = Model.assembler().addShapes(o2, b).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "ChangedOperationOutput").size(), equalTo(1));
    }
}
