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

import java.util.function.BiConsumer;
import software.amazon.smithy.model.neighbor.Walker;
import software.amazon.smithy.model.shapes.Shape;

/**
 * Uses a {@link Walker} to find all shapes connected to the set of
 * given shapes.
 */
final class RecursiveNeighborSelector implements InternalSelector {
    @Override
    public void push(Context context, Shape shape, BiConsumer<Context, Shape> next) {
        Walker walker = new Walker(context.neighborIndex.getProvider());

        for (Shape nextShape : walker.walkShapes(shape)) {
            // Don't include the shape being visited.
            if (!nextShape.equals(shape)) {
                next.accept(context, nextShape);
            }
        }
    }
}
