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

import java.util.Collections;
import java.util.Set;
import software.amazon.smithy.model.shapes.Shape;

/**
 * Pushes the shapes stored in a specific variable to the next selector.
 */
final class VariableGetSelector implements InternalSelector {
    private final String variableName;

    VariableGetSelector(String variableName) {
        this.variableName = variableName;
    }

    @Override
    public Response push(Context context, Shape shape, Receiver next) {
        // Do not fail on invalid variable access.
        for (Shape v : getShapes(context)) {
            if (next.apply(context, v) == Response.STOP) {
                // Propagate the signal to stop upstream.
                return Response.STOP;
            }
        }

        return Response.CONTINUE;
    }

    private Set<Shape> getShapes(Context context) {
        return context.getVars().getOrDefault(variableName, Collections.emptySet());
    }

    @Override
    public ContainsShape containsShapeOptimization(Context context, Shape shape) {
        return getShapes(context).contains(shape) ? ContainsShape.YES : ContainsShape.NO;
    }
}
