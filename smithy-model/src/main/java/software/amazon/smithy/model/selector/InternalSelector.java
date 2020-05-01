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
     * {@link AbstractNeighborSelector}, and {@link RecursiveNeighborSelector}
     * act on the return value. As soon as these selectors see a false return
     * value, they stop sending shapes to the {@code next} receiver, and
     * they, in turn, return false, propagating the signal to stop.
     *
     * @param ctx Context being evaluated.
     * @param shape Shape being pushed through the selector.
     * @param next Receiver to call 0 or more times.
     * @return Returns true to continue sending shapes to the selector.
     */
    boolean push(Context ctx, Shape shape, Receiver next);

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
        boolean apply(Context context, Shape shape);
    }
}
