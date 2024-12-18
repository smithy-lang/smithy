/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.traits.customizations;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AnnotationTrait;

public final class S3UnwrappedXmlOutputTrait extends AnnotationTrait {
    public static final ShapeId ID = ShapeId.from("aws.customizations#s3UnwrappedXmlOutput");

    public S3UnwrappedXmlOutputTrait(ObjectNode node) {
        super(ID, node);
    }

    public S3UnwrappedXmlOutputTrait() {
        this(Node.objectNode());
    }

    public static final class Provider extends AnnotationTrait.Provider<S3UnwrappedXmlOutputTrait> {
        public Provider() {
            super(ID, S3UnwrappedXmlOutputTrait::new);
        }
    }
}
