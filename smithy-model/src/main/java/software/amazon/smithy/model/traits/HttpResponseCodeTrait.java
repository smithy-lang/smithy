/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Indicates that the structure member represents the HTTP response
 * status code. This MAY differ from the HTTP status code provided
 * in the response.
 */
public final class HttpResponseCodeTrait extends AnnotationTrait {
    public static final ShapeId ID = ShapeId.from("smithy.api#httpResponseCode");

    public HttpResponseCodeTrait(ObjectNode node) {
        super(ID, node);
    }

    public HttpResponseCodeTrait() {
        this(Node.objectNode());
    }

    public static final class Provider extends AnnotationTrait.Provider<HttpResponseCodeTrait> {
        public Provider() {
            super(ID, HttpResponseCodeTrait::new);
        }
    }
}
