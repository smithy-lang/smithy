/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.model.neighbor;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.NeighborProviderIndex;
import software.amazon.smithy.model.loader.Prelude;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeIndex;

/**
 * Finds shapes that are not connected to a service shape.
 */
public final class UnreferencedShapes {

    private final Predicate<Shape> keepFilter;

    public UnreferencedShapes() {
        this(shape -> true);
    }

    /**
     * @param keepFilter Predicate that if matched keeps a shape from being unreferenced.
     */
    public UnreferencedShapes(Predicate<Shape> keepFilter) {
        this.keepFilter = keepFilter;
    }

    public Set<Shape> compute(Model model) {
        Walker shapeWalker = new Walker(model.getKnowledge(NeighborProviderIndex.class).getProvider());
        // Find all shapes connected to any service shape.
        Set<Shape> connected = model.getShapeIndex().shapes(ServiceShape.class)
                .flatMap(service -> shapeWalker.walkShapes(service).stream())
                .collect(Collectors.toSet());
        Predicate<Shape> matchesFilters = createPredicate(model, shapeWalker);
        // Any shape that wasn't identified as connected to a service is considered unreferenced.
        return model.getShapeIndex().shapes()
                .filter(Predicate.not(Shape::isMemberShape))
                .filter(Predicate.not(connected::contains))
                .filter(matchesFilters)
                .filter(keepFilter)
                .collect(Collectors.toSet());
    }

    private Predicate<Shape> createPredicate(Model model, Walker walker) {
        Map<String, Set<ShapeId>> traitShapes = findTraitShapes(model, walker);
        // Retain prelude shapes
        Predicate<Shape> predicate = Predicate.not(Prelude::isPreludeShape);
        // Consider any shape used in a trait definition to be referenced.
        Set<ShapeId> allTraitShapes = traitShapes.values().stream()
                .flatMap(Set::stream)
                .collect(Collectors.toSet());

        return predicate.and(shape -> !allTraitShapes.contains(shape.getId()));
    }

    private Map<String, Set<ShapeId>> findTraitShapes(Model model, Walker walker) {
        ShapeIndex index = model.getShapeIndex();
        Map<String, Set<ShapeId>> result = new HashMap<>();
        model.getTraitDefinitions().forEach(def -> {
            def.getShape().flatMap(index::getShape).ifPresent(shape -> {
                result.put(def.getName(), findTraitNeighbors(shape, walker));
            });
        });
        return result;
    }

    private Set<ShapeId> findTraitNeighbors(Shape shape, Walker walker) {
        Set<ShapeId> result = walker.walkShapes(shape).stream().map(Shape::getId).collect(Collectors.toSet());
        result.add(shape.getId());
        return result;
    }
}
