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

import java.util.HashSet;
import java.util.Set;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.neighbor.NeighborProvider;
import software.amazon.smithy.model.neighbor.Walker;
import software.amazon.smithy.model.shapes.Shape;

/**
 * Uses a {@link Walker} to find all shapes connected to the set of
 * given shapes.
 */
final class RecursiveNeighborSelector implements Selector {
    @Override
    public Set<Shape> select(Model model, NeighborProvider neighborProvider, Set<Shape> shapes) {
        Walker walker = new Walker(neighborProvider);

        Set<Shape> result = new HashSet<>();
        for (Shape shape : shapes) {
            for (Shape rel : walker.walkShapes(shape)) {
                // Don't include the shape being visited.
                if (!rel.getId().equals(shape.getId())) {
                    result.add(rel);
                }
            }
        }

        return result;
    }
}
