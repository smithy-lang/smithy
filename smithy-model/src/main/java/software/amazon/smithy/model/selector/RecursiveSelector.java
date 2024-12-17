/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.selector;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;

final class RecursiveSelector implements InternalSelector {

    private final InternalSelector selector;

    RecursiveSelector(InternalSelector selector) {
        this.selector = selector;
    }

    @Override
    public Response push(Context context, Shape shape, Receiver next) {
        // This queue contains the shapes that have yet to have the selector applied to them.
        QueueReceiver queueReceiver = new QueueReceiver(next);
        queueReceiver.queue.add(shape);

        while (!queueReceiver.queue.isEmpty()) {
            Shape match = queueReceiver.queue.pop();
            // Apply the selector to the queue, it will send results downstream immediately, and can ask to stop early.
            if (selector.push(context, match, queueReceiver) == Response.STOP) {
                return Response.STOP;
            }
        }

        return Response.CONTINUE;
    }

    private static final class QueueReceiver implements Receiver {

        final Deque<Shape> queue = new ArrayDeque<>();
        private final Set<ShapeId> visited = new HashSet<>();
        private final Receiver next;

        QueueReceiver(Receiver next) {
            this.next = next;
        }

        @Override
        public Response apply(Context context, Shape matchedShapeFromSelector) {
            // This method receives each shape matched by the selector of RecursiveSelector.
            if (visited.add(matchedShapeFromSelector.getId())) {
                // Send the match downstream right away to do as little work as possible.
                // For example, in `:recursive(-[mixin]->) :test([id=foo#Bar])`, when a match is found, the recursive
                // function can stop finding more mixins.
                if (next.apply(context, matchedShapeFromSelector) == Response.STOP) {
                    return Response.STOP;
                }
                // Enqueue the shape so that the outer loop can send this match back into the selector.
                queue.add(matchedShapeFromSelector);
            }

            return Response.CONTINUE;
        }
    }
}
