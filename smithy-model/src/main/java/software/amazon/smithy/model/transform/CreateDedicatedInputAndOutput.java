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

import java.util.ArrayList;
import java.util.List;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.NeighborProviderIndex;
import software.amazon.smithy.model.knowledge.OperationIndex;
import software.amazon.smithy.model.neighbor.NeighborProvider;
import software.amazon.smithy.model.neighbor.Relationship;
import software.amazon.smithy.model.neighbor.RelationshipDirection;
import software.amazon.smithy.model.neighbor.RelationshipType;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.InputTrait;
import software.amazon.smithy.model.traits.OutputTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.UnitTypeTrait;
import software.amazon.smithy.model.traits.ephemeral.OriginalShapeIdTrait;

/**
 * @see ModelTransformer#createDedicatedInputAndOutput(Model, String, String)
 */
final class CreateDedicatedInputAndOutput {
    private final String inputSuffix;
    private final String outputSuffix;

    CreateDedicatedInputAndOutput(String inputSuffix, String outputSuffix) {
        this.inputSuffix = inputSuffix;
        this.outputSuffix = outputSuffix;
    }

    Model transform(ModelTransformer transformer, Model model) {
        NeighborProvider reverse = NeighborProviderIndex.of(model).getReverseProvider();
        OperationIndex operationIndex = OperationIndex.of(model);

        List<Shape> updates = new ArrayList<>();
        List<Shape> toRemove = new ArrayList<>();
        for (OperationShape operation : model.getOperationShapes()) {
            StructureShape input = operationIndex.expectInputShape(operation);
            StructureShape updatedInput = createdUpdatedInput(model, operation, input, reverse);
            StructureShape output = operationIndex.expectOutputShape(operation);
            StructureShape updatedOutput = createdUpdatedOutput(model, operation, output, reverse);

            if (!updatedInput.equals(input) || !updatedOutput.equals(output)) {
                OperationShape.Builder builder = operation.toBuilder();
                if (!updatedInput.equals(input)) {
                    updates.add(updatedInput);
                    builder.input(updatedInput);
                    // If the ID changed and the original is no longer referenced, then remove it.
                    if (!input.getId().equals(updatedInput.getId())
                            && isSingularReference(reverse, input, RelationshipType.INPUT)) {
                        toRemove.add(input);
                    }
                }
                if (!updatedOutput.equals(output)) {
                    updates.add(updatedOutput);
                    builder.output(updatedOutput);
                    // If the ID changed and the original is no longer referenced, then remove it.
                    if (!output.getId().equals(updatedOutput.getId())
                            && isSingularReference(reverse, output, RelationshipType.OUTPUT)) {
                        toRemove.add(output);
                    }
                }
                updates.add(builder.build());
            }
        }

        // Replace the operations and add new shapes.
        Model result = transformer.replaceShapes(model, updates);

        // Remove no longer referenced shapes.
        return transformer.removeShapes(result, toRemove);
    }

    private StructureShape createdUpdatedInput(
            Model model,
            OperationShape operation,
            StructureShape input,
            NeighborProvider reverse
    ) {
        if (input.hasTrait(InputTrait.class)) {
            return renameShapeIfNeeded(model, input, operation, inputSuffix);
        } else if (isDedicatedHeuristic(operation, input, reverse, RelationshipType.INPUT)) {
            InputTrait trait = new InputTrait();
            return renameShapeIfNeeded(model, input.toBuilder().addTrait(trait).build(), operation, inputSuffix);
        } else {
            return createSyntheticShape(model, operation, inputSuffix, input, new InputTrait());
        }
    }

