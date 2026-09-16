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

    // Event-id prefixes for changes that make a member non-nullable will not be downgraded, these two can be
    // backward incompatible for users.
    private static final Set<String> NULLABILITY_EVENT_ID_PREFIXES =
            SetUtils.of("ChangedNullability", "AddedRequiredMember");

    // Adding, updating, or removing @unstable's featureId is a misuse of the trait itself and must stay
    // blocking rather than be downgraded as a preview change.
    private static final String UNSTABLE_TRAIT_EVENT_PREFIX = "TraitBreakingChange.";
    private static final String UNSTABLE_TRAIT_EVENT_SUFFIX = "." + UnstableTrait.ID;

    @Override
    public ValidationEvent decorate(Differences differences, ValidationEvent event) {
        Severity severity = event.getSeverity();
        if (severity != Severity.ERROR && severity != Severity.DANGER) {
            return event;
        }
        if (!event.getShapeId().isPresent()) {
            return event;
        }

        if (event.getId().startsWith(UNSTABLE_TRAIT_EVENT_PREFIX)
                && event.getId().endsWith(UNSTABLE_TRAIT_EVENT_SUFFIX)) {
            return event;
        }

        ShapeId shapeId = event.getShapeId().get();
        Model newModel = differences.getNewModel();
        boolean inNewModel = newModel.getShape(shapeId).isPresent();

        if (inNewModel && !UnstableFeatureIndex.of(newModel).isInPreviewClosure(shapeId)) {
            return event;
        }

        Model oldModel = differences.getOldModel();
        boolean inOldModel = oldModel.getShape(shapeId).isPresent();
        if (inOldModel && !UnstableFeatureIndex.of(oldModel).isInPreviewClosure(shapeId)) {
            return event;
        }

        if (!inNewModel && !inOldModel) {
            return event;
        }

        Model model = inNewModel ? newModel : oldModel;
        if (isNullabilityEvent(event) && hasMemberOwner(model, shapeId)) {
            return event;
        }

        return event.toBuilder().severity(Severity.WARNING).build();
    }

    private static boolean hasMemberOwner(Model model, ShapeId shapeId) {
        for (ShapeId ownerId : UnstableFeatureIndex.of(model).getFeatureOwners(shapeId)) {
            if (model.getShape(ownerId).filter(Shape::isMemberShape).isPresent()) {
                return true;
            }
        }
        return false;
    }

    private static boolean isNullabilityEvent(ValidationEvent event) {
        for (String prefix : NULLABILITY_EVENT_ID_PREFIXES) {
            if (event.getId().startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
