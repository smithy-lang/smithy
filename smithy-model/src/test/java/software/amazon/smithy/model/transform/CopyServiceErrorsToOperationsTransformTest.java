/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.transform;

import static org.hamcrest.MatcherAssert.assertThat;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.ErrorTrait;

public class CopyServiceErrorsToOperationsTransformTest {
    @Test
    public void copiesErrors() {
        StructureShape errorShape1 = StructureShape.builder()
                .id("ns.foo#Error1")
                .addTrait(new ErrorTrait("client"))
                .build();
        StructureShape errorShape2 = StructureShape.builder()
                .id("ns.foo#Error2")
                .addTrait(new ErrorTrait("client"))
                .build();
        OperationShape operation1 = OperationShape.builder()
                .id("smithy.example#Operation1")
                .addError(errorShape1)
                .build();
        OperationShape operation2 = OperationShape.builder()
                .id("smithy.example#Operation2")
                .build();
        ServiceShape service = ServiceShape.builder()
                .id("ns.foo#Svc")
                .version("2017-01-17")
                .addError(errorShape2)
                .addOperation(operation1)
                .build();
        Model model = Model.builder()
                .addShapes(service, errorShape1, errorShape2, operation1, operation2)
                .build();
        Model result = ModelTransformer.create().copyServiceErrorsToOperations(model, service);

        // operation2 is not in the service closure so leave it alone.
        assertThat(operation2, Matchers.equalTo(result.expectShape(operation2.getId())));

        // Make sure service errors were copied to the operation bound within it.
        assertThat(result.expectShape(operation1.getId(), OperationShape.class).getErrors(),
                Matchers.containsInAnyOrder(errorShape1.getId(), errorShape2.getId()));
    }
}
