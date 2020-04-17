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

package software.amazon.smithy.model.selector;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.neighbor.NeighborProvider;
import software.amazon.smithy.model.neighbor.Relationship;
import software.amazon.smithy.model.neighbor.RelationshipType;
import software.amazon.smithy.model.shapes.Shape;

/**
 * Traverses into the neighbors of shapes with an optional list of
 * neighbor rel filters.
 */
final class NeighborSelector implements Selector {

    private final List<String> relTypes;
    private final boolean includeTraits;

    NeighborSelector(List<String> relTypes) {
        this.relTypes = relTypes;
        includeTraits = relTypes.contains("trait");
    }

    @Override
    public Set<Shape> select(Model model, NeighborProvider neighborProvider, Set<Shape> shapes) {
        NeighborProvider resolvedProvider = createProvider(model, neighborProvider);

        Set<Shape> result = new HashSet<>();
        for (Shape shape : shapes) {
            for (Relationship rel : resolvedProvider.getNeighbors(shape)) {
                if (rel.getNeighborShape().isPresent()) {
                    Shape target = createNeighbor(rel, rel.getNeighborShape().get());
                    if (target != null) {
                        result.add(target);
                    }
                }
            }
        }

        return result;
    }

    // Enable trait relationships only if explicitly asked for in a selector.
    private NeighborProvider createProvider(Model model, NeighborProvider neighborProvider) {
        return includeTraits
               ? NeighborProvider.withTraitRelationships(model, neighborProvider)
               : neighborProvider;
    }

    private Shape createNeighbor(Relationship rel, Shape target) {
        if (rel.getRelationshipType() != RelationshipType.MEMBER_CONTAINER
                && (relTypes.isEmpty() || relTypes.contains(getRelType(rel)))) {
            return target;
        }

        return null;
    }

    private static String getRelType(Relationship rel) {
        return rel.getSelectorLabel().orElse("");
    }
}
