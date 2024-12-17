/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.cloudformation.traits;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AnnotationTrait;

/**
 * Indicates that the CloudFormation property generated from this member has a
 * default value for this resource.
 */
public final class CfnDefaultValueTrait extends AnnotationTrait {
    public static final ShapeId ID = ShapeId.from("aws.cloudformation#cfnDefaultValue");

    public CfnDefaultValueTrait(ObjectNode node) {
        super(ID, node);
    }

    public CfnDefaultValueTrait() {
        this(Node.objectNode());
    }

    public static final class Provider extends AnnotationTrait.Provider<CfnDefaultValueTrait> {
        public Provider() {
            super(ID, CfnDefaultValueTrait::new);
        }
    }
}
