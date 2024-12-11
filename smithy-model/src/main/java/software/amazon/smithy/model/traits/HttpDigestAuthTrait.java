/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * An auth scheme trait uses HTTP digest auth.
 */
public final class HttpDigestAuthTrait extends AnnotationTrait {

    public static final ShapeId ID = ShapeId.from("smithy.api#httpDigestAuth");

    public HttpDigestAuthTrait() {
        this(Node.objectNode());
    }

    public HttpDigestAuthTrait(ObjectNode node) {
        super(ID, node);
    }

    public static final class Provider extends AnnotationTrait.Provider<HttpDigestAuthTrait> {
        public Provider() {
            super(ID, HttpDigestAuthTrait::new);
        }
    }
}
