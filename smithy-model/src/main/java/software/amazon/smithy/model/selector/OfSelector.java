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
final class OfSelector implements InternalSelector {

    private final List<InternalSelector> selectors;

    OfSelector(List<InternalSelector> selectors) {
        this.selectors = selectors;
    }

    @Override
    public void push(Context context, Shape shape, BiConsumer<Context, Shape> next) {
        if (shape.isMemberShape() && isParentMatch(shape, context)) {
            next.accept(context, shape);
        }
    }

    private boolean isParentMatch(Shape shape, Context context) {
        Shape parent = findParent(context.neighborProvider, shape);

        // If the parent provides a result for the parent predicate, then
        // the Shape is not filtered out.
        return parent != null && isParentSelectionValid(parent, context);
    }

    private Shape findParent(NeighborProvider neighborProvider, Shape shape) {
        for (Relationship rel : neighborProvider.getNeighbors(shape)) {
            if (rel.getRelationshipType() == RelationshipType.MEMBER_CONTAINER) {
                return rel.getNeighborShape().orElse(null);
            }
        }

        return null;
    }

    private boolean isParentSelectionValid(Shape parent, Context context) {
        for (InternalSelector selector : selectors) {
            if (context.receivedShapes(parent, selector)) {
                return true;
            }
        }

        return false;
    }
}
