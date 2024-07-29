/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.model.validation.validators;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
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
import software.amazon.smithy.utils.ListUtils;

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
        for (MemberShape memberShape : model.getMemberShapes()) {
            Shape container = model.expectShape(memberShape.getContainer());
            // Skip non-structures (invalid) and mixins (handled at mixed site).
            if (!container.isStructureShape() || container.hasTrait(MixinTrait.class)) {
                continue;
            }

            if (memberShape.hasTrait(IdempotencyTokenTrait.class)) {
                Trait trait = memberShape.expectTrait(IdempotencyTokenTrait.class);
                events.addAll(checkRelationships(container.asStructureShape().get(), memberShape, trait, reverse));
            }
        }
        return events;
    }

    private List<ValidationEvent> checkRelationships(
        StructureShape containerShape,
        MemberShape memberShape,
        Trait trait,
        NeighborProvider reverse
    ) {

        // Store relationships so we can emit one event per ignored binding.
        Map<RelationshipType, List<ShapeId>> ignoredRelationships = new TreeMap<>();
        List<ValidationEvent> events = new ArrayList<>();
        List<Relationship> relationships = reverse.getNeighbors(containerShape);
        int checkedRelationshipCount = relationships.size();
        for (Relationship relationship : relationships) {
            // Skip members of the container.
            if (relationship.getRelationshipType() == RelationshipType.MEMBER_CONTAINER) {
                checkedRelationshipCount--;
                continue;
            }
            if (relationship.getRelationshipType() != RelationshipType.INPUT) {
                ignoredRelationships.merge(relationship.getRelationshipType(),
                                           ListUtils.of(relationship.getShape().getId()), this::mergeShapeIdLists);
            }
        }

        // If we detected invalid relationships, build the right event message.
        if (!ignoredRelationships.isEmpty()) {
            events.add(emit(memberShape, trait, checkedRelationshipCount, ignoredRelationships));
        }

        return events;
    }

    private List<ShapeId> mergeShapeIdLists(List<ShapeId> shapeIds1, List<ShapeId> shapeIds2) {
        List<ShapeId> shapeIds = new ArrayList<>();
        shapeIds.addAll(shapeIds1);
        shapeIds.addAll(shapeIds2);
        return shapeIds;
    }

    private ValidationEvent emit(
        MemberShape memberShape,
        Trait trait,
        int checkedRelationshipCount,
        Map<RelationshipType, List<ShapeId>> ignoredRelationships
    ) {
        String mixedIn = memberShape.getMixins().isEmpty() ? "" : " mixed in";
        String message = "The `%s` trait applied to this%s member is ";
        if (checkedRelationshipCount == ignoredRelationships.size()) {
            message += "ignored in all contexts.";
        } else {
            message += "ignored in some contexts: " + formatIgnoredRelationships(ignoredRelationships);
        }
        return warning(memberShape, trait, format(message, Trait.getIdiomaticTraitName(trait), mixedIn));
    }

    private String formatIgnoredRelationships(Map<RelationshipType, List<ShapeId>> ignoredRelationships) {
        List<String> relationshipTypeBindings = new ArrayList<>();
        for (Map.Entry<RelationshipType, List<ShapeId>> ignoredRelationshipType : ignoredRelationships.entrySet()) {
            StringBuilder stringBuilder = new StringBuilder(ignoredRelationshipType.getKey().toString()
                                                                                   .toLowerCase(Locale.US)
                                                                                   .replace("_", " "));
            Set<String> bindings = new TreeSet<>();
            for (ShapeId binding : ignoredRelationshipType.getValue()) {
                bindings.add(binding.toString());
            }
            stringBuilder.append(": [").append(String.join(", ", bindings)).append("]");
            relationshipTypeBindings.add(stringBuilder.toString());
        }
        return "{" + String.join(", ", relationshipTypeBindings) + "}";
    }
}
