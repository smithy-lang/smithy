/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Shapes marked with the internal trait are meant only for internal use and
 * must not be exposed to customers.
 */
public final class InternalTrait extends AnnotationTrait {
    public static final ShapeId ID = ShapeId.from("smithy.api#internal");

    public InternalTrait(ObjectNode node) {
        super(ID, node);
    }

    public InternalTrait() {
        this(Node.objectNode());
    }

    public static final class Provider extends AnnotationTrait.Provider<InternalTrait> {
        public Provider() {
            super(ID, InternalTrait::new);
        }
    }
}
