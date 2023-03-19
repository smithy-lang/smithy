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

import java.util.List;
import software.amazon.smithy.model.shapes.Shape;

/**
 * Filters out shapes that do not match any predicates.
 *
 * <p>The result of this selector is always a subset of the input
 * (i.e., it does not map over the input).
 */
final class TestSelector implements InternalSelector {
    private final List<InternalSelector> selectors;

    TestSelector(List<InternalSelector> selectors) {
        this.selectors = selectors;
    }

    @Override
    public Response push(Context context, Shape shape, Receiver next) {
        for (InternalSelector predicate : selectors) {
            if (context.receivedShapes(shape, predicate)) {
                // The instant something matches, stop testing selectors.
                return next.apply(context, shape);
            }
        }

        // Continue to receive shapes because other shapes could match.
        return Response.CONTINUE;
    }
}
