/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import static java.lang.String.format;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import software.amazon.smithy.jmespath.JmespathException;
import software.amazon.smithy.jmespath.JmespathExpression;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.NeighborProviderIndex;
import software.amazon.smithy.model.neighbor.NeighborProvider;
import software.amazon.smithy.model.neighbor.Relationship;
import software.amazon.smithy.model.neighbor.RelationshipDirection;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AbstractResourceLifecycleTrait;
import software.amazon.smithy.model.traits.ReferencesTrait;
import software.amazon.smithy.model.traits.ResourceLifecycleBinding;
import software.amazon.smithy.model.traits.ResourceMemberBinding;
import software.amazon.smithy.model.traits.Trait;
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
        Map<String, Map<ShapeId, Set<ShapeId>>> resourceIdsByIdentifier = getResourceIdsByIdentifier(model);
        // Short circuit validating all the members if we don't have any resources to test.
        if (resourceIdsByIdentifier.isEmpty()) {
            return ListUtils.of();
        }

        // Reverse (predecessor) neighbors are used to discover which resources a member is
        // already bound to. This index is cached on the model and reused for every member.
        NeighborProvider reverseProvider = NeighborProviderIndex.of(model).getReverseProvider();

        // Precompute the resources that a resource lifecycle trait (@createsResources,
        // @readsResources, etc.) declares as located at a given member, resolving each identifier
        // path to the member it designates (for example `path: "output.fooId"` -> the `fooId`
        // member of `output`). Such a binding is an explicit declaration that the member identifies
        // the resource, exactly like `@references`, so it should suppress this advisory. Paths that
        // point into a list element or through a `...From` pointer target nested element structures
        // and are intentionally not covered here.
        Map<ShapeId, Set<ShapeId>> lifecycleBoundByMember = getLifecycleBoundMembers(model);

        // Check every member to see if it's a potential reference.
        List<ValidationEvent> events = new ArrayList<>();
        for (MemberShape member : model.getMemberShapes()) {
            // Only the known identifier names can match for this, skip names that we don't know.
            Map<ShapeId, Set<ShapeId>> candidateTargets = resourceIdsByIdentifier.get(member.getMemberName());
            if (candidateTargets == null) {
                continue;
            }
            // Only strings can be identifiers, so skip non-String targets.
            if (!model.expectShape(member.getTarget()).isStringShape()) {
                continue;
            }

            Set<ShapeId> candidateResources = candidateTargets.get(member.getTarget());
            if (candidateResources == null) {
                continue;
            }

            Set<ShapeId> potentialReferences = computePotentialReferences(
                    model,
                    reverseProvider,
                    lifecycleBoundByMember,
                    member,
                    candidateResources);
            if (!potentialReferences.isEmpty()) {
                events.add(warning(member,
                        format("This member appears to reference the following resources without "
                                + "being included in a `@references` trait: [%s]",
                                ValidationUtils.tickedList(potentialReferences))));
            }
        }

        return events;
    }

    private Map<String, Map<ShapeId, Set<ShapeId>>> getResourceIdsByIdentifier(Model model) {
        Map<String, Map<ShapeId, Set<ShapeId>>> result = new HashMap<>();
        for (ResourceShape resource : model.getResourceShapes()) {
            for (Map.Entry<String, ShapeId> identifier : resource.getIdentifiers().entrySet()) {
                result.computeIfAbsent(identifier.getKey(), key -> new HashMap<>())
                        .computeIfAbsent(identifier.getValue(), key -> new HashSet<>())
                        .add(resource.getId());
            }
        }
        return result;
    }

    /**
     * Builds an index of {@code member ShapeId -> resources} for members that a resource lifecycle
     * trait ({@code @createsResources}, {@code @readsResources}, etc.) accounts for as identifying
     * a bound resource. A member is accounted for in two ways:
     *
     * <ul>
     *   <li>An explicit identifier path resolves to it (for example {@code path: "output.fooId"}
     *       resolves to the {@code fooId} member of whatever {@code output} targets).</li>
     *   <li>The binding names a resource and a direct member of the operation's input or output has
     *       the same name as one of that resource's identifiers. This mirrors how the standard
     *       lifecycle accounts for identifier-named members of a bound operation, so naming the
     *       resource is enough even without an explicit locator.</li>
     * </ul>
     *
     * <p>Either way an explicit binding suppresses this advisory just as {@code @references} would.
     * Only concrete members of the operation's own input or output are indexed. Paths that point
     * into a list element or through a {@code ...From} pointer designate members of nested element
     * structures rather than a member this validator flags, so they are intentionally left out.
     */
    private Map<ShapeId, Set<ShapeId>> getLifecycleBoundMembers(Model model) {
        Map<ShapeId, Set<ShapeId>> result = new HashMap<>();
        for (OperationShape operation : model.getOperationShapes()) {
            for (Trait applied : operation.getAllTraits().values()) {
                if (!(applied instanceof AbstractResourceLifecycleTrait)) {
                    continue;
                }
                AbstractResourceLifecycleTrait trait = (AbstractResourceLifecycleTrait) applied;
                // Identifiers live on the input for every lifecycle trait except create, which
                // returns generated identifiers on the output. Resolve against both sides: a path
                // only resolves to a real member of the correct side, and keying on the concrete
                // member id keeps this independent of the per-trait side rules in the validator.
                Shape input = model.getShape(operation.getInputShape()).orElse(null);
                Shape output = model.getShape(operation.getOutputShape()).orElse(null);
                for (ResourceLifecycleBinding binding : trait.getBindings()) {
                    for (ResourceMemberBinding locator : binding.getIdentifiers().values()) {
                        JmespathExpression path = parseQuietly(locator.getPath());
                        if (path == null) {
                            continue;
                        }
                        indexTerminalMember(model, input, path, binding.getResource(), result);
                        indexTerminalMember(model, output, path, binding.getResource(), result);
                    }
                    indexIdentifierNamedMembers(model, input, binding.getResource(), result);
                    indexIdentifierNamedMembers(model, output, binding.getResource(), result);
                }
            }
        }
        return result;
    }

    // Exempts direct members of the operation's input or output whose name matches an identifier of
    // the bound resource, even without an explicit locator: naming the resource is itself the
    // declaration that its identifier-named members refer to it.
    private void indexIdentifierNamedMembers(
            Model model,
            Shape structure,
            ShapeId resourceId,
            Map<ShapeId, Set<ShapeId>> index
    ) {
        if (structure == null) {
            return;
        }
        ResourceShape resource = model.getShape(resourceId).flatMap(Shape::asResourceShape).orElse(null);
        if (resource == null) {
            return;
        }
        for (String identifier : resource.getIdentifiers().keySet()) {
            structure.getMember(identifier)
                    .ifPresent(member -> index.computeIfAbsent(member.getId(), key -> new HashSet<>())
                            .add(resourceId));
        }
    }

    private void indexTerminalMember(
            Model model,
            Shape structure,
            JmespathExpression path,
            ShapeId resource,
            Map<ShapeId, Set<ShapeId>> index
    ) {
        if (structure == null) {
            return;
        }
        MemberShape member = ResourceLifecycleResolver.resolveTerminalMember(model, structure, path);
        if (member != null) {
            index.computeIfAbsent(member.getId(), key -> new HashSet<>()).add(resource);
        }
    }

    // Parses a path, returning null when it is not valid JMESPath. Invalid paths are reported by
    // the resource lifecycle trait validator; here they simply contribute no exemption.
    private static JmespathExpression parseQuietly(String path) {
        try {
            return JmespathExpression.parse(path);
        } catch (JmespathException e) {
            return null;
        }
    }

    private Set<ShapeId> computePotentialReferences(
            Model model,
            NeighborProvider reverseProvider,
            Map<ShapeId, Set<ShapeId>> lifecycleBoundByMember,
            MemberShape member,
            Set<ShapeId> candidateResources
    ) {
        // Exclude any resources already in `@references` on the member or container structure.
        Set<ShapeId> resourcesToIgnore = new HashSet<>();
        ignoreReferencedResources(member, resourcesToIgnore);
        ignoreReferencedResources(model.expectShape(member.getContainer()), resourcesToIgnore);

        // Exclude resources declared to be located at this member by a resource lifecycle trait.
        // This mirrors `@references`: the modeler has already stated that the member identifies the
        // resource, so no separate `@references` is needed.
        Set<ShapeId> lifecycleBound = lifecycleBoundByMember.get(member.getId());
        if (lifecycleBound != null) {
            resourcesToIgnore.addAll(lifecycleBound);
        }

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
        for (ShapeId resourceId : candidateResources) {
            // Exclude members bound to resource hierarchies from generating events,
            // including for resources that are within the same hierarchy.
            if (resourcesToIgnore.contains(resourceId)) {
                continue;
            }

            potentialResources.add(resourceId);
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

    private void ignoreReferencedResources(Shape shape, Set<ShapeId> resourcesToIgnore) {
        if (shape.hasTrait(ReferencesTrait.ID)) {
            for (ReferencesTrait.Reference reference : shape.expectTrait(ReferencesTrait.class)
                    .getReferences()) {
                resourcesToIgnore.add(reference.getResource());
            }
        }
    }

}
