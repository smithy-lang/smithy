/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * An auth scheme trait uses HTTP bearer auth.
 */
public final class HttpBearerAuthTrait extends AnnotationTrait {

    public static final ShapeId ID = ShapeId.from("smithy.api#httpBearerAuth");

    public HttpBearerAuthTrait() {
        this(Node.objectNode());
    }

    public HttpBearerAuthTrait(ObjectNode node) {
        super(ID, node);
    }

    public static final class Provider extends AnnotationTrait.Provider<HttpBearerAuthTrait> {
        public Provider() {
            super(ID, HttpBearerAuthTrait::new);
        }
    }
}
