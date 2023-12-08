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

package software.amazon.smithy.model.knowledge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.ExpectationNotMetException;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.ToShapeId;
import software.amazon.smithy.model.traits.InputTrait;
import software.amazon.smithy.model.traits.OutputTrait;
import software.amazon.smithy.model.traits.UnitTypeTrait;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.SetUtils;

/**
 * Index of operation IDs to their resolved input, output, and error
 * structures.
 *
 * <p>This index performs no validation that the input, output, and
 * errors actually reference valid structures. Such operation inputs,
 * outputs, and errors may be discarded as if they do not exist.
 */
public final class OperationIndex implements KnowledgeIndex {
    private final Map<ShapeId, StructureShape> inputs = new HashMap<>();
    private final Map<ShapeId, StructureShape> outputs = new HashMap<>();
    private final Map<ShapeId, List<StructureShape>> errors = new HashMap<>();
    private final Map<ShapeId, Set<OperationShape>> boundInputOperations = new HashMap<>();
    private final Map<ShapeId, Set<OperationShape>> boundOutputOperations = new HashMap<>();
    private final Map<ShapeId, Set<Shape>> boundErrorShapes = new HashMap<>();

    public OperationIndex(Model model) {
        for (OperationShape operation : model.getOperationShapes()) {
            getStructure(model, operation.getInputShape()).ifPresent(shape -> {
                inputs.put(operation.getId(), shape);
                boundInputOperations.computeIfAbsent(shape.getId(), id -> new HashSet<>()).add(operation);
            });
            getStructure(model, operation.getOutputShape()).ifPresent(shape -> {
                outputs.put(operation.getId(), shape);
                boundOutputOperations.computeIfAbsent(shape.getId(), id -> new HashSet<>()).add(operation);
            });
            addErrorsFromShape(model, operation.getId(), operation.getErrors());
        }

        for (ServiceShape service : model.getServiceShapes()) {
            addErrorsFromShape(model, service.getId(), service.getErrors());
        }
    }

    private void addErrorsFromShape(Model model, ShapeId source, List<ShapeId> errorShapeIds) {
        List<StructureShape> errorShapes = new ArrayList<>(errorShapeIds.size());
        Shape sourceShape = model.expectShape(source);
        for (ShapeId target : errorShapeIds) {
            model.getShape(target).flatMap(Shape::asStructureShape).ifPresent(errorShapes::add);
            boundErrorShapes.computeIfAbsent(target, id -> new HashSet<>()).add(sourceShape);
        }
        errors.put(source, errorShapes);
    }

    public static OperationIndex of(Model model) {
        return model.getKnowledge(OperationIndex.class, OperationIndex::new);
    }

    /**
     * Gets the optional input structure of an operation, and returns an
     * empty optional if the input targets {@code smithy.api#Unit}.
     *
     * @param operation Operation to get the input structure of.
     * @return Returns the optional operation input structure.
     */
    public Optional<StructureShape> getInput(ToShapeId operation) {
        return getInputShape(operation).filter(shape -> !shape.getId().equals(UnitTypeTrait.UNIT));
    }

    /**
     * Gets the optional input structure of an operation.
     *
     * <p>Operations in the model always have input. This operation will
     * only return an empty optional if the given operation shape cannot
     * be found in the model or if it is not an operation shape.
     *
     * @param operation Operation to get the input structure of.
     * @return Returns the optional operation input structure.
     */
    public Optional<StructureShape> getInputShape(ToShapeId operation) {
        return Optional.ofNullable(inputs.get(operation.toShapeId()));
    }

    /**
     * Gets the input shape of an operation, and returns Smithy's Unit type
     * trait if the operation has no meaningful input.
     *
     * <p>In general, this method should be used instead of
     * {@link #getInputShape(ToShapeId)} when getting the input of operations
     * that are known to exist.
     *
     * @param operation Operation to get the input of.
     * @return Returns the input shape of the operation.
     * @throws ExpectationNotMetException if the operation shape cannot be found.
     */
    public StructureShape expectInputShape(ToShapeId operation) {
        return getInputShape(operation).orElseThrow(() -> new ExpectationNotMetException(
                "Cannot get the input of `" + operation.toShapeId() + "` because "
                + "it is not an operation shape in the model.",
                SourceLocation.NONE));
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
        return getInputShape(operation)
                .map(input -> input.getAllMembers())
                .orElse(Collections.emptyMap());
    }

