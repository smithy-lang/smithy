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

import java.util.Collection;
import java.util.List;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;

/**
 * Maps input over a list of functions, passing the result of each to
 * the next.
 *
 * <p>The list of selectors is short-circuited if any selector returns
 * an empty result (by virtue of a selector not forwarding a shape to
 * the next selector).
 */
final class AndSelector {

    private AndSelector() {}

    static InternalSelector of(List<InternalSelector> selectors) {
        switch (selectors.size()) {
            case 0:
                return InternalSelector.IDENTITY;
            case 1:
                // If there's only a single selector, then no need to wrap.
                return selectors.get(0);
            case 2:
                return new IntermediateAndSelector(selectors.get(0), selectors.get(1));
            default:
                InternalSelector result = selectors.get(selectors.size() - 1);
                for (int i = selectors.size() - 2; i >= 0; i--) {
                    result = new IntermediateAndSelector(selectors.get(i), result);
                }
                return result;
        }
    }

    static final class IntermediateAndSelector implements InternalSelector {
        private final InternalSelector leftSelector;
        private final InternalSelector rightSelector;

        IntermediateAndSelector(InternalSelector leftSelector, InternalSelector rightSelector) {
            this.leftSelector = leftSelector;
            this.rightSelector = rightSelector;
        }

        @Override
        public Response push(Context ctx, Shape shape, Receiver next) {
            return leftSelector.push(ctx, shape, (c, s) -> rightSelector.push(c, s, next));
        }

        @Override
        public Collection<? extends Shape> getStartingShapes(Model model) {
            return leftSelector.getStartingShapes(model);
        }
    }
}
