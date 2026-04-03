/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.traits;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AnnotationTrait;

/**
 * Indicates that the streaming blob supports aws-chunked content encoding.
 *
 * <p>When present, SDKs MUST aws-chunk encode the underlying data stream.
 * aws-chunked encoding is a series of data blocks followed by a final block
 * that contains metadata about the content transferred (e.g., checksums).
 */
public final class AwsChunkedTrait extends AnnotationTrait {
    public static final ShapeId ID = ShapeId.from("aws.api#awsChunked");

    public AwsChunkedTrait(ObjectNode node) {
        super(ID, node);
    }

    public AwsChunkedTrait() {
        this(Node.objectNode());
    }

    public static final class Provider extends AnnotationTrait.Provider<AwsChunkedTrait> {
        public Provider() {
            super(ID, AwsChunkedTrait::new);
        }
    }
}
