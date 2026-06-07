/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import static java.lang.String.format;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
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
 *
 * <p>Uses a one-of-N policy: if a member's name and target match multiple resources,
 * declaring a `@references` trait for at least one of them silences the warning for
 * all of them. The warning only fires when the member matches one or more resources
 * and the structure references none of them.
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

            Set<ShapeId> matchingResources = computeMatchingResources(model, reverseProvider, member);
            if (matchingResources.isEmpty()) {
                continue;
            }

            // One-of-N: any explicit `@references` to a matching resource is sufficient
            // evidence of author intent, so stay silent for all the other matches too.
            Set<ShapeId> referencedResources = collectReferencedResources(model, member);
            if (!Collections.disjoint(matchingResources, referencedResources)) {
                continue;
            }

            events.add(warning(member,
                    format("This member appears to reference the following resources without "
                            + "being included in a `@references` trait: [%s]",
                            ValidationUtils.tickedList(matchingResources))));
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

    private Set<ShapeId> computeMatchingResources(Model model, NeighborProvider reverseProvider, MemberShape member) {
        // Exclude resources the member is already bound to (i.e., the member is reachable from
        // the resource through the model graph), including the other resources in those
        // hierarchies. Resources on a `resource -> ... -> member` path are exactly the resources
        // from which the member is reachable, so a single reverse-reachability walk from the
        // member finds all of them at once. Being bound via lifecycle already makes this member
        // an identifier for that resource, so flagging it as a "missing reference" would be noise.
        Set<ShapeId> resourcesToIgnore = new HashSet<>();
        for (ShapeId boundResource : findBindingResources(reverseProvider, member)) {
            resourcesToIgnore.add(boundResource);
            resourcesToIgnore.addAll(model.expectShape(boundResource, ResourceShape.class).getResources());
        }

        Set<ShapeId> matchingResources = new HashSet<>();
        for (ResourceShape resource : model.getResourceShapes()) {
            if (resourcesToIgnore.contains(resource.getId())) {
                continue;
            }
            if (isIdentifierMatch(resource, member)) {
                matchingResources.add(resource.getId());
            }
        }
        return matchingResources;
    }

    private Set<ShapeId> collectReferencedResources(Model model, MemberShape member) {
        Set<ShapeId> referenced = new HashSet<>();
        addReferencedResources(member, referenced);
        addReferencedResources(model.expectShape(member.getContainer()), referenced);
        return referenced;
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
     * on recursive or highly-connected models and can cause this validator to hang.
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

    private void addReferencedResources(Shape shape, Set<ShapeId> referenced) {
        if (shape.hasTrait(ReferencesTrait.ID)) {
            for (ReferencesTrait.Reference reference : shape.expectTrait(ReferencesTrait.class)
                    .getReferences()) {
                referenced.add(reference.getResource());
            }
        }
    }

    private boolean isIdentifierMatch(ResourceShape resource, MemberShape member) {
        Map<String, ShapeId> identifiers = resource.getIdentifiers();
        return identifiers.containsKey(member.getMemberName())
                && identifiers.get(member.getMemberName()).equals(member.getTarget());
    }
}
