/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.model.validation.node;

import java.util.function.BiConsumer;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.Shape;

abstract class FilteredPlugin<S extends Shape, N extends Node> implements NodeValidatorPlugin {
    private final Class<S> shapeClass;
    private final Class<N> nodeClass;

    FilteredPlugin(Class<S> shapeClass, Class<N> nodeClass) {
        this.shapeClass = shapeClass;
        this.nodeClass = nodeClass;
    }

    @Override
    @SuppressWarnings("unchecked")
    public final void apply(Shape shape, Node value, Model model, BiConsumer<FromSourceLocation, String> emitter) {
        if (shapeClass.isInstance(shape) && nodeClass.isInstance(value)) {
            check((S) shape, (N) value, model, emitter);
        }
    }

    abstract void check(S shape, N node, Model model, BiConsumer<FromSourceLocation, String> emitter);
}
