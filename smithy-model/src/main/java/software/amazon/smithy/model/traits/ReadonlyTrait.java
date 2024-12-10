/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Indicates that an operation is read-only.
 */
public final class ReadonlyTrait extends AnnotationTrait {
    public static final ShapeId ID = ShapeId.from("smithy.api#readonly");

    public ReadonlyTrait(ObjectNode node) {
        super(ID, node);
    }

    public ReadonlyTrait() {
        this(Node.objectNode());
    }

    public static final class Provider extends AnnotationTrait.Provider<ReadonlyTrait> {
        public Provider() {
            super(ID, ReadonlyTrait::new);
        }
    }
}
