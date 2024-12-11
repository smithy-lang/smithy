/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.selector;

import java.util.HashSet;
import java.util.Set;
import software.amazon.smithy.model.shapes.Shape;

/**
 * Stores a variable in the {@link Context} object using a selector.
 *
 * <p>The result of evaluating the selector is stored in the {@code Context}
 * using the given variable name. This selector is much like the ':test()'
 * function in that it does not change the current node. Selectors run after
 * a {@code VariableStoreSelector} start processing shapes from the same
 * point that the variable capture occurred.
 */
final class VariableStoreSelector implements InternalSelector {
    private final String variableName;
    private final InternalSelector selector;

    VariableStoreSelector(String variableName, InternalSelector selector) {
        this.variableName = variableName;
        this.selector = selector;
    }

    @Override
    public Response push(Context context, Shape shape, Receiver next) {
        // Buffer the result of piping the shape through the selector
        // so that it can be retrieved through context vars.
        Set<Shape> captures = selector.pushResultsToCollection(context, shape, new HashSet<>());
        context.getVars().put(variableName, captures);

        // Now send the received shape to the next receiver.
        return next.apply(context, shape);
    }
}
