/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.transform;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.PaginatedIndex;
import software.amazon.smithy.model.knowledge.PaginationInfo;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.PaginatedTrait;

/**
 * Flattens pagination info from service shapes into operation-level pagination traits.
 */
final class FlattenPaginationInfo {

    private final ServiceShape service;

    FlattenPaginationInfo(ServiceShape service) {
        this.service = service;
    }

    public Model transform(ModelTransformer transformer, Model model) {
        Optional<PaginatedTrait> serviceLevelPagination = service.getTrait(PaginatedTrait.class);
        if (!serviceLevelPagination.isPresent()) {
            return model;
        }
        PaginatedIndex paginatedIndex = PaginatedIndex.of(model);

        // Merge service-level information into each operation's pagination trait.
        Set<Shape> updatedShapes = new HashSet<>();
        for (OperationShape operationShape : model.getOperationShapesWithTrait(PaginatedTrait.class)) {
            PaginationInfo paginationInfo = paginatedIndex.getPaginationInfo(service, operationShape).get();
            OperationShape updatedShape = operationShape.toBuilder()
                    .addTrait(paginationInfo.getPaginatedTrait())
                    .build();
            updatedShapes.add(updatedShape);
        }

        // Remove the paginated trait from the service as it's info has been flattened into the operations
        updatedShapes.add(service.toBuilder().removeTrait(PaginatedTrait.ID).build());

        return transformer.replaceShapes(model, updatedShapes);
    }
}
