/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.NeighborProviderIndex;
import software.amazon.smithy.model.neighbor.NeighborProvider;
import software.amazon.smithy.model.neighbor.Relationship;
import software.amazon.smithy.model.neighbor.RelationshipType;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.PrivateTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.ListUtils;

/**
 * Ensures that shapes in separate namespaces don't refer to shapes in other
 * namespaces that are marked as private.
 */
public final class PrivateAccessValidator extends AbstractValidator {

    @Override
    public List<ValidationEvent> validate(Model model) {
        Set<Shape> privateShapes = model.getShapesWithTrait(PrivateTrait.class);
        if (privateShapes.isEmpty()) {
            return ListUtils.of();
        }

        Set<ShapeId> privateShapeIds = new HashSet<>(privateShapes.size());
        for (Shape privateShape : privateShapes) {
            privateShapeIds.add(privateShape.getId());
        }

        NeighborProvider provider = NeighborProviderIndex.of(model).getProvider();

        List<ValidationEvent> events = new ArrayList<>();
        for (Shape shape : model.toSet()) {
            validateNeighbors(shape, provider.getNeighbors(shape), privateShapeIds, events);
            validateTraitRelationships(model, shape, privateShapeIds, events);
        }

        return events;
    }

    private void validateNeighbors(
            Shape shape,
            List<Relationship> relationships,
            Set<ShapeId> privateShapeIds,
            List<ValidationEvent> events
    ) {
        String sourceNamespace = shape.getId().getNamespace();
        for (Relationship rel : relationships) {
            ShapeId neighborId = rel.getNeighborShapeId();
            if (!sourceNamespace.equals(neighborId.getNamespace()) && privateShapeIds.contains(neighborId)) {
                events.add(getPrivateAccessValidationEvent(rel));
            }
        }
    }

    private void validateTraitRelationships(
            Model model,
            Shape shape,
            Set<ShapeId> privateShapeIds,
            List<ValidationEvent> events
    ) {
        String sourceNamespace = shape.getId().getNamespace();
        for (ShapeId traitId : shape.getAllTraits().keySet()) {
            if (!sourceNamespace.equals(traitId.getNamespace()) && privateShapeIds.contains(traitId)) {
                Shape privateTrait = model.expectShape(traitId);
                events.add(getPrivateAccessValidationEvent(Relationship.create(
                        shape,
                        RelationshipType.TRAIT,
                        privateTrait)));
            }
        }
    }

    private ValidationEvent getPrivateAccessValidationEvent(Relationship relationship) {
        ShapeId neighborId = relationship.expectNeighborShape().getId();
        String message = String.format(
                "This shape has an invalid %s relationship that targets a private shape, `%s`, in another namespace.",
                relationship.getRelationshipType().toString().toLowerCase(Locale.US),
                neighborId);

        // For now, emit a warning for trait relationships instead of an error. This is because private access on trait
        // relationships was not being validated in the past, so emitting a warning maintains backward compatibility.
        // This will be upgraded to an error in the future.
        if (relationship.getRelationshipType().equals(RelationshipType.TRAIT)) {
            return warning(relationship.getShape(), message);
        } else {
            return error(relationship.getShape(), message);
        }
    }
}
