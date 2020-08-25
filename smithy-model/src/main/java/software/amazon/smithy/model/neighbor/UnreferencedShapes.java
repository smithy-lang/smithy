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

import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.NeighborProviderIndex;
import software.amazon.smithy.model.loader.Prelude;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.TraitDefinition;
import software.amazon.smithy.utils.FunctionalUtils;

/**
 * Finds shapes that are not connected to a service shape, are not trait
 * definitions, and are not referenced by trait definitions.
 *
 * <p>Prelude shapes are never considered unreferenced.
 */
public final class UnreferencedShapes {

    private final Predicate<Shape> keepFilter;

    public UnreferencedShapes() {
        this(FunctionalUtils.alwaysTrue());
    }

    /**
     * @param keepFilter Predicate that if matched keeps a shape from being unreferenced.
     */
    public UnreferencedShapes(Predicate<Shape> keepFilter) {
        this.keepFilter = keepFilter;
    }

    /**
     * Gets the set of shapes that are unreferenced.
     *
     * @param model Model to compute from.
     * @return Returns the unreferenced shapes.
     */
    public Set<Shape> compute(Model model) {
        Walker shapeWalker = new Walker(NeighborProviderIndex.of(model).getProvider());

        // Find all shapes connected to any service shape.
        Set<Shape> connected = model.shapes(ServiceShape.class)
                .flatMap(service -> shapeWalker.walkShapes(service).stream())
                .collect(Collectors.toSet());

        // Don't remove shapes that are traits or connected to traits.
        model.shapes()
                .filter(shape -> shape.hasTrait(TraitDefinition.class))
                .flatMap(shape -> shapeWalker.walkShapes(shape).stream())
                .forEach(connected::add);

        // Any shape that wasn't identified as connected to a service is considered unreferenced.
        return model.shapes()
                .filter(FunctionalUtils.not(Shape::isMemberShape))
                .filter(FunctionalUtils.not(connected::contains))
                // Retain prelude shapes
                .filter(FunctionalUtils.not(Prelude::isPreludeShape))
                .filter(keepFilter)
                .collect(Collectors.toSet());
    }
}
