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

package software.amazon.smithy.model.selector;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import software.amazon.smithy.model.neighbor.NeighborProvider;
import software.amazon.smithy.model.shapes.Shape;

/**
 * Filters out shapes that do not match any predicates.
 *
 * <p>The result of this selector is always a subset of the input
 * (i.e., it does not map over the input).
 */
final class TestSelector implements Selector {
    private final List<Selector> selectors;

    TestSelector(List<Selector> selectors) {
        this.selectors = selectors;
    }

    @Override
    public Set<Shape> select(NeighborProvider neighborProvider, Set<Shape> shapes) {
        Set<Shape> result = new HashSet<>();
        for (Shape shape : shapes) {
            Set<Shape> attempt = Set.of(shape);
            for (Selector predicate : selectors) {
                if (predicate.select(neighborProvider, attempt).size() > 0) {
                    result.add(shape);
                    break;
                }
            }
        }

        return result;
    }
}
