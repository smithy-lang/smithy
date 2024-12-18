/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Binds an input member to a label in the hostPrefix of an endpoint
 * trait on an operation.
 */
public final class HostLabelTrait extends AnnotationTrait {
    public static final ShapeId ID = ShapeId.from("smithy.api#hostLabel");

    public HostLabelTrait(ObjectNode node) {
        super(ID, node);
    }

    public HostLabelTrait() {
        this(Node.objectNode());
    }

    public static final class Provider extends AnnotationTrait.Provider<HostLabelTrait> {
        public Provider() {
            super(ID, HostLabelTrait::new);
        }
    }
}
