/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.protocol.traits;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.neighbor.Walker;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.EventHeaderTrait;
import software.amazon.smithy.model.traits.EventPayloadTrait;
import software.amazon.smithy.model.traits.MixinTrait;
import software.amazon.smithy.model.traits.StreamingTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Validates indexed structures, unions, and services.
 */
public final class IndexedTraitValidator extends AbstractValidator {

    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();

        // First look for indexed services and add all of their indexable shapes.
        Set<ShapeId> serviceClosureShapes = findServiceClosureShapes(model);
        Set<ShapeId> candidates = new TreeSet<>(serviceClosureShapes);

        // Then check all indexable shapes for any that have indexed members,
        // regardless of whether they're bound to an indexed service or not.
        for (Shape shape : model.toSet()) {
            if (!candidates.contains(shape.getId()) && isIndexable(shape) && hasIndexedMember(shape)) {
                candidates.add(shape.getId());
            }
        }

        for (ShapeId candidate : candidates) {
            validateMembers(
                    model,
                    model.expectShape(candidate),
                    serviceClosureShapes.contains(candidate),
                    events);
        }
        return events;
    }

    private boolean hasIndexedMember(Shape shape) {
        for (MemberShape member : shape.members()) {
            if (member.hasTrait(IdxTrait.class)) {
                return true;
            }
        }
        return false;
    }

    private Set<ShapeId> findServiceClosureShapes(Model model) {
        Set<ShapeId> result = new TreeSet<>();
        for (ServiceShape service : model.getServiceShapesWithTrait(IndexedTrait.class)) {
            new Walker(model).iterateShapes(service).forEachRemaining(shape -> {
                if (isIndexable(shape)) {
                    result.add(shape.getId());
                }
            });
        }
        return result;
    }

    private boolean isIndexable(Shape shape) {
        return (shape.isStructureShape() || shape.isUnionShape()) && !shape.hasTrait(MixinTrait.class);
    }

    private void validateMembers(
            Model model,
            Shape container,
            boolean inServiceClosure,
            List<ValidationEvent> events
    ) {
        List<MemberShape> members = container.members()
                .stream()
                .sorted(Comparator
                        .comparingInt(this::getIndexOrMissing)
                        .thenComparing(MemberShape::getMemberName))
                .collect(Collectors.toList());

        int lastIdx = 0;
        for (MemberShape member : members) {
            if (!member.hasTrait(IdxTrait.class)) {
                if (isIndexExempt(model, member)) {
                    continue;
                }
                String reason = inServiceClosure
                        ? String.format("Structure `%s` is in the closure of an indexed service",
                                member.getContainer())
                        : String.format("Shape `%s` contains an idx trait", member.getContainer());
                events.add(error(member,
                        member.getSourceLocation(),
                        String.format(
                                "%s, but its member `%s` is missing an idx trait",
                                reason,
                                member.toShapeId())));
                continue;
            }

            IdxTrait idx = member.expectTrait(IdxTrait.class);
            int value = idx.getValue();
            if (value == lastIdx) {
                events.add(error(member, idx, String.format("Duplicate idx value \"%d\"", value)));
            } else {
                int expected = lastIdx + 1;
                if (value != expected) {
                    if (value == expected + 1) {
                        events.add(error(member,
                                idx,
                                String.format(
                                        "idx must start at 1 and increase with no gaps, "
                                                + "but no members were found for idx %d",
                                        expected)));
                    } else {
                        events.add(error(member,
                                idx,
                                String.format(
                                        "idx must start at 1 and increase with no gaps, "
                                                + "but no members were found for idxs between %d and %d, inclusive",
                                        expected,
                                        value - 1)));
                    }
                }
                lastIdx = value;
            }
        }
    }

    private int getIndexOrMissing(MemberShape member) {
        return member.getTrait(IdxTrait.class).map(IdxTrait::getValue).orElse(-1);
    }

    private boolean isIndexExempt(Model model, MemberShape member) {
        if (member.hasTrait(EventHeaderTrait.class) || member.hasTrait(EventPayloadTrait.class)) {
            return true;
        }
        Shape target = model.expectShape(member.getTarget());
        return (target.isBlobShape() || target.isUnionShape()) && target.hasTrait(StreamingTrait.class);
    }
}
