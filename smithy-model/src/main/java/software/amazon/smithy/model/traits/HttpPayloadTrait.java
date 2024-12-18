/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Binds a single structure member to the payload of an HTTP request.
 */
public final class HttpPayloadTrait extends AnnotationTrait {
    public static final ShapeId ID = ShapeId.from("smithy.api#httpPayload");

    public HttpPayloadTrait(ObjectNode node) {
        super(ID, node);
    }

    public HttpPayloadTrait() {
        this(Node.objectNode());
    }

    public static final class Provider extends AnnotationTrait.Provider<HttpPayloadTrait> {
        public Provider() {
            super(ID, HttpPayloadTrait::new);
        }
    }
}
