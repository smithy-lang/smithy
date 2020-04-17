/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
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
