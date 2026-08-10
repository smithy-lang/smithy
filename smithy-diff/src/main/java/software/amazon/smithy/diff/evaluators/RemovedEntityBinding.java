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
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.shapes.EntityShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * A meta-validator that checks for the removal of an operation or
 * resource binding from a service or resource.
 *
 * <p>A "RemovedOperationBinding" eventId is used when an operation is
 * removed, and a "RemovedResourceBinding" eventId is used when a
 * resource is removed.
 */
public final class RemovedEntityBinding extends AbstractDiffEvaluator {
    private static final String REMOVED_RESOURCE = "RemovedResourceBinding";
    private static final String REMOVED_OPERATION = "RemovedOperationBinding";
    private static final String FROM_RESOURCE = ".FromResource.";
    private static final String FROM_SERVICE = ".FromService.";

    @Override
    public List<ValidationEvent> evaluate(Differences differences) {
        List<ValidationEvent> events = new ArrayList<>();
        differences.changedShapes(EntityShape.class)
                .forEach(change -> validateBindings(change, events, differences.getNewModel()));
        return events;
    }

    private void validateBindings(ChangedShape<EntityShape> change, List<ValidationEvent> events, Model newModel) {
        TopDownIndex topDownIndex = TopDownIndex.of(newModel);
        EntityShape oldEntity = change.getOldShape();
        EntityShape newEntity = change.getNewShape();

        for (ShapeId removed : findRemoved(oldEntity.getOperations(), newEntity.getOperations())) {
            if (isContainedInService(newEntity, removed, topDownIndex)) {
                events.add(createMovedOperationEvent(newEntity, removed));
            } else {
                events.add(createRemovedEvent(REMOVED_OPERATION, newEntity, removed));
            }
        }

        for (ShapeId removed : findRemoved(oldEntity.getResources(), newEntity.getResources())) {
            events.add(createRemovedEvent(REMOVED_RESOURCE, newEntity, removed));
        }
    }

    private boolean isContainedInService(EntityShape entity, ShapeId operationId, TopDownIndex topDownIndex) {
        if (!entity.isServiceShape()) {
            return false;
        }

        for (OperationShape operation : topDownIndex.getContainedOperations(entity)) {
            ShapeId currentId = operation.getId();
            int result = currentId.compareTo(operationId);
            if (result == 0) {
                return true;
            }
            if (result > 0) {
                return false;
            }
        }
        return false;
    }

    private Set<ShapeId> findRemoved(Set<ShapeId> oldShapes, Set<ShapeId> newShapes) {
        Set<ShapeId> removed = new HashSet<>(oldShapes);
        removed.removeAll(newShapes);
        return removed;
    }

    private ValidationEvent createMovedOperationEvent(EntityShape service, ShapeId operationId) {
        String message = String.format(
                "Operation binding of `%s` was removed from service shape, `%s`, "
                        + "but the operation remains in the service closure through a resource",
                operationId,
                service.getId());
        return ValidationEvent.builder()
                .id(REMOVED_OPERATION + FROM_SERVICE + operationId.getName())
                .severity(Severity.WARNING)
                .shape(service)
                .message(message)
                .build();
    }

    private ValidationEvent createRemovedEvent(String typeOfRemoval, EntityShape parentEntity, ShapeId childShape) {
        String childType = typeOfRemoval.equals(REMOVED_RESOURCE) ? "Resource" : "Operation";
        String typeOfParentShape = ShapeType.RESOURCE.equals(parentEntity.getType()) ? FROM_RESOURCE : FROM_SERVICE;
        String message = String.format(
                "%s binding of `%s` was removed from %s shape, `%s`",
                childType,
                childShape,
                parentEntity.getType(),
                parentEntity.getId());
        return ValidationEvent.builder()
                .id(typeOfRemoval + typeOfParentShape + childShape.getName())
                .severity(Severity.ERROR)
                .shape(parentEntity)
                .message(message)
                .build();
    }
}
