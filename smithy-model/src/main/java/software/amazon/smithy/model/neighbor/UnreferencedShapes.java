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

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.NeighborProviderIndex;
import software.amazon.smithy.model.loader.Prelude;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.TraitDefinition;
import software.amazon.smithy.utils.FunctionalUtils;

/**
 * Finds shapes that are not connected to a service shape, are not trait
 * definitions, are not referenced by trait definitions, and are not
 * referenced in trait values through
 * {@link software.amazon.smithy.model.traits.IdRefTrait}.
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
        NeighborProvider baseProvider = NeighborProviderIndex.of(model).getProvider();
        NeighborProvider providerWithIdRefRelationships = NeighborProvider.withIdRefRelationships(model, baseProvider);
        Walker shapeWalker = new Walker(providerWithIdRefRelationships);

        // Find all shapes connected to any service shape.
        Set<Shape> connected = new HashSet<>();
        for (Shape service : model.getServiceShapes()) {
            connected.addAll(shapeWalker.walkShapes(service));
        }

        // Don't remove shapes that are traits or connected to traits.
        for (Shape trait : model.getShapesWithTrait(TraitDefinition.class)) {
            connected.addAll(shapeWalker.walkShapes(trait));
        }

        // Any shape that wasn't identified as connected to a service is considered unreferenced.
        Set<Shape> result = new HashSet<>();
        for (Shape shape : model.toSet()) {
            if (!shape.isMemberShape()
                    && !connected.contains(shape)
                    && !Prelude.isPreludeShape(shape)
                    && keepFilter.test(shape)) {
                result.add(shape);
            }
        }

        return result;
    }
}
