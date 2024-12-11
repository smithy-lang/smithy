/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Indicates that an operation requires a checksum in its HTTP request.
 * By default, the checksum used for a service is a MD5 checksum passed
 * in the Content-MD5 header.
 */
public final class HttpChecksumRequiredTrait extends AnnotationTrait {

    public static final ShapeId ID = ShapeId.from("smithy.api#httpChecksumRequired");

    public HttpChecksumRequiredTrait(ObjectNode node) {
        super(ID, node);
    }

    public HttpChecksumRequiredTrait() {
        this(Node.objectNode());
    }

    public static final class Provider extends AnnotationTrait.Provider<HttpChecksumRequiredTrait> {
        public Provider() {
            super(ID, HttpChecksumRequiredTrait::new);
        }
    }
}
