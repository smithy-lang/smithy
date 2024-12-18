/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.PaginatedTrait;

public class FlattenPaginationInfoTest {
    private static final ShapeId serviceId = ShapeId.from("smithy.example#PaginatedService");
    private static final ShapeId operationId = ShapeId.from("smithy.example#PaginatedOperation");

    @Test
    void compareTransform() {
        Model before = Model.assembler()
                .addImport(FlattenPaginationInfoTest.class.getResource("flatten-pagination-before.smithy"))
                .assemble()
                .unwrap();
        ServiceShape service = before.expectShape(serviceId).asServiceShape().get();
        Model result = ModelTransformer.create().flattenPaginationInfoIntoOperations(before, service);

        Shape resultService = result.expectShape(serviceId);
        assertFalse(resultService.hasTrait(PaginatedTrait.class));

        Shape resultOperation = result.expectShape(operationId);
        PaginatedTrait resultTrait = resultOperation.expectTrait(PaginatedTrait.class);
        assertEquals(resultTrait.getInputToken().get(), "nextToken");
        assertEquals(resultTrait.getOutputToken().get(), "nextToken");
        assertEquals(resultTrait.getPageSize().get(), "maxResults");
        assertEquals(resultTrait.getItems().get(), "foos");
    }
}
