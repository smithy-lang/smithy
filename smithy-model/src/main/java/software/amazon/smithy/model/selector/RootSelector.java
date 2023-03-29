/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
 * Root expressions are rooted common subexpressions.
 *
 * <p>Roots are evaluated eagerly and then the result is retrieved by ID. This prevents needing to evaluate a root
 * expression over and over for each shape given the result does not vary based on the current shape.
 * Roots are evaluated in an isolated context, meaning it can't use variables defined outside the root, nor can it
 * set variables that can be used outside the root.
 */
final class RootSelector implements InternalSelector {

    private final InternalSelector selector;
    private final int id;

    RootSelector(InternalSelector selector, int id) {
        this.selector = selector;
        this.id = id;
    }

    @Override
    public Response push(Context context, Shape shape, Receiver next) {
        for (Shape v : context.getRootResult(id)) {
            if (next.apply(context, v) == Response.STOP) {
                return Response.STOP;
            }
        }

        return Response.CONTINUE;
    }

    @Override
    public ContainsShape containsShapeOptimization(Context context, Shape shape) {
        return context.getRootResult(id).contains(shape) ? ContainsShape.YES : ContainsShape.NO;
    }
}
