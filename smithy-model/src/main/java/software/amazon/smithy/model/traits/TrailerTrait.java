/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Indicates that a member's value can only be resolved after the sibling
 * streaming payload has been fully consumed.
 */
public final class TrailerTrait extends AnnotationTrait {
    public static final ShapeId ID = ShapeId.from("smithy.api#trailer");

    public TrailerTrait(ObjectNode node) {
        super(ID, node);
    }

    public TrailerTrait() {
        this(Node.objectNode());
    }

    public static final class Provider extends AnnotationTrait.Provider<TrailerTrait> {
        public Provider() {
            super(ID, TrailerTrait::new);
        }
    }
}
