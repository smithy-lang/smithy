/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Defines an operation input member that is used to prevent
 * replayed requests.
 */
public final class IdempotencyTokenTrait extends AnnotationTrait {
    public static final ShapeId ID = ShapeId.from("smithy.api#idempotencyToken");

    public IdempotencyTokenTrait(ObjectNode node) {
        super(ID, node);
    }

    public IdempotencyTokenTrait() {
        this(Node.objectNode());
    }

    public static final class Provider extends AnnotationTrait.Provider<IdempotencyTokenTrait> {
        public Provider() {
            super(ID, IdempotencyTokenTrait::new);
        }
    }
}
