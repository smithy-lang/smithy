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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.NeighborProviderIndex;
import software.amazon.smithy.model.shapes.Shape;

/**
 * Selector evaluation context object.
 */
final class Context {

    NeighborProviderIndex neighborIndex;
    private final Model model;
    private final Map<String, Set<Shape>> variables = new HashMap<>();
    private final List<Set<Shape>> roots;

    Context(Model model, NeighborProviderIndex neighborIndex, List<Set<Shape>> roots) {
        this.model = model;
        this.neighborIndex = neighborIndex;
        this.roots = roots;
    }

    /**
     * Gets the mutable map of captured variables.
     *
     * @return Returns the captured variables.
     */
    Map<String, Set<Shape>> getVars() {
        return variables;
    }

    Set<Shape> getRootResult(int index) {
        return roots.get(index);
    }

    Model getModel() {
        return model;
    }

    /**
     * Placeholder value used to check if a selector emits any values.
     */
    private static final class Holder implements InternalSelector.Receiver {
        boolean set;

        @Override
        public InternalSelector.Response apply(Context context, Shape shape) {
            set = true;
            // Stop receiving shapes once the first value is seen.
            return InternalSelector.Response.STOP;
        }
    }

    /**
     * Checks if the shape matches the predicate by detecting if the
     * predicate pushes any values when provided the shape.
     *
     * @param shape Shape to push to the given {@code predicate}.
     * @param predicate Predicate to test with the given {@code shape}.
     * @return Returns true if the {@code predicate} matches the {@code shape}.
     */
    boolean receivedShapes(Shape shape, InternalSelector predicate) {
        Holder holder = new Holder();
        predicate.push(this, shape, holder);
        return holder.set;
    }
}
