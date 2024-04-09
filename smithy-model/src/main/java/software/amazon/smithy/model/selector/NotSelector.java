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

import software.amazon.smithy.model.shapes.Shape;

/**
 * Filters out shapes that yield shapes when applied to a selector.
 */
final class NotSelector implements InternalSelector {

    private final InternalSelector selector;

    NotSelector(InternalSelector selector) {
        this.selector = selector;
    }

    @Override
    public Response push(Context context, Shape shape, Receiver next) {
        if (!context.receivedShapes(shape, selector)) {
            return next.apply(context, shape);
        } else {
            return Response.CONTINUE;
        }
    }
}
