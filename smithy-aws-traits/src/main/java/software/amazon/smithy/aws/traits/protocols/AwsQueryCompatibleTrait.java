/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.traits.protocols;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AnnotationTrait;

public final class AwsQueryCompatibleTrait extends AnnotationTrait {

    public static final ShapeId ID = ShapeId.from("aws.protocols#awsQueryCompatible");

    public AwsQueryCompatibleTrait(ObjectNode node) {
        super(ID, node);
    }

    public AwsQueryCompatibleTrait() {
        this(Node.objectNode());
    }

    public static final class Provider extends AnnotationTrait.Provider<AwsQueryCompatibleTrait> {
        public Provider() {
            super(ID, AwsQueryCompatibleTrait::new);
        }
    }
}
