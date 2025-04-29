/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.transform.plugins;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
        Set<ShapeId> removedIds = new HashSet<>();
        for (Shape shape : removed) {
            removedIds.add(shape.getId());
        }

        List<Shape> modifiedShapes = new ArrayList<>();
        for (OperationShape operation : model.getOperationShapes()) {
            OperationShape.Builder builder = transformInput(removedIds, operation);
            builder = transformOutput(removedIds, operation, builder);
            builder = transformErrors(removedIds, operation, builder);
            if (builder != null) {
                modifiedShapes.add(builder.build());
            }
        }
        return modifiedShapes;
    }

    private OperationShape.Builder transformInput(Set<ShapeId> removed, OperationShape operation) {
        if (removed.contains(operation.getInputShape())) {
            OperationShape.Builder builder = operation.toBuilder();
            builder.input(null);
            return builder;
        }
        return null;
    }

    private OperationShape.Builder transformOutput(
            Set<ShapeId> removed,
            OperationShape operation,
            OperationShape.Builder builder
    ) {
        if (removed.contains(operation.getOutputShape())) {
            if (builder == null) {
                builder = operation.toBuilder();
            }
            builder.output(null);
            return builder;
        }
        return builder;
    }

    private OperationShape.Builder transformErrors(
            Set<ShapeId> removed,
            OperationShape operation,
            OperationShape.Builder builder
    ) {
        Set<ShapeId> errors = new HashSet<>(operation.getErrors());
        errors.removeAll(removed);

        if (errors.size() != operation.getErrors().size()) {
            if (builder == null) {
                builder = operation.toBuilder();
            }
            builder.errors(errors);
            return builder;
        }

        return builder;
    }
}
