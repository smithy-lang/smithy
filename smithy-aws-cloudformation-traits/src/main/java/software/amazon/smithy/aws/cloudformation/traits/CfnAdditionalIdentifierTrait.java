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
 * Indicates that the CloudFormation property generated from this member is an
 * additional identifier for the resource.
 */
public final class CfnAdditionalIdentifierTrait extends AnnotationTrait {
    public static final ShapeId ID = ShapeId.from("aws.cloudformation#cfnAdditionalIdentifier");

    public CfnAdditionalIdentifierTrait(ObjectNode node) {
        super(ID, node);
    }

    public CfnAdditionalIdentifierTrait() {
        this(Node.objectNode());
    }

    public static final class Provider extends AnnotationTrait.Provider<CfnAdditionalIdentifierTrait> {
        public Provider() {
            super(ID, CfnAdditionalIdentifierTrait::new);
        }
    }
}
