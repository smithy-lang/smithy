/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.shapes.AbstractShapeBuilder;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.Validator;

/**
 * Sets member targets based on referenced resource identifiers.
 *
 * <p>Structures can elide the targets of members if they're bound to a resource
 * and that resource has an identifier with a matching name. Here we set the
 * target based on that information.
 */
final class ApplyResourceBasedTargets implements ShapeModifier {
    private final ShapeId resourceId;
    private List<ValidationEvent> events;

    ApplyResourceBasedTargets(ShapeId resourceId) {
        this.resourceId = resourceId;
    }

    @Override
    public void modifyMember(
            AbstractShapeBuilder<?, ?> shapeBuilder,
            MemberShape.Builder memberBuilder,
            Function<ShapeId, Map<ShapeId, Trait>> unclaimedTraits,
            Function<ShapeId, Shape> shapeMap
    ) {
        // Fast-fail the common case of the target having already been set.
        if (memberBuilder.getTarget() != null) {
            return;
        }

        Shape fromShape = shapeMap.apply(resourceId);
        if (fromShape == null) {
            throw new SourceException("Cannot apply resource to elided member " + memberBuilder.getId() + ": "
                    + resourceId + " not found", memberBuilder);
        }

        if (!fromShape.isResourceShape()) {
            fromShapeIsNotResource(memberBuilder, fromShape);
        } else {
            ResourceShape resource = fromShape.asResourceShape().get();
            String name = memberBuilder.getId().getMember().get();
            if (resource.getIdentifiers().containsKey(name)) {
                memberBuilder.target(resource.getIdentifiers().get(name));
            }
            if (resource.getProperties().containsKey(name)) {
                memberBuilder.target(resource.getProperties().get(name));
            }
        }
    }

    private void fromShapeIsNotResource(MemberShape.Builder memberBuilder, Shape fromShape) {
        String message = String.format(
                "The target of the `for` production must be a resource shape, but found a %s shape: %s",
                fromShape.getType(),
                resourceId);
        ValidationEvent event = ValidationEvent.builder()
                .id(Validator.MODEL_ERROR)
                .severity(Severity.ERROR)
                .shapeId(memberBuilder.getId())
                .sourceLocation(memberBuilder.getSourceLocation())
                .message(message)
                .build();
        if (events == null) {
            events = new ArrayList<>(1);
        }
        events.add(event);
    }

    @Override
    public List<ValidationEvent> getEvents() {
        return events == null ? Collections.emptyList() : events;
    }
}
