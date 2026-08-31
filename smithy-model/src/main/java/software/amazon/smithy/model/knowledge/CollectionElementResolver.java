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
import software.amazon.smithy.model.traits.PaginatedTrait;

/**
 * Resolves the structure that carries per-instance resource state in the
 * output of a collection operation, by looking through the output's list
 * member to the structure its elements target.
 */
final class CollectionElementResolver {

    private CollectionElementResolver() {}

    /**
     * Returns true if the operation is bound to the resource in a way that
     * carries resource state per list element: through the {@code list}
     * lifecycle or the {@code collectionOperations} property. The
     * {@code create} lifecycle is excluded because its input and output carry
     * top-level resource state.
     */
    static boolean isElementCarrier(ResourceShape resource, ShapeId operationId) {
        return resource.getList().filter(operationId::equals).isPresent()
                || resource.getCollectionOperations().contains(operationId);
    }

    /**
     * Resolves the element structure of a collection operation's output.
     * Uses the member named by {@code @paginated(items)} when present, and
     * otherwise resolves only when exactly one output member targets a list
     * of structures. Returns empty when the carrying member is ambiguous or
     * absent.
     */
    static Optional<StructureShape> resolveOutputElement(Model model, OperationShape operation) {
        Optional<StructureShape> output = model.getShape(operation.getOutputShape())
                .flatMap(Shape::asStructureShape);
        if (!output.isPresent()) {
            return Optional.empty();
        }

        Optional<String> itemsMember = operation.getTrait(PaginatedTrait.class).flatMap(PaginatedTrait::getItems);
        if (itemsMember.isPresent()) {
            return output.get().getMember(itemsMember.get()).flatMap(member -> resolveListElement(model, member));
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
