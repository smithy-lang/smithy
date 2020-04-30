/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import java.util.function.BiConsumer;
import software.amazon.smithy.model.neighbor.NeighborProvider;
import software.amazon.smithy.model.neighbor.Relationship;
import software.amazon.smithy.model.neighbor.RelationshipType;
import software.amazon.smithy.model.shapes.Shape;

abstract class AbstractNeighborSelector implements InternalSelector {

    private final List<String> relTypes;
    private final boolean includeTraits;

    AbstractNeighborSelector(List<String> relTypes) {
        this.relTypes = relTypes;
        includeTraits = relTypes.contains("trait");
    }

    @Override
    public final void push(Context context, Shape shape, BiConsumer<Context, Shape> next) {
        NeighborProvider resolvedProvider = getNeighborProvider(context, includeTraits);
        for (Relationship rel : resolvedProvider.getNeighbors(shape)) {
            if (matches(rel)) {
                emitMatchingRel(context, rel, next);
            }
        }
    }

    abstract NeighborProvider getNeighborProvider(Context context, boolean includeTraits);

    abstract void emitMatchingRel(Context context, Relationship rel, BiConsumer<Context, Shape> next);

    private boolean matches(Relationship rel) {
        return rel.getNeighborShape().isPresent()
               && rel.getRelationshipType() != RelationshipType.MEMBER_CONTAINER
               && (relTypes.isEmpty() || relTypes.contains(getRelType(rel)));
    }

    private static String getRelType(Relationship rel) {
        return rel.getSelectorLabel().orElse("");
    }
}
