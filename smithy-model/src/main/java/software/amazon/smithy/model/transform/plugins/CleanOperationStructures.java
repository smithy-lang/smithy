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

package software.amazon.smithy.model.transform.plugins;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.transform.ModelTransformer;
import software.amazon.smithy.model.transform.ModelTransformerPlugin;

/**
 * Removes references to removed structures from operation
 * input, output, and errors.
 */
public final class CleanOperationStructures implements ModelTransformerPlugin {
    @Override
    public Model onRemove(ModelTransformer transformer, Collection<Shape> removed, Model model) {
        return transformer.replaceShapes(model, getModifiedOperations(model, removed));
    }

    private Collection<Shape> getModifiedOperations(Model model, Collection<Shape> removed) {
        return model.shapes(OperationShape.class)
                .flatMap(operation -> {
                    OperationShape result = transformErrors(removed, operation);
                    result = transformInput(removed, result);
                    result = transformOutput(removed, result);
                    return result.equals(operation) ? Stream.empty() : Stream.of(result);
                })
                .collect(Collectors.toList());
    }

    private OperationShape transformInput(Collection<Shape> removed, OperationShape operation) {
        for (Shape remove : removed) {
            if (remove.getId().equals(operation.getInputShape())) {
                return operation.toBuilder().input(null).build();
            }
        }
        return operation;
    }

    private OperationShape transformOutput(Collection<Shape> removed, OperationShape operation) {
        for (Shape remove : removed) {
            if (remove.getId().equals(operation.getOutputShape())) {
                return operation.toBuilder().output(null).build();
            }
        }
        return operation;
    }

    private OperationShape transformErrors(Collection<Shape> removed, OperationShape operation) {
        Set<ShapeId> errors = new HashSet<>(operation.getErrors());
        removed.forEach(shape -> errors.remove(shape.getId()));

        if (new ArrayList<>(errors).equals(operation.getErrors())) {
            return operation;
        }

        return operation.toBuilder().errors(errors).build();
    }
}
