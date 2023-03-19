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
 * Maps input over each function and returns the concatenated result.
 */
final class IsSelector implements InternalSelector {
    private final List<InternalSelector> selectors;

    private IsSelector(List<InternalSelector> predicates) {
        this.selectors = predicates;
    }

    static InternalSelector of(List<InternalSelector> predicates) {
        return predicates.size() == 1 ? predicates.get(0) : new IsSelector(predicates);
    }

    @Override
    public Response push(Context context, Shape shape, Receiver next) {
        for (InternalSelector selector : selectors) {
            if (selector.push(context, shape, next) == Response.STOP) {
                return Response.STOP;
            }
        }

        return Response.CONTINUE;
    }
}
