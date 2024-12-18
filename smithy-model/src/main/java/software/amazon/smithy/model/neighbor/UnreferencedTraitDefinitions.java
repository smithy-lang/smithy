/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.neighbor;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.NeighborProviderIndex;
import software.amazon.smithy.model.loader.Prelude;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.TraitDefinition;
import software.amazon.smithy.utils.FunctionalUtils;

/**
 * Finds trait definitions that are not connected to a service shape.
 *
 * <p>Prelude traits are never considered unreferenced.
 */
public final class UnreferencedTraitDefinitions {

    private final Predicate<Shape> keepFilter;

    public UnreferencedTraitDefinitions() {
        this(FunctionalUtils.alwaysTrue());
    }

    /**
     * @param keepFilter Predicate that if matched keeps a trait definition from being unreferenced.
     */
    public UnreferencedTraitDefinitions(Predicate<Shape> keepFilter) {
        this.keepFilter = keepFilter;
    }

    public Set<Shape> compute(Model model) {
        Walker walker = new Walker(NeighborProviderIndex.of(model).getProvider());

        // Begin with a mutable set of all trait definitions contained in the model
        Set<Shape> unused = model.getShapesWithTrait(TraitDefinition.class)
                .stream()
                // Exclude prelude traits -- these are defined by Smithy, not by the model itself
                .filter(FunctionalUtils.not(Prelude::isPreludeShape))
                .collect(Collectors.toSet());

        // Find all traits used directly or indirectly by a service shape and remove
        // their definitions from the unused set.
        model.shapes(ServiceShape.class)
                .flatMap(service -> walker.walkShapes(service).stream())
                .distinct()
                .map(Shape::getAllTraits)
                .flatMap(traits -> traits.keySet().stream())
                .distinct()
                .flatMap(traitId -> getTraitShapes(model, traitId).stream())
                .filter(keepFilter)
                .forEach(unused::remove);

        return unused;
    }

    private Collection<Shape> getTraitShapes(Model model, ShapeId traitId) {
        return getTraitShapes(model, traitId, new HashMap<>()).values();
    }

    private Map<ShapeId, Shape> getTraitShapes(Model model, ShapeId traitId, Map<ShapeId, Shape> traitShapes) {
        Optional<Shape> initialTraitShapeOp = model.getShape(traitId);
        if (initialTraitShapeOp.isPresent()) {
            Shape initialTraitShape = initialTraitShapeOp.get();
            traitShapes.put(traitId, initialTraitShape);
            for (ShapeId metaTraitId : initialTraitShape.getAllTraits().keySet()) {
                if (!metaTraitId.equals(TraitDefinition.ID) && !traitShapes.containsKey(metaTraitId)) {
                    traitShapes.putAll(getTraitShapes(model, metaTraitId, traitShapes));
                }
            }
        }
        return traitShapes;
    }
}
