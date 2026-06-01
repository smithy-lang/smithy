/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import static java.lang.String.format;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.NeighborProviderIndex;
import software.amazon.smithy.model.neighbor.NeighborProvider;
import software.amazon.smithy.model.neighbor.Relationship;
import software.amazon.smithy.model.neighbor.RelationshipDirection;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.ReferencesTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidationUtils;
import software.amazon.smithy.utils.ListUtils;

/**
 * Validates if a member matches a resource identifier without the
 * proper configuration of a `@references` trait.
 */
public final class MemberShouldReferenceResourceValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        // There are usually far fewer resources than members, precompute the identifiers
        // so various short circuits can be added.
        Set<String> identifierNames = getAllIdentifierNames(model);
        // Short circuit validating all the members if we don't have any resources to test.
        if (identifierNames.isEmpty()) {
            return ListUtils.of();
        }

        // Reverse (predecessor) neighbors are used to discover which resources a member is
        // already bound to. This index is cached on the model and reused for every member.
        NeighborProvider reverseProvider = NeighborProviderIndex.of(model).getReverseProvider();

        // Check every member to see if it's a potential reference.
        List<ValidationEvent> events = new ArrayList<>();
        for (MemberShape member : model.getMemberShapes()) {
            // Only the known identifier names can match for this, skip names that we don't know.
            if (!identifierNames.contains(member.getMemberName())) {
                continue;
            }
            // Only strings can be identifiers, so skip non-String targets.
            if (!model.expectShape(member.getTarget()).isStringShape()) {
                continue;
            }

            Set<ShapeId> potentialReferences = computePotentialReferences(model, reverseProvider, member);
            if (!potentialReferences.isEmpty()) {
                events.add(warning(member,
                        format("This member appears to reference the following resources without "
                                + "being included in a `@references` trait: [%s]",
                                ValidationUtils.tickedList(potentialReferences))));
            }
        }

        return events;
    }

    private Set<String> getAllIdentifierNames(Model model) {
        Set<String> identifierNames = new HashSet<>();
        for (ResourceShape resource : model.getResourceShapes()) {
            identifierNames.addAll(resource.getIdentifiers().keySet());
        }
        return identifierNames;
    }

    private Set<ShapeId> computePotentialReferences(Model model, NeighborProvider reverseProvider, MemberShape member) {
        // Exclude any resources already in `@references` on the member or container structure.
        Set<ShapeId> resourcesToIgnore = new HashSet<>();
        ignoreReferencedResources(member, resourcesToIgnore);
        ignoreReferencedResources(model.expectShape(member.getContainer()), resourcesToIgnore);

        // Exclude resources the member is already bound to (i.e., the member is reachable from
        // the resource through the model graph), including the other resources in those
        // hierarchies. Resources on a `resource -> ... -> member` path are exactly the resources
        // from which the member is reachable, so a single reverse-reachability walk from the
        // member finds all of them at once.
        for (ShapeId boundResource : findBindingResources(reverseProvider, member)) {
            resourcesToIgnore.add(boundResource);
            resourcesToIgnore.addAll(model.expectShape(boundResource, ResourceShape.class).getResources());
        }

        // Check each resource in the model for something missed.
        Set<ShapeId> potentialResources = new HashSet<>();
        for (ResourceShape resource : model.getResourceShapes()) {
            // Exclude members bound to resource hierarchies from generating events,
            // including for resources that are within the same hierarchy.
            if (resourcesToIgnore.contains(resource.getId())) {
                continue;
            }

            // This member matches the identifier for the resource we're checking, add it to a list.
            if (isIdentifierMatch(resource, member)) {
                potentialResources.add(resource.getId());
            }
        }

        // Clean up any resources added through other paths that should be ignored.
        potentialResources.removeAll(resourcesToIgnore);
        return potentialResources;
    }

    /**
     * Finds every resource from which the given member is reachable by traversing directed
     * relationships, i.e., the resources the member is effectively bound to.
     *
     * <p>This walks the reverse (predecessor) neighbor graph starting from the member using a
     * permanent visited set, so each shape and relationship is visited at most once and the walk
     * runs in {@code O(V + E)}. It intentionally mirrors the directed-relationship traversal that
     * {@link software.amazon.smithy.model.selector.PathFinder} performs, but without enumerating
     * every simple path between each resource and the member ... that enumeration is exponential
     * on recursive or highly-connected models and previously caused this validator to hang.
     */
    private Set<ShapeId> findBindingResources(NeighborProvider reverseProvider, MemberShape member) {
        Set<ShapeId> boundResources = new HashSet<>();
        Set<ShapeId> visited = new HashSet<>();
        Deque<Shape> frontier = new ArrayDeque<>();
        visited.add(member.getId());
        frontier.push(member);

        while (!frontier.isEmpty()) {
            Shape current = frontier.pop();
            for (Relationship relationship : reverseProvider.getNeighbors(current)) {
                // Match PathFinder, which only walks directed relationships.
                if (relationship.getDirection() != RelationshipDirection.DIRECTED) {
                    continue;
                }
                Shape predecessor = relationship.getShape();
                if (visited.add(predecessor.getId())) {
                    if (predecessor.isResourceShape()) {
                        boundResources.add(predecessor.getId());
                    }
                    frontier.push(predecessor);
                }
            }
        }

        return boundResources;
    }

    private void ignoreReferencedResources(Shape shape, Set<ShapeId> resourcesToIgnore) {
        if (shape.hasTrait(ReferencesTrait.ID)) {
            for (ReferencesTrait.Reference reference : shape.expectTrait(ReferencesTrait.class)
                    .getReferences()) {
                resourcesToIgnore.add(reference.getResource());
            }
        }
    }

    private boolean isIdentifierMatch(ResourceShape resource, MemberShape member) {
        Map<String, ShapeId> identifiers = resource.getIdentifiers();
        return identifiers.containsKey(member.getMemberName())
                && identifiers.get(member.getMemberName()).equals(member.getTarget());
    }
}
