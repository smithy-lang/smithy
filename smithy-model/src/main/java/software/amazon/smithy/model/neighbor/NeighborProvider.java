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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeIndex;
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

    @Deprecated
    static NeighborProvider of(ShapeIndex index) {
        return new NeighborVisitor(index);
    }

    /**
     * Creates a default NeighborProvider for the given model.
     *
     * @param model Model to create a neighbor provider for.
     * @return Returns the created neighbor provider.
     */
    static NeighborProvider of(Model model) {
        return new NeighborVisitor(model);
    }

    @Deprecated
    static NeighborProvider precomputed(ShapeIndex index) {
        return precomputed(index, of(index));
    }

    /**
     * Creates a NeighborProvider that precomputes the neighbors of a model.
     *
     * @param model Model to create a neighbor provider for.
     * @return Returns the created neighbor provider.
     */
    static NeighborProvider precomputed(Model model) {
        return precomputed(model.getShapeIndex());
    }

    @Deprecated
    static NeighborProvider precomputed(ShapeIndex index, NeighborProvider provider) {
        Map<ShapeId, List<Relationship>> relationships = new HashMap<>();
        index.shapes().forEach(shape -> relationships.put(shape.getId(), provider.getNeighbors(shape)));
        return shape -> relationships.getOrDefault(shape.getId(), ListUtils.of());
    }

    /**
     * Creates a NeighborProvider that precomputes the neighbors of a model.
     *
     * @param model Model to create a neighbor provider for.
     * @param provider Provider to use when precomputing.
     * @return Returns the created neighbor provider.
     */
    static NeighborProvider precomputed(Model model, NeighborProvider provider) {
        return precomputed(model.getShapeIndex(), provider);
    }

    @Deprecated
    static NeighborProvider bottomUp(ShapeIndex index) {
        return reverse(index, of(index));
    }

    static NeighborProvider bottomUp(Model model) {
        return reverse(model.getShapeIndex(), of(model.getShapeIndex()));
    }

    @Deprecated
    static NeighborProvider reverse(ShapeIndex index, NeighborProvider topDown) {
        Map<ShapeId, List<Relationship>> targetedFrom = index.shapes()
                .map(topDown::getNeighbors)
                .flatMap(List::stream)
                .distinct()
                .collect(Collectors.groupingBy(Relationship::getNeighborShapeId, ListUtils.toUnmodifiableList()));

        return shape -> targetedFrom.getOrDefault(shape.getId(), ListUtils.of());
    }

    static NeighborProvider reverse(Model model, NeighborProvider topDown) {
        return reverse(model.getShapeIndex(), topDown);
    }
}