    private StructureShape createdUpdatedOutput(
            Model model,
            OperationShape operation,
            StructureShape output,
            NeighborProvider reverse
    ) {
        if (output.hasTrait(OutputTrait.class)) {
            return renameShapeIfNeeded(model, output, operation, outputSuffix);
        } else if (isDedicatedHeuristic(operation, output, reverse, RelationshipType.OUTPUT)) {
            OutputTrait trait = new OutputTrait();
            return renameShapeIfNeeded(model, output.toBuilder().addTrait(trait).build(), operation, outputSuffix);
        } else {
            return createSyntheticShape(model, operation, outputSuffix, output, new OutputTrait());
        }
    }

    private StructureShape renameShapeIfNeeded(
            Model model,
            StructureShape struct,
            OperationShape operation,
            String suffix
    ) {
        // Check if the shape already has the desired name.
        ShapeId expectedName = ShapeId.fromParts(operation.getId().getNamespace(),
                                                 operation.getId().getName() + suffix);
        if (struct.getId().equals(expectedName)) {
            return struct;
        }

        ShapeId newId = createSyntheticShapeId(model, operation, suffix);
        return struct.toBuilder()
                .id(newId)
                .addTrait(new OriginalShapeIdTrait(struct.getId()))
                .build();
    }

    private boolean isDedicatedHeuristic(
            OperationShape operation,
            StructureShape struct,
            NeighborProvider reverse,
            RelationshipType expected
    ) {
        // Only assume that a shape is dedicated to the operation its name starts with the operation name.
        if (!struct.getId().getName().startsWith(operation.getId().getName())) {
            return false;
        }

        // Check if the shape is only referenced as input or output.
        return isSingularReference(reverse, struct, expected);
    }

    private boolean isSingularReference(NeighborProvider reverse, Shape shape, RelationshipType expected) {
        int totalDirectedEdges = 0;

        // We need to exclude inverted edges like MEMBER_CONTAINER, and only look for directed
        // edges like INPUT and OUTPUT.
        for (Relationship rel : reverse.getNeighbors(shape)) {
            if (rel.getRelationshipType().getDirection() == RelationshipDirection.DIRECTED) {
                totalDirectedEdges++;
                if (rel.getRelationshipType() != expected) {
                    return false;
                }
            }
        }

        return totalDirectedEdges == 1;
    }

    private static StructureShape createSyntheticShape(
            Model model,
            OperationShape operation,
            String suffix,
            StructureShape source,
            Trait inputOutputTrait
    ) {
        ShapeId newId = createSyntheticShapeId(model, operation, suffix);

        // Special handling for copying unit types (that, you don't copy unit types)
        StructureShape.Builder builder = source.getId().equals(UnitTypeTrait.UNIT)
                ? StructureShape.builder()
                : source.toBuilder();

        builder.id(newId);
        builder.addTrait(inputOutputTrait);

        if (!newId.equals(source.getId())) {
            builder.addTrait(new OriginalShapeIdTrait(source.getId()));
        }

        return builder.build();
    }

    private static ShapeId createSyntheticShapeId(
            Model model,
            OperationShape operation,
            String suffix
    ) {
        // Synthesize an input shape as a dedicated copy of the existing input.
        ShapeId newId = ShapeId.fromParts(operation.getId().getNamespace(),
                                          operation.getId().getName() + suffix);

        if (model.getShapeIds().contains(newId)) {
            ShapeId deconflicted = resolveConflict(newId, suffix);
            if (model.getShapeIds().contains(deconflicted)) {
                throw new ModelTransformException(String.format(
                        "Unable to generate a synthetic %s shape for the %s operation. The %s shape already exists "
                        + "in the model, and the conflict resolver also returned a shape ID that already exists: %s",
                        suffix,
                        operation.getId(),
                        newId,
                        deconflicted));
            }
            newId = deconflicted;
        }

        return newId;
    }

    private static ShapeId resolveConflict(ShapeId id, String suffix) {
        // Say GetFooRequest exists. This then returns GetFooOperationRequest.
        String updatedName = id.getName().replace(suffix, "Operation" + suffix);
        return ShapeId.fromParts(id.getNamespace(), updatedName);
    }
}
