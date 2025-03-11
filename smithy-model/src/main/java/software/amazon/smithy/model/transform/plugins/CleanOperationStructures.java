/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
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
    public Model onRemove(ModelTransformer transformer, Collection<Shape> removed, Set<ShapeId> removedIds, Model model) {
        return transformer.replaceShapes(model, getModifiedOperations(model, removedIds));
    }

    private Collection<Shape> getModifiedOperations(Model model, Set<ShapeId> removed) {
        return model.shapes(OperationShape.class)
                .flatMap(operation -> {
                    OperationShape result = transformErrors(removed, operation);
                    result = transformInput(removed, result);
                    result = transformOutput(removed, result);
                    return result.equals(operation) ? Stream.empty() : Stream.of(result);
                })
                .collect(Collectors.toList());
    }

    private OperationShape transformInput(Set<ShapeId> removed, OperationShape operation) {
        if (removed.contains(operation.getInputShape())) {
            return operation.toBuilder().input(null).build();
        }
        return operation;
    }

    private OperationShape transformOutput(Set<ShapeId> removed, OperationShape operation) {
        if (removed.contains(operation.getOutputShape())) {
            return operation.toBuilder().output(null).build();
        }
        return operation;
    }

    private OperationShape transformErrors(Set<ShapeId> removed, OperationShape operation) {
        Set<ShapeId> errors = new HashSet<>(operation.getErrors());
        errors.removeAll(removed);

        if (new ArrayList<>(errors).equals(operation.getErrors())) {
            return operation;
        }

        return operation.toBuilder().errors(errors).build();
    }
}
