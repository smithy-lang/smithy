/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Indicates that a structure member is required.
 */
public final class RequiredTrait extends AnnotationTrait {
    public static final ShapeId ID = ShapeId.from("smithy.api#required");

    public RequiredTrait(ObjectNode node) {
        super(ID, node);
    }

    public RequiredTrait() {
        this(Node.objectNode());
    }

    public static final class Provider extends AnnotationTrait.Provider<RequiredTrait> {
        public Provider() {
            super(ID, RequiredTrait::new);
        }
    }
}
