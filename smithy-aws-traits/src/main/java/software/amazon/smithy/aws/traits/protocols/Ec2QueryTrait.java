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
 * An RPC-based protocol that sends query string requests and XML responses,
 * customized for Amazon EC2.
 *
 * <p>This protocol is deprecated. For new services, ise {@link RestJson1Trait}
 * or {@link AwsJson1_1Trait} instead.
 */
public final class Ec2QueryTrait extends AnnotationTrait {

    public static final ShapeId ID = ShapeId.from("aws.protocols#ec2Query");

    public Ec2QueryTrait(ObjectNode node) {
        super(ID, node);
    }

    public Ec2QueryTrait() {
        this(Node.objectNode());
    }

    public static final class Provider extends AnnotationTrait.Provider<Ec2QueryTrait> {
        public Provider() {
            super(ID, Ec2QueryTrait::new);
        }
    }
}
