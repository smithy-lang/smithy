/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.knowledge;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.NestedPropertiesTrait;

/**
 * Resolves the structure that carries per-instance resource state in the
 * input or output of a collection-bound operation, by looking through a list
 * member to the structure its elements target.
 *
 * <p>Resolution is explicit and opt-in through the {@code @nestedProperties}
 * trait applied to a member targeting a list of structures. Only the
 * {@code list} lifecycle operation supports automatic detection of the
 * carrying member in its output, and only when exactly one output member
 * targets a list of structures.
 */
final class CollectionElementResolver {

    private CollectionElementResolver() {}

    /**
     * Returns true if the operation is bound to the resource through the
     * {@code list} lifecycle.
     */
    static boolean isListLifecycle(ResourceShape resource, ShapeId operationId) {
        return resource.getList().filter(operationId::equals).isPresent();
    }

    /**
     * Returns true if the operation is bound to the resource through the
     * {@code list} lifecycle or the {@code collectionOperations} property.
     */
    static boolean isElementCarrier(ResourceShape resource, ShapeId operationId) {
        return isListLifecycle(resource, operationId)
                || resource.getCollectionOperations().contains(operationId);
    }

    /**
     * Returns true if any member of the given input or output shape is
     * marked with the {@code @nestedProperties} trait.
     */
    static boolean hasElementMarker(Model model, ShapeId ioShapeId) {
        return model.getShape(ioShapeId)
                .flatMap(Shape::asStructureShape)
                .map(shape -> shape.members().stream().anyMatch(m -> m.hasTrait(NestedPropertiesTrait.ID)))
                .orElse(false);
    }

    /**
     * Resolves the element structure explicitly marked by the
     * {@code @nestedProperties} trait on a member of the given input or
     * output shape that targets a list of structures.
     */
    static Optional<StructureShape> resolveExplicitElement(Model model, ShapeId ioShapeId) {
        Optional<StructureShape> ioShape = model.getShape(ioShapeId).flatMap(Shape::asStructureShape);
        if (!ioShape.isPresent()) {
            return Optional.empty();
        }
        for (MemberShape member : ioShape.get().members()) {
            if (member.hasTrait(NestedPropertiesTrait.ID)) {
                return resolveListElement(model, member);
            }
        }
        return Optional.empty();
    }

    /**
     * Automatically resolves the element structure of a {@code list}
     * lifecycle operation's output. Resolves only when exactly one output
     * member targets a list of structures; returns empty when the carrying
     * member is ambiguous or absent.
     */
    static Optional<StructureShape> resolveAutoOutputElement(Model model, OperationShape operation) {
        Optional<StructureShape> output = model.getShape(operation.getOutputShape())
                .flatMap(Shape::asStructureShape);
        if (!output.isPresent()) {
            return Optional.empty();
        }
        List<StructureShape> candidates = new ArrayList<>();
        for (MemberShape member : output.get().members()) {
            resolveListElement(model, member).ifPresent(candidates::add);
        }
        return candidates.size() == 1 ? Optional.of(candidates.get(0)) : Optional.empty();
    }

    private static Optional<StructureShape> resolveListElement(Model model, MemberShape member) {
        return model.getShape(member.getTarget())
                .flatMap(Shape::asListShape)
                .flatMap(list -> model.getShape(list.getMember().getTarget()))
                .flatMap(Shape::asStructureShape);
    }
}
