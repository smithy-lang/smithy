/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Indicates that a shape cannot be targeted outside of the namespace in
 * which it was defined.
 */
public final class PrivateTrait extends AnnotationTrait {
    public static final ShapeId ID = ShapeId.from("smithy.api#private");

    public PrivateTrait(ObjectNode node) {
        super(ID, node);
    }

    public PrivateTrait() {
        this(Node.objectNode());
    }

    public static final class Provider extends AnnotationTrait.Provider<PrivateTrait> {
        public Provider() {
            super(ID, PrivateTrait::new);
        }
    }
}
