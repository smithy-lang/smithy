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
import java.util.Optional;
import java.util.Set;
import software.amazon.smithy.model.neighbor.NeighborProvider;
import software.amazon.smithy.model.neighbor.RelationshipType;
import software.amazon.smithy.model.shapes.Shape;

/**
 * Filters out members shapes that have a container shape that doesn't
 * match one of the given predicates.
 *
 * <p>For example, the following selector matches members that are the
 * member of a structure:
 *
 * <code>
 *     member:of(structure)
 * </code>
 */
final class OfSelector implements Selector {
    private final List<Selector> selectors;

    OfSelector(List<Selector> selectors) {
        this.selectors = selectors;
    }

    @Override
    public Set<Shape> select(NeighborProvider neighborProvider, Set<Shape> shapes) {
        Set<Shape> result = new HashSet<>();

        // Filter out non-member shapes, and member shapes that cannot
        // resolve their parents.
        shapes.stream().filter(Shape::isMemberShape).forEach(shape -> {
            findParent(neighborProvider, shape).ifPresent(parent -> {
                Set<Shape> parentSet = Set.of(parent);
                // If the parent provides a result for the predicate, then the
                // Shape is not filtered out.
                boolean anyMatch = selectors.stream()
                        .anyMatch(selector -> !selector.select(neighborProvider, parentSet).isEmpty());
                if (anyMatch) {
                    result.add(shape);
                }
            });
        });

        return result;
    }

    private Optional<Shape> findParent(NeighborProvider neighborProvider, Shape shape) {
        return neighborProvider.getNeighbors(shape).stream()
                .filter(rel -> rel.getRelationshipType() == RelationshipType.MEMBER_CONTAINER)
                .flatMap(rel -> rel.getNeighborShape().stream())
                .findFirst();
    }
}
