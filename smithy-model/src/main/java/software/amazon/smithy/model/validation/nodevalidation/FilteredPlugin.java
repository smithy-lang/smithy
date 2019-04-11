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

package software.amazon.smithy.model.validation.nodevalidation;

import java.util.List;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeIndex;
import software.amazon.smithy.utils.ListUtils;

abstract class FilteredPlugin<S extends Shape, N extends Node> implements NodeValidatorPlugin {
    private final Class<S> shapeClass;
    private final Class<N> nodeClass;

    FilteredPlugin(Class<S> shapeClass, Class<N> nodeClass) {
        this.shapeClass = shapeClass;
        this.nodeClass = nodeClass;
    }

    @SuppressWarnings("unchecked")
    public final List<String> apply(Shape shape, Node node, ShapeIndex index) {
        if (shapeClass.isInstance(shape) && nodeClass.isInstance(node)) {
            return check((S) shape, (N) node, index);
        } else {
            return ListUtils.of();
        }
    }

    abstract List<String> check(S shape, N node, ShapeIndex index);
}
