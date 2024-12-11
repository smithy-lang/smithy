/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Binds a member to a URI label of an input of an operation using
 * the member name.
 */
public final class HttpLabelTrait extends AnnotationTrait {
    public static final ShapeId ID = ShapeId.from("smithy.api#httpLabel");

    public HttpLabelTrait(ObjectNode node) {
        super(ID, node);
    }

    public HttpLabelTrait() {
        this(Node.objectNode());
    }

    public static final class Provider extends AnnotationTrait.Provider<HttpLabelTrait> {
        public Provider() {
            super(ID, HttpLabelTrait::new);
        }
    }
}
