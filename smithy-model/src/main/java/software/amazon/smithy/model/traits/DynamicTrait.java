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

package software.amazon.smithy.model.traits;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * A general-purpose trait used to represent traits that are defined in the
 * model and have no concrete implementation.
 */
public final class DynamicTrait extends AbstractTrait {

    private final Node value;

    public DynamicTrait(ShapeId id, Node value) {
        super(id, value);
        this.value = value;
    }

    @Override
    protected Node createNode() {
        return value;
    }
}
