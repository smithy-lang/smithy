/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
