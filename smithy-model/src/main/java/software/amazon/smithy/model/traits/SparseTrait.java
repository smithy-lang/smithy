/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Indicates that a list or map is sparse, meaning they may contain nulls.
 */
public final class SparseTrait extends AnnotationTrait {
    public static final ShapeId ID = ShapeId.from("smithy.api#sparse");

    public SparseTrait(ObjectNode node) {
        super(ID, node);
    }

    public SparseTrait() {
        this(Node.objectNode());
    }

    public static final class Provider extends AnnotationTrait.Provider<SparseTrait> {
        public Provider() {
            super(ID, SparseTrait::new);
        }
    }
}
