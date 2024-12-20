/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.traits.auth;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AnnotationTrait;

/**
 * Indicates that the payload of an operation is not to be signed.
 */
public final class UnsignedPayloadTrait extends AnnotationTrait {
    public static final ShapeId ID = ShapeId.from("aws.auth#unsignedPayload");

    public UnsignedPayloadTrait(ObjectNode node) {
        super(ID, node);
    }

    public UnsignedPayloadTrait() {
        this(Node.objectNode());
    }

    public static final class Provider extends AnnotationTrait.Provider<UnsignedPayloadTrait> {
        public Provider() {
            super(ID, UnsignedPayloadTrait::new);
        }
    }
}
