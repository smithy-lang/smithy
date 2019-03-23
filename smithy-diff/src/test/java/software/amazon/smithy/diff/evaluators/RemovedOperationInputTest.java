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
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;

public class RemovedOperationInputTest {
    @Test
    public void detectWhenOperationInputIsRemoved() {
        OperationShape a = OperationShape.builder().id("foo.baz#Bar").input(ShapeId.from("foo.baz#Input")).build();
        OperationShape b = OperationShape.builder().id("foo.baz#Bar").build();
        StructureShape input = StructureShape.builder().id("foo.baz#Input").build();
        Model modelA = Model.assembler().addShapes(a, input).assemble().unwrap();
        Model modelB = Model.assembler().addShapes(b, input).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "RemovedOperationInput").size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, a.getId()).size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, Severity.ERROR).size(), equalTo(1));
    }
}
