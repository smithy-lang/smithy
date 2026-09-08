/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.diff;

import java.util.Set;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.UnstableFeatureIndex;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.UnstableTrait;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.SetUtils;

/**
 * Downgrades backward-incompatible diff events from ERROR or DANGER to WARNING when they occur on a shape
 * governed by a preview unstable feature.
 *
 * <p>An event is downgraded only when its shape is owned by a preview feature (resolved via
 * {@link UnstableFeatureIndex}) in both models it appears in.
 */
public final class PreviewFeatureDiffEventDecorator implements DiffEventDecorator {

    // Event-id prefixes for changes that make a member non-nullable will not be downgraded, these two can cause
    private static final Set<String> NULLABILITY_EVENT_ID_PREFIXES =
            SetUtils.of("ChangedNullability", "AddedRequiredMember");

    // Emitted when @unstable's featureId is added to an existing shape.
    private static final String UNSTABLE_ADDED_EVENT_ID =
            "TraitBreakingChange.Add." + UnstableTrait.ID;

    @Override
    public ValidationEvent decorate(Differences differences, ValidationEvent event) {
        Severity severity = event.getSeverity();
        if (severity != Severity.ERROR && severity != Severity.DANGER) {
            return event;
        }
        if (!event.getShapeId().isPresent()) {
            return event;
        }

        if (event.getId().equals(UNSTABLE_ADDED_EVENT_ID)) {
            return event;
        }

        Shape unstableOwner = resolveClosureOwner(differences, event.getShapeId().get());
        if (unstableOwner == null) {
            return event;
        }

        // A preview member's nullability changes break the enclosing GA operation's existing customers,
        // so they stay blocking.
        if (unstableOwner.isMemberShape() && isNullabilityEvent(event)) {
            return event;
        }

        return event.toBuilder().severity(Severity.WARNING).build();
    }

    private static boolean isNullabilityEvent(ValidationEvent event) {
        for (String prefix : NULLABILITY_EVENT_ID_PREFIXES) {
            if (event.getId().startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static Shape resolveClosureOwner(Differences differences, ShapeId shapeId) {
        Model newModel = differences.getNewModel();
        Model oldModel = differences.getOldModel();
        boolean inNewModel = newModel.getShape(shapeId).isPresent();
        boolean inOldModel = oldModel.getShape(shapeId).isPresent();

        if (!inNewModel) {
            return inOldModel ? resolvePreviewOwner(oldModel, shapeId) : null;
        }

        Shape owner = resolvePreviewOwner(newModel, shapeId);
        if (owner == null || (inOldModel && resolvePreviewOwner(oldModel, shapeId) == null)) {
            return null;
        }

        return owner;
    }

    private static Shape resolvePreviewOwner(Model model, ShapeId shapeId) {
        UnstableFeatureIndex index = UnstableFeatureIndex.of(model);
        if (!index.isInPreviewClosure(shapeId)) {
            return null;
        }

        // isInPreviewClosure only resolves for a shape with exactly one owner, so this owner is unambiguous.
        Set<ShapeId> owners = index.getFeatureOwners(shapeId);
        return model.getShape(owners.iterator().next()).orElse(null);
    }
}
