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

import java.util.List;
import java.util.Set;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.neighbor.NeighborProvider;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.utils.SetUtils;

/**
 * Maps input over a list of functions, passing the result of each to
 * the next.
 *
 * <p>The list of selectors is short-circuited if any selector returns
 * an empty result.
 */
final class AndSelector implements Selector {
    private final List<Selector> selectors;

    private AndSelector(List<Selector> predicates) {
        this.selectors = predicates;
    }

    static Selector of(List<Selector> predicates) {
        return predicates.size() == 1 ? predicates.get(0) : new AndSelector(predicates);
    }

    @Override
    public Set<Shape> select(Model model, NeighborProvider neighborProvider, Set<Shape> shapes) {
        for (Selector selector : selectors) {
            shapes = selector.select(model, neighborProvider, shapes);
            if (shapes.isEmpty()) {
                return SetUtils.of();
            }
        }

        return shapes;
    }
}
