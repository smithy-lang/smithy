/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.model.traits.synthetic;

import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AbstractTrait;

/**
 * Used to provide the original shape ID of a shape that is renamed
 * in the semantic model.
 *
 * <p>This is a synthetic trait that is not defined in the semantic model,
 * nor is it persisted when the model is serialized.
 */
public final class OriginalShapeIdTrait extends AbstractTrait {

    public static final ShapeId ID = ShapeId.from("smithy.synthetic#originalShapeId");

    private final ShapeId originalId;

    public OriginalShapeIdTrait(ShapeId original) {
        super(ID, SourceLocation.NONE);
        this.originalId = original;
    }

    /**
     * Gets the original shape ID of the shape before it was renamed.
     *
     * @return Returns the shape original shape ID.
     */
    public ShapeId getOriginalId() {
        return originalId;
    }

    @Override
    public boolean isSynthetic() {
        return true;
    }

    @Override
    protected Node createNode() {
        return Node.from(getOriginalId().toString());
    }
}
