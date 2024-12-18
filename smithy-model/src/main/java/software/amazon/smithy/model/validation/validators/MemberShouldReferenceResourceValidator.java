/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.selector.PathFinder;
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

            Set<ShapeId> potentialReferences = computePotentialReferences(model, member);
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

    private Set<ShapeId> computePotentialReferences(Model model, MemberShape member) {
        // Exclude any resources already in `@references` on the member or container structure.
        Set<ShapeId> resourcesToIgnore = new HashSet<>();
        ignoreReferencedResources(member, resourcesToIgnore);
        ignoreReferencedResources(model.expectShape(member.getContainer()), resourcesToIgnore);

        // Check each resource in the model for something missed.
        Set<ShapeId> potentialResources = new HashSet<>();
        for (ResourceShape resource : model.getResourceShapes()) {
            // We'll want to ignore some resources based on the member -> resource path.
            computeResourcesToIgnore(model, member, resource, resourcesToIgnore);

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

    private void computeResourcesToIgnore(
            Model model,
            MemberShape member,
            ResourceShape resource,
            Set<ShapeId> resourcesToIgnore
    ) {
        // Exclude actually bound members via searching with a PathFinder.
        List<PathFinder.Path> resourceMemberPaths = PathFinder.create(model)
                .search(resource, ListUtils.of(member));
        if (!resourceMemberPaths.isEmpty()) {
            // This member is already bound to a resource, so we don't need a references trait for it.
            // In addition, we should not tell users to add a references trait for other resources that
            // are children in that hierarchy - any parent resources or other children of those parents.
            for (PathFinder.Path path : resourceMemberPaths) {
                for (Shape pathShape : path.getShapes()) {
                    if (pathShape.isResourceShape()) {
                        ResourceShape resourceShape = (ResourceShape) pathShape;
                        resourcesToIgnore.add(resourceShape.getId());
                        resourcesToIgnore.addAll(resourceShape.getResources());
                    }
                }
            }
        }
    }

    private void ignoreReferencedResources(Shape shape, Set<ShapeId> resourcesToIgnore) {
        if (shape.hasTrait(ReferencesTrait.class)) {
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
