/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.diff.evaluators;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import software.amazon.smithy.diff.ChangedShape;
import software.amazon.smithy.diff.Differences;
import software.amazon.smithy.model.shapes.EntityShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * A meta-validator that emits a NOTE when an operation resource is added
 * to a service or resource entity.
 *
 * <p>An "AddedOperationBinding" eventId is used when an operation is
 * added, and an "AddedResourceBinding" eventId is used when a
 * resource is added.
 */
public final class AddedEntityBinding extends AbstractDiffEvaluator {
    private static final String ADDED_RESOURCE = "AddedResourceBinding";
    private static final String ADDED_OPERATION = "AddedOperationBinding";
    private static final String TO_RESOURCE = ".ToResource.";
    private static final String TO_SERVICE = ".ToService.";

    @Override
    public List<ValidationEvent> evaluate(Differences differences) {
        List<ValidationEvent> events = new ArrayList<>();
        differences.changedShapes(EntityShape.class).forEach(change -> validateOperation(change, events));
        return events;
    }

    private void validateOperation(ChangedShape<EntityShape> change, List<ValidationEvent> events) {
        findAdded(change.getOldShape().getOperations(), change.getNewShape().getOperations())
                .forEach(added -> events.add(createAddedEvent(ADDED_OPERATION, change.getNewShape(), added)));

        findAdded(change.getOldShape().getResources(), change.getNewShape().getResources())
                .forEach(added -> events.add(createAddedEvent(ADDED_RESOURCE, change.getNewShape(), added)));
    }

    private Set<ShapeId> findAdded(Set<ShapeId> oldShapes, Set<ShapeId> newShapes) {
        Set<ShapeId> added = new HashSet<>(newShapes);
        added.removeAll(oldShapes);
        return added;
    }

    private ValidationEvent createAddedEvent(String typeOfAddition, EntityShape parentEntity, ShapeId childShape) {
        String childType = typeOfAddition.equals(ADDED_RESOURCE) ? "Resource" : "Operation";
        String typeOfParentShape = ShapeType.RESOURCE.equals(parentEntity.getType()) ? TO_RESOURCE : TO_SERVICE;
        String message = String.format(
                "%s binding of `%s` was added to the %s shape, `%s`",
                childType,
                childShape,
                parentEntity.getType(),
                parentEntity.getId());
        return ValidationEvent.builder()
                .id(typeOfAddition + typeOfParentShape + childShape.getName())
                .severity(Severity.NOTE)
                .shape(parentEntity)
                .message(message)
                .build();
    }
}
