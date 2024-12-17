/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.transform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.model.traits.ErrorTrait;
import software.amazon.smithy.model.traits.HttpErrorTrait;
import software.amazon.smithy.model.traits.HttpHeaderTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.utils.Pair;

/**
 * Deconflicts errors on operations that share the same status code by replacing
 * the conflicting errors with a synthetic error structure that contains hoisted
 * members that were bound to HTTP headers. The conflicting errors are added to
 * a union on the synthetic error.
 */
final class DeconflictErrorsWithSharedStatusCode {

    private final ServiceShape forService;

    DeconflictErrorsWithSharedStatusCode(ServiceShape forService) {
        this.forService = forService;
    }

    Model transform(ModelTransformer transformer, Model model) {
        // Copy any service errors to the operations to find all potential conflicts.
        model = transformer.copyServiceErrorsToOperations(model, forService);
        TopDownIndex topDownIndex = TopDownIndex.of(model);
        List<Shape> shapesToReplace = new ArrayList<>();

        for (OperationShape operation : topDownIndex.getContainedOperations(forService)) {
            OperationShape.Builder replacementOperation = operation.toBuilder();
            boolean replaceOperation = false;

            // Collect errors that share the same status code.
            Map<Integer, List<StructureShape>> statusCodesToErrors = new HashMap<>();
            for (ShapeId errorId : operation.getErrors()) {
                StructureShape error = model.expectShape(errorId, StructureShape.class);
                Integer statusCode = error.hasTrait(HttpErrorTrait.ID)
                        ? error.getTrait(HttpErrorTrait.class).get().getCode()
                        : error.getTrait(ErrorTrait.class).get().getDefaultHttpStatusCode();
                statusCodesToErrors.computeIfAbsent(statusCode, k -> new ArrayList<>()).add(error);
            }

            // Create union error for errors with same status code.
            for (Map.Entry<Integer, List<StructureShape>> statusCodeToErrors : statusCodesToErrors.entrySet()) {
                if (statusCodeToErrors.getValue().size() > 1) {
                    replaceOperation = true;
                    List<StructureShape> errors = statusCodeToErrors.getValue();
                    // Create a new top-level synthetic error and all the shapes that need replaced for it.
                    Pair<Shape, List<Shape>> syntheticErrorPair = synthesizeErrorUnion(operation.getId().getName(),
                            statusCodeToErrors.getKey(),
                            errors);
                    for (StructureShape error : errors) {
                        replacementOperation.removeError(error.getId());
                    }
                    replacementOperation.addError(syntheticErrorPair.getLeft());
                    shapesToReplace.add(syntheticErrorPair.getLeft());
                    shapesToReplace.addAll(syntheticErrorPair.getRight());
                }
            }
            // Replace the operation if it has been updated with a synthetic error.
            if (replaceOperation) {
                replacementOperation.build();
                shapesToReplace.add(replacementOperation.build());
            }
        }

        return transformer.replaceShapes(model, shapesToReplace);
    }

    // Return synthetic error, along with any updated shapes.
    private Pair<Shape, List<Shape>> synthesizeErrorUnion(
            String operationName,
            Integer statusCode,
            List<StructureShape> errors
    ) {
        List<Shape> replacementShapes = new ArrayList<>();
        StructureShape.Builder errorResponse = StructureShape.builder();
        ShapeId errorResponseId = ShapeId.fromParts(forService.getId().getNamespace(),
                operationName + statusCode + "Error");
        errorResponse.id(errorResponseId);
        errorResponse.addTraits(getErrorTraitsFromStatusCode(statusCode));
        Map<String, HttpHeaderTrait> headerTraitMap = new HashMap<>();
        UnionShape.Builder errorUnion = UnionShape.builder()
                .id(
                        ShapeId.fromParts(errorResponseId.getNamespace(), errorResponseId.getName() + "Content"));
        for (StructureShape error : errors) {
            StructureShape newError = createNewError(error, headerTraitMap);
            replacementShapes.add(newError);
            MemberShape newErrorMember = MemberShape.builder()
                    .id(errorUnion.getId().withMember(newError.getId().getName()))
                    .target(newError.getId())
                    .build();
            replacementShapes.add(newErrorMember);
            errorUnion.addMember(newErrorMember);
        }
        UnionShape union = errorUnion.build();
        replacementShapes.add(union);
        errorResponse.addMember(MemberShape.builder()
                .id(errorResponseId.withMember("errorUnion"))
                .target(union.getId())
                .build());
        // Add members with hoisted HttpHeader traits.
        for (Map.Entry<String, HttpHeaderTrait> entry : headerTraitMap.entrySet()) {
            errorResponse.addMember(MemberShape.builder()
                    .id(errorResponseId.withMember(entry.getKey()))
                    .addTrait(entry.getValue())
                    .target("smithy.api#String")
                    .build());
        }
        StructureShape built = errorResponse.build();
        return Pair.of(built, replacementShapes);
    }

    private StructureShape createNewError(StructureShape oldError, Map<String, HttpHeaderTrait> headerMap) {
        StructureShape.Builder newErrorBuilder = oldError.toBuilder().clearMembers();
        for (MemberShape member : oldError.getAllMembers().values()) {
            String name = member.getMemberName();
            // Collect HttpHeaderTraits to hoist.
            if (member.hasTrait(HttpHeaderTrait.ID)) {
                HttpHeaderTrait newTrait = member.expectTrait(HttpHeaderTrait.class);
                HttpHeaderTrait previousTrait = headerMap.put(name, newTrait);
                if (previousTrait != null && !previousTrait.equals(newTrait)) {
                    throw new ModelTransformException("Conflicting header when de-conflicting");
                }
            } else {
                newErrorBuilder.addMember(member.toBuilder().id(newErrorBuilder.getId().withMember(name)).build());
            }
        }
        return newErrorBuilder.build();
    }

    private List<Trait> getErrorTraitsFromStatusCode(Integer statusCode) {
        List<Trait> traits = new ArrayList<>();
        if (statusCode >= 400 && statusCode < 500) {
            traits.add(new ErrorTrait("client"));
        } else {
            traits.add(new ErrorTrait("server"));
        }
        traits.add(new HttpErrorTrait(statusCode));
        return traits;
    }
}
