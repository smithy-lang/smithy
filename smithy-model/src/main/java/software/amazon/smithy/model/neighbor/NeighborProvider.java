/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.neighbor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.IdRefTrait;
import software.amazon.smithy.utils.ListUtils;

/**
 * Provides the neighbor relationships for a given shape.
 */
@FunctionalInterface
public interface NeighborProvider {
    /**
     * Gets the neighbor relationships of a shape.
     *
     * @param shape Shape to get neighbors for.
     * @return Returns the found neighbors.
     */
    List<Relationship> getNeighbors(Shape shape);

    /**
     * Creates a default NeighborProvider for the given model.
     *
     * @param model Model to create a neighbor provider for.
     * @return Returns the created neighbor provider.
     */
    static NeighborProvider of(Model model) {
        return new NeighborVisitor(model);
    }

    /**
     * Creates a NeighborProvider that precomputes the neighbors of a model.
     *
     * @param model Model to create a neighbor provider for.
     * @return Returns the created neighbor provider.
     */
    static NeighborProvider precomputed(Model model) {
        return precomputed(model, of(model));
    }

    /**
     * Creates a NeighborProvider that includes {@link RelationshipType#TRAIT}
     * relationships.
     *
     * @param model Model to use to look up trait shapes.
     * @param neighborProvider Provider to wrap.
     * @return Returns the created neighbor provider.
     */
    static NeighborProvider withTraitRelationships(Model model, NeighborProvider neighborProvider) {
        return shape -> {
            List<Relationship> relationships = neighborProvider.getNeighbors(shape);

            // Don't copy the array unless the shape has traits.
            if (shape.getAllTraits().isEmpty()) {
                return relationships;
            }

            // The delegate might have returned an immutable list, so copy first.
            relationships = new ArrayList<>(relationships);
            for (ShapeId trait : shape.getAllTraits().keySet()) {
                Relationship traitRel = model.getShape(trait)
                        .map(target -> Relationship.create(shape, RelationshipType.TRAIT, target))
                        .orElseGet(() -> Relationship.createInvalid(shape, RelationshipType.TRAIT, trait));
                relationships.add(traitRel);
            }

            return relationships;
        };
    }

    /**
     * Creates a NeighborProvider that includes {@link RelationshipType#ID_REF}
     * relationships.
     *
     * @param model Model to use to look up shapes referenced by {@link IdRefTrait}.
     * @param neighborProvider Provider to wrap.
     * @return Returns the created neighbor provider.
     */
    static NeighborProvider withIdRefRelationships(Model model, NeighborProvider neighborProvider) {
        Map<ShapeId, Set<Relationship>> idRefRelationships = new IdRefShapeRelationships(model).getRelationships();
        return shape -> {
            List<Relationship> relationships = neighborProvider.getNeighbors(shape);

            if (!idRefRelationships.containsKey(shape.getId())) {
                return relationships;
            }

            relationships = new ArrayList<>(relationships);
            relationships.addAll(idRefRelationships.get(shape.getId()));
            return relationships;
        };
    }

    /**
     * Creates a NeighborProvider that precomputes the neighbors of a model.
     *
     * @param model Model to create a neighbor provider for.
     * @param provider Provider to use when precomputing.
     * @return Returns the created neighbor provider.
     */
    static NeighborProvider precomputed(Model model, NeighborProvider provider) {
        Set<Shape> shapes = model.toSet();
        Map<Shape, List<Relationship>> relationships = new HashMap<>(shapes.size());
        for (Shape shape : shapes) {
            relationships.put(shape, provider.getNeighbors(shape));
        }
        return shape -> relationships.getOrDefault(shape, ListUtils.of());
    }

    /**
     * Returns a NeighborProvider that returns relationships that point at a
     * given shape rather than relationships that the given shape points at.
     *
     * @param model Model to build reverse relationships from.
     * @return Returns the reverse neighbor provider.
     */
    static NeighborProvider reverse(Model model) {
        return reverse(model, of(model));
    }

    /**
     * Returns a NeighborProvider that returns relationships that point at a
     * given shape rather than relationships that the given shape points at.
     *
     * @param model Model to build reverse relationships from.
     * @param forwardProvider The forward directed neighbor provider to grab relationships from.
     * @return Returns the reverse neighbor provider.
     */
    static NeighborProvider reverse(Model model, NeighborProvider forwardProvider) {
        // Note: this method previously needed lots of intermediate representations
        // stored in memory to create a Map<ShapeId, List<RelationShip>> that contains
        // only unique relationships. It was done using Stream, distinct, and groupingBy.
        // However, when trying to load ridiculously large models, that approach consumes
        // tons of heap. This approach allocates as little as possible (I think), but
        // does require creating an ArrayList copy of a Set each time neighbors are returned.
        Map<ShapeId, Set<Relationship>> targetedFrom = new HashMap<>();

        for (Shape shape : model.toSet()) {
            for (Relationship rel : forwardProvider.getNeighbors(shape)) {
                targetedFrom.computeIfAbsent(rel.getNeighborShapeId(), id -> new HashSet<>()).add(rel);
            }
        }

        return shape -> {
            Set<Relationship> shapes = targetedFrom.get(shape.getId());
            return shapes == null ? Collections.emptyList() : ListUtils.copyOf(shapes);
        };
    }

    /**
     * Caches the results of calling a delegate provider.
     *
     * @param provider Provider to delegate to and cache.
     * @return Returns the thread-safe caching neighbor provider.
     */
    static NeighborProvider cached(NeighborProvider provider) {
        Map<Shape, List<Relationship>> relationships = new ConcurrentHashMap<>();
        return shape -> relationships.computeIfAbsent(shape, provider::getNeighbors);
    }
}
