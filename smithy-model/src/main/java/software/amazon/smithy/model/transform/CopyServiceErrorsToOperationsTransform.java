/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.transform;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Copies errors from a service onto each operation bound to the service.
 */
final class CopyServiceErrorsToOperationsTransform {

    private final ServiceShape forService;

    CopyServiceErrorsToOperationsTransform(ServiceShape forService) {
        this.forService = forService;
    }

    Model transform(ModelTransformer transformer, Model model) {
        if (forService.getErrors().isEmpty()) {
            return model;
        }

        Set<Shape> toReplace = new HashSet<>();
        TopDownIndex topDownIndex = TopDownIndex.of(model);
        for (OperationShape operation : topDownIndex.getContainedOperations(forService)) {
            Set<ShapeId> errors = new LinkedHashSet<>(operation.getErrors());
            errors.addAll(forService.getErrors());
            toReplace.add(operation.toBuilder().errors(errors).build());
        }

        return transformer.replaceShapes(model, toReplace);
    }
}
