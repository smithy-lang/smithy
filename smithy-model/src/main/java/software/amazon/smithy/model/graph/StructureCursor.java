/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.model.graph;

import software.amazon.smithy.model.shapes.ShapeId;

public final class StructureCursor implements ShapeCursor {

    private final ModelIndex index;
    private final ShapeId id;

    public StructureCursor(ModelIndex index, ShapeId id) {
        this.index = index;
        this.id = id;
    }

    @Override
    public ShapeId toShapeId() {
        return id;
    }

    @Override
    public ModelIndex index() {
        return index;
    }
}
