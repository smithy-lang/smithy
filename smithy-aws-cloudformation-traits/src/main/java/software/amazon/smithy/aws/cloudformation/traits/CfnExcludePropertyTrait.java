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
 * Indicates that structure member should not be included in generated
 * CloudFormation resource definitions.
 */
public final class CfnExcludePropertyTrait extends AnnotationTrait {
    public static final ShapeId ID = ShapeId.from("aws.cloudformation#cfnExcludeProperty");

    public CfnExcludePropertyTrait(ObjectNode node) {
        super(ID, node);
    }

    public CfnExcludePropertyTrait() {
        this(Node.objectNode());
    }

    public static final class Provider extends AnnotationTrait.Provider<CfnExcludePropertyTrait> {
        public Provider() {
            super(ID, CfnExcludePropertyTrait::new);
        }
    }
}
