/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Indicates that an operation is idempotent.
 */
public final class IdempotentTrait extends AnnotationTrait {
    public static final ShapeId ID = ShapeId.from("smithy.api#idempotent");

    public IdempotentTrait(ObjectNode node) {
        super(ID, node);
    }

    public IdempotentTrait() {
        this(Node.objectNode());
    }

    public static final class Provider extends AnnotationTrait.Provider<IdempotentTrait> {
        public Provider() {
            super(ID, IdempotentTrait::new);
        }
    }
}
