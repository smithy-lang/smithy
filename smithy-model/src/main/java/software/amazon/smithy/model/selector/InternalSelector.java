/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.selector;

import java.util.Collection;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;

/**
 * An internal implementation of selectors.
 */
interface InternalSelector {

    /** A selector that always returns all provided values. */
    InternalSelector IDENTITY = (ctx, shape, next) -> next.apply(ctx, shape);

    /**
     * Sends a shape to the selector.
     *
     * <p>If the selector accepts the shape, then it sends 0 or
     * more shapes to the {@code next} receiver. The receiver, in turn,
     * then sends shapes to more receivers, and so on. A selector should
     * return true to continue to receive shapes.
     *
     * <p>Selectors that return false instruct other selectors to stop
     * sending shapes (for example, the {@link TestSelector} and
     * {@link NotSelector} stop accepting shapes as soon as any predicate
     * they test emits a shape). A false return value from a selector or
     * {@code next} receiver _does not_ cause the top-level shapes of a
     * model to stop being sent through the selector. Only selectors that
     * send multiple values in a loop like the {@link VariableGetSelector},
     * {@link NeighborSelector}, and {@link RecursiveNeighborSelector}
     * act on the return value. As soon as these selectors see a false return
     * value, they stop sending shapes to the {@code next} receiver, and
     * they, in turn, return false, propagating the signal to stop.
     *
     * @param ctx Context being evaluated.
     * @param shape Shape being pushed through the selector.
     * @param next Receiver to call 0 or more times.
     * @return Returns true to continue sending shapes to the selector.
     */
    Response push(Context ctx, Shape shape, Receiver next);

    /** Tells shape emitters whether to continue to send shapes to an InternalSelector or Receiver. */
    enum Response {
        CONTINUE,
        STOP
    }

    /**
     * Pushes {@code shape} through the selector and adds all results to {@code captures}.
     *
     * <p>This method exists because we've messed this up multiple times. When buffering values sent to a receiver,
     * you have to return true to keep getting results. It's easy to make a closure that just uses
     * {@link Collection#add(Object)}, but that will return false if the shape was already in the collection, which
     * isn't the desired behavior.
     *
     * @param context Context being evaluated.
     * @param shape Shape being pushed through the selector.
     * @param captures Where to buffer all results.
     * @param <C> Collection type that is given and returned.
     * @return Returns the given {@code captures} collection.
     */
    default <C extends Collection<Shape>> C pushResultsToCollection(Context context, Shape shape, C captures) {
        push(context, shape, (c, s) -> {
            captures.add(s);
            return Response.CONTINUE;
        });
        return captures;
    }

    /**
     * Returns the set of shapes to pump through the selector.
     *
     * <p>This method returns all shapes in the model by default. Some selectors can return a subset of shapes if
     * the selector can filter shapes more efficiently. For example, when selecting "structure", it is far less work
     * to leverage {@link Model#toSet(Class)} than it is to send every shape
     * through every selector.
     *
     * @return Returns the starting shapes to push through the selector.
     */
    default Collection<? extends Shape> getStartingShapes(Model model) {
        return model.toSet();
    }

    /**
     * The result of determining if a presence optimization can be made to find a shape.
     */
    enum ContainsShape {
        /** The shape is definitely in the selector. */
        YES,

        /** The shape is definitely not in the selector. */
        NO,

        /** No optimization could be made, so send every shape through to determine if the shape is present. */
        MAYBE
    }

    /**
     * Checks if the internal selector can quickly detect if it contains the given shape.
     *
     * @param context Evaluation context.
     * @param shape Shape to check.
     * @return Returns YES if the selector knows the shape is in the selector, NO if it isn't, and MAYBE if unknown.
     */
    default ContainsShape containsShapeOptimization(Context context, Shape shape) {
        return ContainsShape.MAYBE;
    }

    /**
     * Receives shapes from an InternalSelector.
     */
    interface Receiver {
        /**
         * Receive the {@code context} and {@code shape} from an
         * {@code InternalSelector} for processing.
         *
         * @param context Context being used in the evaluation.
         * @param shape Shape that is received.
         * @return Returns true to continue receiving shapes.
         */
        Response apply(Context context, Shape shape);
    }
}
