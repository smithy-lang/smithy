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
import java.util.function.BiConsumer;
import software.amazon.smithy.model.shapes.Shape;

/**
 * Maps input over a list of functions, passing the result of each to
 * the next.
 *
 * <p>The list of selectors is short-circuited if any selector returns
 * an empty result (by virtue of a selector not forwarding a shape to
 * the next selector).
 */
final class AndSelector implements InternalSelector {
    private final List<InternalSelector> selectors;

    private AndSelector(List<InternalSelector> predicates) {
        this.selectors = predicates;
    }

    static InternalSelector of(List<InternalSelector> predicates) {
        return predicates.size() == 1 ? predicates.get(0) : new AndSelector(predicates);
    }

    @Override
    public void push(Context context, Shape shape, BiConsumer<Context, Shape> next) {
        // Unroll common cases for selectors.
        switch (selectors.size()) {
            case 2:
                selectors.get(0).push(context, shape, (c, s) -> {
                    selectors.get(1).push(c, s, next);
                });
                break;
            case 3:
                selectors.get(0).push(context, shape, (c1, s1) -> {
                    selectors.get(1).push(c1, s1, (c2, s2) -> {
                        selectors.get(2).push(c2, s2, next);
                    });
                });
                break;
            case 4:
                selectors.get(0).push(context, shape, (c1, s1) -> {
                    selectors.get(1).push(c1, s1, (c2, s2) -> {
                        selectors.get(2).push(c2, s2, (c3, s3) -> {
                            selectors.get(3).push(c3, s3, next);
                        });
                    });
                });
                break;
            default:
                // Compose the next selector from the inside out. Note that there
                // can never be a single selector provided to this class.
                BiConsumer<Context, Shape> composedNext = composeNext(selectors.size() - 1, next);
                for (int i = selectors.size() - 2; i > 0; i--) {
                    composedNext = composeNext(i, composedNext);
                }

                // Push to the first selector, which pushes to the next, to the next...
                selectors.get(0).push(context, shape, composedNext);
        }
    }

    private BiConsumer<Context, Shape> composeNext(int position, BiConsumer<Context, Shape> next) {
        return (c, s) -> selectors.get(position).push(c, s, next);
    }
}