    /**
     * Returns true if the given structure is used as input by any
     * operation in the model or is marked with the input trait.
     *
     * @param structureId Structure to check.
     * @return Returns true if the structure is used as input.
     */
    public boolean isInputStructure(ToShapeId structureId) {
        if (structureId instanceof Shape && ((Shape) structureId).hasTrait(InputTrait.class)) {
            return true;
        }

        ShapeId id = structureId.toShapeId();

        for (StructureShape shape : inputs.values()) {
            if (shape.getId().equals(id)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Gets all the operations that bind the given shape as input.
     *
     * @param input The structure that may be used as input.
     * @return Returns a set of operations that bind the given input shape.
     */
    public Set<OperationShape> getInputBindings(ToShapeId input) {
        return SetUtils.copyOf(boundInputOperations.getOrDefault(input.toShapeId(), Collections.emptySet()));
    }

    /**
     * Gets the optional output structure of an operation, and returns an
     * empty optional if the output targets {@code smithy.api#Unit}.
     *
     * @param operation Operation to get the output structure of.
     * @return Returns the optional operation output structure.
     */
    public Optional<StructureShape> getOutput(ToShapeId operation) {
        return getOutputShape(operation).filter(shape -> !shape.getId().equals(UnitTypeTrait.UNIT));
    }

    /**
     * Gets the optional output structure of an operation.
     *
     * <p>Operations in the model always have output. This operation will
     * only return an empty optional if the given operation shape cannot
     * be found in the model or if it is not an operation shape.
     *
     * @param operation Operation to get the output structure of.
     * @return Returns the optional operation output structure.
     */
    public Optional<StructureShape> getOutputShape(ToShapeId operation) {
        return Optional.ofNullable(outputs.get(operation.toShapeId()));
    }

    /**
     * Gets the output shape of an operation, and returns Smithy's unit type
     * trait if the operation has no meaningful output.
     *
     * <p>In general, this method should be used instead of
     * {@link #getOutputShape(ToShapeId)} when getting the output of operations
     * that are known to exist.
     *
     * @param operation Operation to get the output of.
     * @return Returns the output shape of the operation.
     * @throws ExpectationNotMetException if the operation shape cannot be found.
     */
    public StructureShape expectOutputShape(ToShapeId operation) {
        return getOutputShape(operation).orElseThrow(() -> new ExpectationNotMetException(
                "Cannot get the output of `" + operation.toShapeId() + "` because "
                + "it is not an operation shape in the model.",
                SourceLocation.NONE));
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
        return getOutputShape(operation)
                .map(output -> output.getAllMembers())
                .orElse(Collections.emptyMap());
    }

    /**
     * Returns true if the given structure is used as output by any
     * operation in the model or is marked with the output trait.
     *
     * @param structureId Structure to check.
     * @return Returns true if the structure is used as output.
     */
    public boolean isOutputStructure(ToShapeId structureId) {
        if (structureId instanceof Shape && ((Shape) structureId).hasTrait(OutputTrait.class)) {
            return true;
        }

        ShapeId id = structureId.toShapeId();

        for (StructureShape shape : outputs.values()) {
            if (shape.getId().equals(id)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Gets all the operations that bind the given shape as output.
     *
     * @param output The structure that may be used as output.
     * @return Returns a set of operations that bind the given output shape.
     */
    public Set<OperationShape> getOutputBindings(ToShapeId output) {
        return SetUtils.copyOf(boundOutputOperations.getOrDefault(output.toShapeId(), Collections.emptySet()));
    }

    /**
     * Gets the list of error structures defined on an operation.
     *
     * <p>An empty list is returned if the operation is not found or
     * has no errors.
     *
     * @param operation Operation to get the errors of.
     * @return Returns the list of error structures, or an empty list.
     * @see #getErrors(ToShapeId, ToShapeId) to get errors that inherit from a service.
     */
    public List<StructureShape> getErrors(ToShapeId operation) {
        return errors.getOrDefault(operation.toShapeId(), ListUtils.of());
    }

    /**
     * Gets the list of error structures defined on an operation,
     * including any common errors inherited from a service shape.
     *
     * <p>An empty list is returned if the operation is not found or
     * has no errors.
     *
     * @param service Service shape to inherit common errors from.
     * @param operation Operation to get the errors of.
     * @return Returns the list of error structures, or an empty list.
     */
    public List<StructureShape> getErrors(ToShapeId service, ToShapeId operation) {
        Set<StructureShape> result = new LinkedHashSet<>(getErrors(service));
        result.addAll(getErrors(operation));
        return new ArrayList<>(result);
    }

    private Optional<StructureShape> getStructure(Model model, ToShapeId id) {
        return model.getShape(id.toShapeId()).flatMap(Shape::asStructureShape);
    }

    /**
     * Gets all the operations and services that bind the given shape as an error.
     *
     * @param error The structure that may be used as an error.
     * @return Returns a set of operations and services that bind the given error shape.
     */
    public Set<Shape> getErrorBindings(ToShapeId error) {
        return SetUtils.copyOf(boundErrorShapes.getOrDefault(error.toShapeId(), Collections.emptySet()));
    }
}
