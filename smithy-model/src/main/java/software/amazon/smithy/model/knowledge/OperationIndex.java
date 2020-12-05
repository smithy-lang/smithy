/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.ToShapeId;
import software.amazon.smithy.utils.ListUtils;

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
        model.shapes(OperationShape.class).forEach(operation -> {
            operation.getInput()
                    .flatMap(id -> getStructure(model, id))
                    .ifPresent(shape -> inputs.put(operation.getId(), shape));
            operation.getOutput()
                    .flatMap(id -> getStructure(model, id))
                    .ifPresent(shape -> outputs.put(operation.getId(), shape));
            errors.put(operation.getId(),
                       operation.getErrors()
                               .stream()
                               .map(e -> getStructure(model, e))
                               .filter(Optional::isPresent)
                               .map(Optional::get)
                               .collect(Collectors.toList()));
        });
    }

    public static OperationIndex of(Model model) {
        return model.getKnowledge(OperationIndex.class, OperationIndex::new);
    }

    /**
     * Gets the optional input structure of an operation.
     *
     * @param operation Operation to get the input structure of.
     * @return Returns the optional operation input structure.
     */
    public Optional<StructureShape> getInput(ToShapeId operation) {
        return Optional.ofNullable(inputs.get(operation.toShapeId()));
    }

    /**
     * Gets the input members of an operation as a map of member names
     * to {@link MemberShape}.
     *
     * <p>The return map is ordered using the same order defined in
     * the model. If the operation has no input, an empty map is returned.
     *
     * @param operation Operation to get the input members of.
     * @return Returns the map of members, or an empty map.
     */
    public Map<String, MemberShape> getInputMembers(ToShapeId operation) {
        return getInput(operation)
                .map(input -> input.getAllMembers())
                .orElse(Collections.emptyMap());
    }

    /**
     * Returns true if the given structure is used as input by any
     * operation in the model.
     *
     * @param structureId Structure to check.
     * @return Returns true if the structure is used as input.
     */
    public boolean isInputStructure(ToShapeId structureId) {
        ShapeId id = structureId.toShapeId();

        for (StructureShape shape : inputs.values()) {
            if (shape.getId().equals(id)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Gets the optional output structure of an operation.
     *
     * @param operation Operation to get the output structure of.
     * @return Returns the optional operation output structure.
     */
    public Optional<StructureShape> getOutput(ToShapeId operation) {
        return Optional.ofNullable(outputs.get(operation.toShapeId()));
    }

    /**
     * Gets the output members of an operation as a map of member names
     * to {@link MemberShape}.
     *
     * <p>The return map is ordered using the same order defined in
     * the model. If the operation has no output, an empty map is returned.
     *
     * @param operation Operation to get the output members of.
     * @return Returns the map of members, or an empty map.
     */
    public Map<String, MemberShape> getOutputMembers(ToShapeId operation) {
        return getOutput(operation)
                .map(output -> output.getAllMembers())
                .orElse(Collections.emptyMap());
    }

    /**
     * Returns true if the given structure is used as output by any
     * operation in the model.
     *
     * @param structureId Structure to check.
     * @return Returns true if the structure is used as output.
     */
    public boolean isOutputStructure(ToShapeId structureId) {
        ShapeId id = structureId.toShapeId();

        for (StructureShape shape : outputs.values()) {
            if (shape.getId().equals(id)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Gets the list of error structures defined on an operation.
     *
     * <p>An empty list is returned if the operation is not found or
     * has no errors.
     *
     * @param operation Operation to get the errors of.
     * @return Returns the list of error structures, or an empty list.
     */
    public List<StructureShape> getErrors(ToShapeId operation) {
        return errors.getOrDefault(operation.toShapeId(), ListUtils.of());
    }

    private Optional<StructureShape> getStructure(Model model, ToShapeId id) {
        return model.getShape(id.toShapeId()).flatMap(Shape::asStructureShape);
    }
}
