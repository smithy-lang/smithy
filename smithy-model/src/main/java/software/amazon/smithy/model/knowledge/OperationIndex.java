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

package software.amazon.smithy.model.knowledge;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeIndex;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.ToShapeId;

/**
 * Index of operation IDs to their resolved input, output, and error
 * structures.
 *
 * <p>This index performs no validation that the input, output, and
 * errors actually reference valid structures.
 */
public final class OperationIndex implements KnowledgeIndex {
    private final Map<ShapeId, StructureShape> inputs = new HashMap<>();
    private final Map<ShapeId, StructureShape> outputs = new HashMap<>();
    private final Map<ShapeId, List<StructureShape>> errors = new HashMap<>();

    public OperationIndex(Model model) {
        var index = model.getShapeIndex();
        index.shapes(OperationShape.class).forEach(operation -> {
            operation.getInput()
                    .flatMap(id -> getStructure(index, id))
                    .ifPresent(shape -> inputs.put(operation.getId(), shape));
            operation.getOutput()
                    .flatMap(id -> getStructure(index, id))
                    .ifPresent(shape -> outputs.put(operation.getId(), shape));
            errors.put(operation.getId(),
                       operation.getErrors()
                               .stream()
                               .map(e -> getStructure(index, e))
                               .filter(Optional::isPresent)
                               .map(Optional::get)
                               .collect(Collectors.toList()));
        });
    }

    public Optional<StructureShape> getInput(ToShapeId operation) {
        return Optional.ofNullable(inputs.get(operation.toShapeId()));
    }

    public Optional<StructureShape> getOutput(ToShapeId operation) {
        return Optional.ofNullable(outputs.get(operation.toShapeId()));
    }

    public List<StructureShape> getErrors(ToShapeId operation) {
        return errors.getOrDefault(operation.toShapeId(), List.of());
    }

    private Optional<StructureShape> getStructure(ShapeIndex index, ToShapeId id) {
        return index.getShape(id.toShapeId()).flatMap(Shape::asStructureShape);
    }
}
