/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.knowledge;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.Prelude;
import software.amazon.smithy.model.neighbor.Relationship;
import software.amazon.smithy.model.neighbor.RelationshipDirection;
import software.amazon.smithy.model.neighbor.Walker;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.UnstableFeaturesTrait;
import software.amazon.smithy.model.traits.UnstableFeaturesTrait.UnstableFeatureInfo;
import software.amazon.smithy.model.traits.UnstableFeaturesTrait.UnstableReason;
import software.amazon.smithy.model.traits.UnstableTrait;

/**
 * Resolves the unstable feature a shape belongs to.
 *
 * <p>A shape carrying the {@code unstable} trait with a {@code featureId} is a feature <em>owner</em>. A
 * shape belongs to an owner's feature when the owner encloses it.
 */
public final class UnstableFeatureIndex implements KnowledgeIndex {

    // Owners that enclose each shape. An owner encloses itself.
    private final Map<ShapeId, Set<ShapeId>> ownersByShape;

    // Services each owner resides in. Only populated for owners.
    private final Map<ShapeId, Set<ShapeId>> servicesByOwner;

    // Each owner's feature metadata, resolved from the services that enclose it.
    private final Map<ShapeId, UnstableFeatureInfo> featureByOwner;

    public UnstableFeatureIndex(Model model) {
        Set<ShapeId> unstableFeatureOwners = new TreeSet<>();
        for (Shape shape : model.getShapesWithTrait(UnstableTrait.class)) {
            if (shape.expectTrait(UnstableTrait.class).getFeatureId().isPresent()) {
                unstableFeatureOwners.add(shape.getId());
            }
        }

        if (unstableFeatureOwners.isEmpty()) {
            this.ownersByShape = Collections.emptyMap();
            this.servicesByOwner = Collections.emptyMap();
            this.featureByOwner = Collections.emptyMap();
            return;
        }

        Walker walker = new Walker(model);
        Set<ShapeId> shapesUnderStableClosure = findShapesUnderStableClosure(model, walker, unstableFeatureOwners);
        this.servicesByOwner = mapOwnersToServices(model, walker, unstableFeatureOwners);
        this.ownersByShape = mapShapesToOwners(model, walker, unstableFeatureOwners, shapesUnderStableClosure);
        this.featureByOwner = resolveFeatures(model, unstableFeatureOwners);
    }

    public static UnstableFeatureIndex of(Model model) {
        return model.getKnowledge(UnstableFeatureIndex.class, UnstableFeatureIndex::new);
    }

    /**
     * Gets the owners of the feature the given shape belongs to.
     *
     * <p>An owner is a shape carrying {@code unstable} with a {@code featureId}, and an owner encloses
     * itself. An empty result means the shape belongs to no feature. Exactly one owner means the shape
     * belongs to that owner's feature. More than one owner means either the shape is shared between features
     * or, when the shape is itself an owner, that it is nested within another feature.
     *
     * @param shape Shape to resolve.
     * @return Returns the enclosing owners, or an empty set if the shape belongs to no feature.
     */
    public Set<ShapeId> getFeatureOwners(ShapeId shape) {
        return Collections.unmodifiableSet(ownersByShape.getOrDefault(shape, Collections.emptySet()));
    }

    /**
     * Gets the services the given feature owner resides in.
     *
     * <p>An owner that is itself a service resides in itself. This is only populated for owners, since a
     * feature id is resolved against the {@code unstableFeatures} trait of the services its owner resides
     * in. An owner nested inside another feature still reports the services it resides in.
     *
     * @param owner Feature owner to resolve.
     * @return Returns the services the owner resides in, or an empty set if it is bound to no service.
     */
    public Set<ShapeId> getContainingServices(ShapeId owner) {
        return Collections.unmodifiableSet(servicesByOwner.getOrDefault(owner, Collections.emptySet()));
    }

    /**
     * Gets the metadata of the feature the given shape belongs to.
     *
     * <p>The result is empty unless the shape belongs to exactly one feature whose id resolves to a single
     * {@code unstableFeatures} entry, so an ambiguous model resolves to no feature rather than to an
     * arbitrary one of the candidates.
     *
     * @param shape Shape to resolve.
     * @return Returns the feature metadata, or empty if it does not resolve.
     */
    public Optional<UnstableFeatureInfo> getFeature(ShapeId shape) {
        Set<ShapeId> shapeOwners = ownersByShape.getOrDefault(shape, Collections.emptySet());
        if (shapeOwners.size() != 1) {
            return Optional.empty();
        }
        return Optional.ofNullable(featureByOwner.get(shapeOwners.iterator().next()));
    }

    /**
     * Returns whether the given shape belongs to a feature that is in preview.
     *
     * @param shape Shape to check.
     * @return True if the shape's feature resolves to a {@code PREVIEW} reason.
     */
    public boolean isInPreviewClosure(ShapeId shape) {
        return getFeature(shape)
                .flatMap(UnstableFeatureInfo::getReason)
                .filter(reason -> reason == UnstableReason.PREVIEW)
                .isPresent();
    }

