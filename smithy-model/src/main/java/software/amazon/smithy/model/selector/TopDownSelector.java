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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import software.amazon.smithy.model.neighbor.Relationship;
import software.amazon.smithy.model.neighbor.RelationshipType;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;

final class TopDownSelector implements InternalSelector {
    private final InternalSelector qualifier;
    private final InternalSelector disqualifier;

    TopDownSelector(List<InternalSelector> selectors) {
        this.qualifier = selectors.get(0);
        disqualifier = selectors.size() > 1 ? selectors.get(1) : null;
    }

    @Override
    public Response push(Context context, Shape shape, Receiver next) {
        if (shape.isServiceShape() || shape.isResourceShape() || shape.isOperationShape()) {
            return pushMatch(false, context, shape, next, new HashSet<>());
        }

        return Response.CONTINUE;
    }

    // While a model can't contain recursive resource references, a custom
    // validator might use the :topdown selector function on a model with
    // recursive references. Custom validators are applied before resource
    // cycles are detected, meaning this function needs to protect against
    // recursion.
    private Response pushMatch(boolean qualified, Context context, Shape shape, Receiver next, Set<ShapeId> visited) {
        if (visited.contains(shape.getId())) {
            return Response.CONTINUE;
        }

        visited.add(shape.getId());

        // If the flag isn't set, then check if this shape sets it to true.
        if (!qualified && context.receivedShapes(shape, qualifier)) {
            qualified = true;
        }

        // If the flag is set, then check if any predicates unset it.
        if (qualified && disqualifier != null && context.receivedShapes(shape, disqualifier)) {
            qualified = false;
        }

        // If the shape is matched, then it's sent to the next receiver.
        if (qualified && next.apply(context, shape) == Response.STOP) {
            return Response.STOP; // fast-fail if the receiver fast-fails.
        }

        // Recursively check each nested resource/operation.
        for (Relationship rel : context.neighborIndex.getProvider().getNeighbors(shape)) {
            if (rel.getNeighborShape().isPresent() && !rel.getNeighborShapeId().equals(shape.getId())) {
                if (rel.getRelationshipType() == RelationshipType.RESOURCE
                        || rel.getRelationshipType() == RelationshipType.OPERATION) {
                    if (pushMatch(qualified, context, rel.getNeighborShape().get(), next, visited) == Response.STOP) {
                        return Response.STOP;
                    }
                }
            }
        }

        return Response.CONTINUE;
    }
}
