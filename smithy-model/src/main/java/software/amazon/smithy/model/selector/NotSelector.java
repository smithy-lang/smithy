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
 * Filters out input that is returned from any of the given selectors.
 */
final class NotSelector implements Selector {
    private final List<Selector> selectors;

    NotSelector(List<Selector> selectors) {
        this.selectors = selectors;
    }

    @Override
    public Set<Shape> select(NeighborProvider neighborProvider, Set<Shape> shapes) {
        Set<Shape> result = new HashSet<>(shapes);
        for (Selector predicate : selectors) {
            result.removeAll(predicate.select(neighborProvider, shapes));
        }
        return result;
    }
}
