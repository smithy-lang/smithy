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
import static org.hamcrest.Matchers.startsWith;

import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.diff.ModelDiff;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.ErrorTrait;
import software.amazon.smithy.model.validation.ValidationEvent;

public class RemovedOperationErrorTest {
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
        OperationShape operation1 = OperationShape.builder()
                .id("foo.baz#Operation")
                .addError(e1)
                .addError(e2)
                .build();
        Shape operation2 = operation1.toBuilder().clearErrors().build();
        Model modelA = Model.assembler().addShapes(operation1, e1, e2).assemble().unwrap();
        Model modelB = Model.assembler().addShapes(operation2, e1, e2).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        // Emits an event for each removal.
        assertThat(TestHelper.findEvents(events, "RemovedOperationError").size(), equalTo(2));
        assertThat(TestHelper.findEvents(events, "RemovedOperationError").get(0).toString(),
                startsWith("[WARNING] foo.baz#Operation: The `foo.baz#E1` error was removed " +
                        "from the `foo.baz#Operation` operation. | RemovedOperationError"));
        assertThat(TestHelper.findEvents(events, "RemovedOperationError").get(1).toString(),
                startsWith("[WARNING] foo.baz#Operation: The `foo.baz#E2` error was removed " +
                        "from the `foo.baz#Operation` operation. | RemovedOperationError"));
    }
}
