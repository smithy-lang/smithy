/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
import software.amazon.smithy.model.traits.HttpHeaderTrait;
import software.amazon.smithy.model.traits.HttpLabelTrait;
import software.amazon.smithy.model.traits.HttpPayloadTrait;
import software.amazon.smithy.model.traits.HttpPrefixHeadersTrait;
import software.amazon.smithy.model.traits.HttpQueryParamsTrait;
import software.amazon.smithy.model.traits.HttpQueryTrait;
import software.amazon.smithy.model.traits.HttpResponseCodeTrait;
import software.amazon.smithy.model.traits.MixinTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.ListUtils;

/**
 * Emits warnings when a structure member has an HTTP binding trait that will be ignored
 * in some contexts to which it is bound.
 *
 * <ul>
 *   <li>When httpLabel, httpQueryParams, or httpQuery is applied to a member of a shape that
 *  *   is not used as operation inputs.</li>
 *   <li>When httpResponseCode is applied to a member of a shape that is not used as an
 *  *   operation output.</li>
 *   <li>When any other HTTP member binding trait is applied to a member of a shape that is
 *  *   not used as a top-level operation input, output, or error.</li>
 * </ul>
 */
public class HttpBindingTraitIgnoredValidator extends AbstractValidator {
    private static final List<ShapeId> IGNORED_OUTSIDE_INPUT = ListUtils.of(
            HttpLabelTrait.ID,
            HttpQueryParamsTrait.ID,
            HttpQueryTrait.ID);
    private static final List<ShapeId> IGNORED_OUTSIDE_OUTPUT = ListUtils.of(
            HttpResponseCodeTrait.ID);
    private static final List<ShapeId> HTTP_MEMBER_BINDING_TRAITS = ListUtils.of(
            HttpHeaderTrait.ID,
            HttpLabelTrait.ID,
            HttpPayloadTrait.ID,
            HttpPrefixHeadersTrait.ID,
            HttpQueryParamsTrait.ID,
            HttpQueryTrait.ID,
            HttpResponseCodeTrait.ID);

    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        NeighborProvider reverse = NeighborProviderIndex.of(model).getReverseProvider();
        for (MemberShape memberShape : model.getMemberShapes()) {
            Shape container = model.expectShape(memberShape.getContainer());
            // Skip non-structures (invalid) and mixins (handled at mixed site).
            if (!container.isStructureShape() || container.hasTrait(MixinTrait.class)) {
                continue;
            }

            // Gather all traits that are HTTP member binding.
            // Keep the trait instance around so that it can be used it later for source location.
            Map<ShapeId, Trait> traits = new HashMap<>();
            for (Map.Entry<ShapeId, Trait> traitEntry : memberShape.getAllTraits().entrySet()) {
                if (HTTP_MEMBER_BINDING_TRAITS.contains(traitEntry.getKey())) {
                    traits.put(traitEntry.getKey(), traitEntry.getValue());
                }
            }

            // The traits set is now the HTTP binding traits that are ignored outside
            // the top level of an operation's components.
            if (!traits.isEmpty()) {
                // All relationships and trait possibilities are checked at once to de-duplicate
                // several parts of the iteration logic.
                events.addAll(checkRelationships(container.asStructureShape().get(), memberShape, traits, reverse));
            }
        }
        return events;
    }

    private List<ValidationEvent> checkRelationships(
            StructureShape containerShape,
            MemberShape memberShape,
            Map<ShapeId, Trait> traits,
            NeighborProvider reverse
    ) {
        // Prepare which traits need relationship tracking for.
        Set<ShapeId> ignoredOutsideInputTraits = new HashSet<>(traits.keySet());
        ignoredOutsideInputTraits.retainAll(IGNORED_OUTSIDE_INPUT);
        Set<ShapeId> ignoredOutsideOutputTraits = new HashSet<>(traits.keySet());
        ignoredOutsideOutputTraits.retainAll(IGNORED_OUTSIDE_OUTPUT);

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

            // Track if we've got a non-input relationship and a trait that's ignored outside input.
            // Continue so we don't emit a duplicate for non-top-level.
            if (relationship.getRelationshipType() != RelationshipType.INPUT
                    && !ignoredOutsideInputTraits.isEmpty()) {
                ignoredRelationships.merge(relationship.getRelationshipType(),
                        ListUtils.of(relationship.getShape().getId()),
                        this::mergeShapeIdLists);
                continue;
            }

            // Track if we've got a non-output relationship and a trait that's ignored outside output.
            // Continue so we don't emit a duplicate for non-top-level.
            if (relationship.getRelationshipType() != RelationshipType.OUTPUT
                    && !ignoredOutsideOutputTraits.isEmpty()) {
                ignoredRelationships.merge(relationship.getRelationshipType(),
                        ListUtils.of(relationship.getShape().getId()),
                        this::mergeShapeIdLists);
                continue;
            }

            // Track if there are non-top-level relationship and any HTTP member binding trait.
            if (relationship.getRelationshipType() != RelationshipType.INPUT
                    && relationship.getRelationshipType() != RelationshipType.OUTPUT
                    && relationship.getRelationshipType() != RelationshipType.ERROR) {
                ignoredRelationships.merge(relationship.getRelationshipType(),
                        ListUtils.of(relationship.getShape().getId()),
                        this::mergeShapeIdLists);
            }
        }

        // If we detected invalid relationships, build the right event message based
        // on the ignored traits. All the traits are conflicting on members, so the
        // immediate grabbing of next traits is all that's necessary.
        if (!ignoredRelationships.isEmpty()) {
            if (!ignoredOutsideInputTraits.isEmpty()) {
                Trait trait = traits.get(ignoredOutsideInputTraits.iterator().next());
                events.add(emit("Input", memberShape, trait, checkedRelationshipCount, ignoredRelationships));

            } else if (!ignoredOutsideOutputTraits.isEmpty()) {
                Trait trait = traits.get(ignoredOutsideOutputTraits.iterator().next());
                events.add(emit("Output", memberShape, trait, checkedRelationshipCount, ignoredRelationships));
            } else {
                // The traits list is always non-empty here, so just grab the first.
                Trait trait = traits.get(traits.keySet().iterator().next());
                events.add(emit("TopLevel", memberShape, trait, checkedRelationshipCount, ignoredRelationships));
            }
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
            String type,
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
        return warning(memberShape, trait, format(message, Trait.getIdiomaticTraitName(trait), mixedIn), type);
    }

    private String formatIgnoredRelationships(Map<RelationshipType, List<ShapeId>> ignoredRelationships) {
        List<String> relationshipTypeBindings = new ArrayList<>();
        for (Map.Entry<RelationshipType, List<ShapeId>> ignoredRelationshipType : ignoredRelationships.entrySet()) {
            StringBuilder stringBuilder = new StringBuilder(ignoredRelationshipType.getKey()
                    .toString()
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
