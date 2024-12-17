/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.NeighborProviderIndex;
import software.amazon.smithy.model.neighbor.NeighborProvider;
import software.amazon.smithy.model.neighbor.Relationship;
import software.amazon.smithy.model.neighbor.RelationshipType;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.IdempotencyTokenTrait;
import software.amazon.smithy.model.traits.MixinTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Emits warnings when a structure member has an idempotency token trait that will be ignored.
 */
public final class IdempotencyTokenIgnoredValidator extends AbstractValidator {

    @Override
    public List<ValidationEvent> validate(Model model) {
        if (!model.getAppliedTraits().contains(IdempotencyTokenTrait.ID)) {
            return Collections.emptyList();
        }
        List<ValidationEvent> events = new ArrayList<>();
        NeighborProvider reverse = NeighborProviderIndex.of(model).getReverseProvider();
        for (MemberShape memberShape : model.getMemberShapesWithTrait(IdempotencyTokenTrait.class)) {
            Shape container = model.expectShape(memberShape.getContainer());
            // Skip non-structures (invalid) and mixins (handled at mixed site).
            if (!container.isStructureShape() || container.hasTrait(MixinTrait.class)) {
                continue;
            }
            Trait trait = memberShape.expectTrait(IdempotencyTokenTrait.class);
            checkRelationships(container.asStructureShape().get(), memberShape, trait, reverse, events);
        }
        return events;
    }

    private void checkRelationships(
            StructureShape containerShape,
            MemberShape memberShape,
            Trait trait,
            NeighborProvider reverse,
            List<ValidationEvent> events
    ) {

        // Store relationships so we can emit one event per ignored binding.
        Map<RelationshipType, List<ShapeId>> ignoredRelationships = new TreeMap<>();
        List<Relationship> relationships = reverse.getNeighbors(containerShape);
        for (Relationship relationship : relationships) {
            // Skip members of the container.
            if (relationship.getRelationshipType() == RelationshipType.MEMBER_CONTAINER) {
                continue;
            }
            if (relationship.getRelationshipType() != RelationshipType.INPUT) {
                ignoredRelationships.computeIfAbsent(relationship.getRelationshipType(), x -> new ArrayList<>())
                        .add(relationship.getShape().getId());
            }
        }

        // If we detected invalid relationships, build the right event message.
        if (!ignoredRelationships.isEmpty()) {
            events.add(emit(memberShape, trait, ignoredRelationships));
        }
    }

    private ValidationEvent emit(
            MemberShape memberShape,
            Trait trait,
            Map<RelationshipType, List<ShapeId>> ignoredRelationships
    ) {
        String message =
                "The `idempotencyToken` trait only has an effect when applied to a top-level operation input member, "
                        + "but it was applied and ignored in the following contexts: "
                        + formatIgnoredRelationships(ignoredRelationships);
        return warning(memberShape, trait, message);
    }

    private String formatIgnoredRelationships(Map<RelationshipType, List<ShapeId>> ignoredRelationships) {
        List<String> relationshipTypeBindings = new ArrayList<>();
        for (Map.Entry<RelationshipType, List<ShapeId>> ignoredRelationshipType : ignoredRelationships.entrySet()) {
            StringBuilder buf = new StringBuilder(ignoredRelationshipType.getKey()
                    .toString()
                    .toLowerCase(Locale.US)
                    .replace("_", " "));
            Set<String> bindings = ignoredRelationshipType.getValue()
                    .stream()
                    .map(ShapeId::toString)
                    .collect(Collectors.toCollection(TreeSet::new));
            buf.append(": [").append(String.join(", ", bindings)).append("]");
            relationshipTypeBindings.add(buf.toString());
        }
        return "{" + String.join(", ", relationshipTypeBindings) + "}";
    }
}
