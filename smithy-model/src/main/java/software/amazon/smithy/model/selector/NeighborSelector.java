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

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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

    NeighborSelector(List<String> relTypes) {
        this.relTypes = relTypes;
    }

    @Override
    public Set<Shape> select(NeighborProvider neighborProvider, Set<Shape> shapes) {
        return shapes.stream()
                .flatMap(shape -> neighborProvider.getNeighbors(shape).stream().flatMap(this::mapNeighbor))
                .collect(Collectors.toSet());
    }

    private Stream<Shape> mapNeighbor(Relationship rel) {
        return rel.getNeighborShape()
                .flatMap(target -> createNeighbor(rel, target))
                .stream();
    }

    private Optional<Shape> createNeighbor(Relationship rel, Shape target) {
        if (rel.getRelationshipType() != RelationshipType.MEMBER_CONTAINER
                && (relTypes.isEmpty() || relTypes.contains(getRelType(rel.getRelationshipType())))) {
            return Optional.of(target);
        }

        return Optional.empty();
    }

    private static String getRelType(RelationshipType rel) {
        switch (rel) {
            case STRUCTURE_MEMBER:
            case UNION_MEMBER:
            case LIST_MEMBER:
            case MAP_KEY:
            case MAP_VALUE:
                return "member";
            default:
                return rel.toString().toLowerCase(Locale.US);
        }
    }
}
