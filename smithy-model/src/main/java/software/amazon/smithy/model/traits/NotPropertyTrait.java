/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Indicates that a top level input/output structure member is not associated
 * with a resource property.
 */
public final class NotPropertyTrait extends AnnotationTrait {
    public static final ShapeId ID = ShapeId.from("smithy.api#notProperty");

    public NotPropertyTrait(ObjectNode node) {
        super(ID, node);
    }

    public NotPropertyTrait() {
        this(Node.objectNode());
    }

    public static final class Provider extends AnnotationTrait.Provider<NotPropertyTrait> {
        public Provider() {
            super(ID, NotPropertyTrait::new);
        }
    }
}
