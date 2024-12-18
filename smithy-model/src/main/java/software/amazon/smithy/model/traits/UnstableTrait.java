/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Marks a shape as unstable.
 */
public final class UnstableTrait extends AnnotationTrait {
    public static final ShapeId ID = ShapeId.from("smithy.api#unstable");

    public UnstableTrait(ObjectNode node) {
        super(ID, node);
    }

    public UnstableTrait() {
        this(Node.objectNode());
    }

    public static final class Provider extends AnnotationTrait.Provider<UnstableTrait> {
        public Provider() {
            super(ID, UnstableTrait::new);
        }
    }
}
