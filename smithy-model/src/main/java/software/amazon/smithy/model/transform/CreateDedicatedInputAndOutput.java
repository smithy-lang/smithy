/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.transform;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.NeighborProviderIndex;
import software.amazon.smithy.model.knowledge.OperationIndex;
import software.amazon.smithy.model.neighbor.NeighborProvider;
import software.amazon.smithy.model.neighbor.RelationshipDirection;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.InputTrait;
import software.amazon.smithy.model.traits.OutputTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.UnitTypeTrait;
import software.amazon.smithy.model.traits.synthetic.OriginalShapeIdTrait;

/**
 * @see ModelTransformer#createDedicatedInputAndOutput(Model, String, String)
 */
final class CreateDedicatedInputAndOutput {
    private static final Logger LOGGER = Logger.getLogger(CreateDedicatedInputAndOutput.class.getName());
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

            boolean inputChanged = !input.equals(updatedInput);
            boolean outputChanged = !output.equals(updatedOutput);

            if (!inputChanged && !outputChanged) {
                continue;
            }

            OperationShape.Builder builder = operation.toBuilder();
            if (inputChanged) {
                LOGGER.fine(() -> String.format("Updating operation input of %s from %s to %s",
                        operation.getId(),
                        input.getId(),
                        updatedInput.getId()));
                updates.add(updatedInput);
                builder.input(updatedInput);
                // If the ID changed and the original is no longer referenced, then remove it.
                boolean idChanged = !input.getId().equals(updatedInput.getId());
                if (idChanged && isSingularReference(reverse, input, operation)) {
                    toRemove.add(input);
                    LOGGER.fine("Removing now unused input shape " + input.getId());
                }
            }
            if (outputChanged) {
                LOGGER.fine(() -> String.format("Updating operation output of %s from %s to %s",
                        operation.getId(),
                        output.getId(),
                        updatedOutput.getId()));
                updates.add(updatedOutput);
                builder.output(updatedOutput);
                // If the ID changed and the original is no longer referenced, then remove it.
                boolean idChanged = !output.getId().equals(updatedOutput.getId());
                if (idChanged && isSingularReference(reverse, output, operation)) {
                    toRemove.add(output);
                    LOGGER.fine("Removing now unused output shape " + output.getId());
                }
            }
            updates.add(builder.build());
        }

        // Remove no longer referenced shapes.
        Model result = transformer.removeShapes(model, toRemove);

        // Replace the operations and add new shapes.
        return transformer.replaceShapes(result, updates);
    }

    private StructureShape createdUpdatedInput(
            Model model,
            OperationShape operation,
            StructureShape input,
            NeighborProvider reverse
    ) {
        if (input.hasTrait(InputTrait.class)) {
            return renameShapeIfNeeded(model, input, operation, inputSuffix);
        } else if (isDedicatedHeuristic(operation, input, reverse)) {
            LOGGER.fine(() -> "Attaching the @input trait to " + input.getId());
            InputTrait trait = new InputTrait(input.getSourceLocation());
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
        } else if (isDedicatedHeuristic(operation, output, reverse)) {
            LOGGER.fine(() -> "Attaching the @output trait to " + output.getId());
            OutputTrait trait = new OutputTrait(output.getSourceLocation());
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

        LOGGER.fine(() -> "Renaming " + struct.getId() + " to " + expectedName);
        ShapeId newId = createSyntheticShapeId(model, operation, suffix);

        return struct.toBuilder()
                .id(newId)
                .addTrait(new OriginalShapeIdTrait(struct.getId()))
                .build();
    }

    private boolean isDedicatedHeuristic(OperationShape operation, StructureShape struct, NeighborProvider reverse) {
        // Only assume that a shape is dedicated to the operation its name starts with the operation name.
        if (!struct.getId().getName().startsWith(operation.getId().getName())) {
            return false;
        }

        // Check if the shape is only referenced as input or output.
        return isSingularReference(reverse, struct, operation);
    }

    private boolean isSingularReference(NeighborProvider reverse, Shape shape, Shape expectedReferencingShape) {
        // We need to exclude inverted edges like MEMBER_CONTAINER, and only look for directed
        // edges to the expected shape.
        return reverse.getNeighbors(shape)
                .stream()
                .filter(relationship -> relationship.getDirection() == RelationshipDirection.DIRECTED)
                .allMatch(relationship -> relationship.getShape().equals(expectedReferencingShape));
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

        builder.source(source.getSourceLocation());
        builder.id(newId);
        builder.addTrait(inputOutputTrait);

        if (!newId.equals(source.getId())) {
            builder.addTrait(new OriginalShapeIdTrait(source.getId()));
        }

        LOGGER.fine(() -> "Creating synthetic " + inputOutputTrait.toShapeId().getName() + " shape " + newId);
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
        LOGGER.fine(() -> "Deconflicting synthetic ID from " + id + " to use name " + updatedName);
        return ShapeId.fromParts(id.getNamespace(), updatedName);
    }
}
