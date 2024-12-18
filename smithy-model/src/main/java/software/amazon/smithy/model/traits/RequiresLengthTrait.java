/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Indicates that the streaming blob must be finite and has a known size.
 */
public final class RequiresLengthTrait extends AnnotationTrait {
    public static final ShapeId ID = ShapeId.from("smithy.api#requiresLength");

    public RequiresLengthTrait(ObjectNode node) {
        super(ID, node);
    }

    public RequiresLengthTrait() {
        this(Node.objectNode());
    }

    public static final class Provider extends AnnotationTrait.Provider<RequiresLengthTrait> {
        public Provider() {
            super(ID, RequiresLengthTrait::new);
        }
    }
}
