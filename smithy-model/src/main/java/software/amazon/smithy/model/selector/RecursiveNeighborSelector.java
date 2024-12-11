/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.selector;

import java.util.Iterator;
import java.util.function.Predicate;
import software.amazon.smithy.model.neighbor.Relationship;
import software.amazon.smithy.model.neighbor.RelationshipDirection;
import software.amazon.smithy.model.neighbor.Walker;
import software.amazon.smithy.model.shapes.Shape;

/**
 * Find all shapes recursively connected to a shape using directed edges.
 */
final class RecursiveNeighborSelector implements InternalSelector {

    private static final Predicate<Relationship> ONLY_DIRECTED = r -> {
        // Don't crawl up from members to their containers.
        return r.getRelationshipType().getDirection() == RelationshipDirection.DIRECTED;
    };

    @Override
    public Response push(Context context, Shape shape, Receiver next) {
        Walker walker = new Walker(context.neighborIndex.getProvider());
        Iterator<Shape> shapeIterator = walker.iterateShapes(shape, ONLY_DIRECTED);

        while (shapeIterator.hasNext()) {
            Shape nextShape = shapeIterator.next();
            // Don't include the shape being visited.
            if (!nextShape.equals(shape)) {
                if (next.apply(context, nextShape) == Response.STOP) {
                    // Stop sending recursive neighbors when told to stop and propagate.
                    return Response.STOP;
                }
            }
        }

        return Response.CONTINUE;
    }
}
