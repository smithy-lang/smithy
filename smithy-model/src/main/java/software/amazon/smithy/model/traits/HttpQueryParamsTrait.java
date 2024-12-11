/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Binds a map structure member to the HTTP query string.
 */
public class HttpQueryParamsTrait extends AnnotationTrait {
    public static final ShapeId ID = ShapeId.from("smithy.api#httpQueryParams");

    public HttpQueryParamsTrait(ObjectNode node) {
        super(ID, node);
    }

    public HttpQueryParamsTrait() {
        this(Node.objectNode());
    }

    public static final class Provider extends AnnotationTrait.Provider<HttpQueryParamsTrait> {
        public Provider() {
            super(ID, HttpQueryParamsTrait::new);
        }
    }
}
