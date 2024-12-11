/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.traits.protocols;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AnnotationTrait;

/**
 * An RPC-based protocol that sends query string requests and XML responses.
 *
 * <p>This protocol is deprecated. For new services, ise {@link RestJson1Trait}
 * or {@link AwsJson1_1Trait} instead.
 */
public final class AwsQueryTrait extends AnnotationTrait {

    public static final ShapeId ID = ShapeId.from("aws.protocols#awsQuery");

    public AwsQueryTrait(ObjectNode node) {
        super(ID, node);
    }

    public AwsQueryTrait() {
        this(Node.objectNode());
    }

    public static final class Provider extends AnnotationTrait.Provider<AwsQueryTrait> {
        public Provider() {
            super(ID, AwsQueryTrait::new);
        }
    }
}