    // Relates each owner to the services it resides in, which is how its featureId finds the
    // unstableFeatures traits it must be declared in. Walks the full closure of every service, without
    // stopping at owners, so that an owner is related to its services even when another owner encloses it.
    // walkShapeIds includes the starting shape, so an owner that is itself a service resides in itself.
    private static Map<ShapeId, Set<ShapeId>> mapOwnersToServices(
            Model model,
            Walker walker,
            Set<ShapeId> owners
    ) {
        Map<ShapeId, Set<ShapeId>> servicesByOwner = new HashMap<>();

        for (Shape service : model.getServiceShapes()) {
            for (ShapeId shapeId : walker.walkShapeIds(service, UnstableFeatureIndex::isContainment)) {
                if (owners.contains(shapeId)) {
                    add(servicesByOwner, shapeId, service.getId());
                }
            }
        }

        return servicesByOwner;
    }

    // Finds the shapes that stable (non-preview) code can reach. What a service that is not itself an owner
    // reaches without passing through an owner is used by stable code, so a preview owner that also reaches
    // it must not treat it as preview.
    private static Set<ShapeId> findShapesUnderStableClosure(Model model, Walker walker, Set<ShapeId> owners) {
        Set<ShapeId> shapesUnderStableClosure = new LinkedHashSet<>();

        for (Shape service : model.getServiceShapes()) {
            if (!owners.contains(service.getId())) {
                shapesUnderStableClosure.addAll(walker.walkShapeIds(service,
                        relationship -> isContainment(relationship)
                                && !owners.contains(relationship.getNeighborShapeId())));
            }
        }

        return shapesUnderStableClosure;
    }

    // Relates each shape to the owners that enclose it, stopping at shapes under the stable closure and at
    // other owners. A nested owner is recorded before stopping, so a shape that ends up with more than one
    // owner is either shared between features or nested within one.
    private static Map<ShapeId, Set<ShapeId>> mapShapesToOwners(
            Model model,
            Walker walker,
            Set<ShapeId> owners,
            Set<ShapeId> shapesUnderStableClosure
    ) {
        Map<ShapeId, Set<ShapeId>> ownersByShape = new HashMap<>();

        for (ShapeId ownerId : owners) {
            Shape owner = model.getShape(ownerId).orElse(null);
            if (owner == null) {
                continue;
            }

            // walkShapeIds includes the starting shape, so an owner always owns itself.
            Set<ShapeId> closure = walker.walkShapeIds(owner, relationship -> {
                if (!isContainment(relationship)) {
                    return false;
                }
                ShapeId neighbor = relationship.getNeighborShapeId();
                if (shapesUnderStableClosure.contains(neighbor)) {
                    return false;
                }
                if (owners.contains(neighbor)) {
                    add(ownersByShape, neighbor, ownerId);
                    return false;
                }
                return true;
            });

            for (ShapeId shapeId : closure) {
                add(ownersByShape, shapeId, ownerId);
            }
        }

        return ownersByShape;
    }

    // Resolves each owner's featureId against the unstableFeatures traits of the services that enclose it.
    private Map<ShapeId, UnstableFeatureInfo> resolveFeatures(Model model, Set<ShapeId> owners) {
        Map<ShapeId, UnstableFeatureInfo> featureByOwner = new HashMap<>();

        for (ShapeId ownerId : owners) {
            String featureId = model.getShape(ownerId)
                    .flatMap(owner -> owner.getTrait(UnstableTrait.class))
                    .flatMap(UnstableTrait::getFeatureId)
                    .orElse(null);
            if (featureId == null) {
                continue;
            }

            Set<UnstableFeatureInfo> candidates = new LinkedHashSet<>();
            for (ShapeId serviceId : getContainingServices(ownerId)) {
                model.getShape(serviceId)
                        .flatMap(service -> service.getTrait(UnstableFeaturesTrait.class))
                        .flatMap(trait -> trait.getFeature(featureId))
                        .ifPresent(candidates::add);
            }

            if (candidates.size() == 1) {
                featureByOwner.put(ownerId, candidates.iterator().next());
            }
        }

        return featureByOwner;
    }

    // Containment and binding edges (service to operation, operation to input / output, structure to member,
    // member to target, and so on) are DIRECTED.
    private static boolean isContainment(Relationship relationship) {
        return relationship.getDirection() == RelationshipDirection.DIRECTED
                && !Prelude.isPreludeShape(relationship.getNeighborShapeId());
    }

    private static void add(Map<ShapeId, Set<ShapeId>> map, ShapeId key, ShapeId value) {
        map.computeIfAbsent(key, id -> new LinkedHashSet<>()).add(value);
    }
}
