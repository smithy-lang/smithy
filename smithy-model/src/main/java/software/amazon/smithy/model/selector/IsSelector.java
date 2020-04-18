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
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.neighbor.NeighborProvider;
import software.amazon.smithy.model.shapes.Shape;

/**
 * Maps input over each function and returns the concatenated result.
 */
final class IsSelector implements Selector {
    private final List<Selector> selectors;

    private IsSelector(List<Selector> predicates) {
        this.selectors = predicates;
    }

    static Selector of(List<Selector> predicates) {
        return predicates.size() == 1 ? predicates.get(0) : new IsSelector(predicates);
    }

    @Override
    public Set<Shape> select(Model model, NeighborProvider neighborProvider, Set<Shape> shapes) {
        return selectors.stream()
                .flatMap(selector -> selector.select(model, neighborProvider, shapes).stream())
                .collect(Collectors.toSet());
    }
}
