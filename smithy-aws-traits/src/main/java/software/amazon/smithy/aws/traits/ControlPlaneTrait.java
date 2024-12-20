/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.traits;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AnnotationTrait;

public final class ControlPlaneTrait extends AnnotationTrait {
    public static final ShapeId ID = ShapeId.from("aws.api#controlPlane");

    public ControlPlaneTrait(ObjectNode node) {
        super(ID, node);
    }

    public ControlPlaneTrait() {
        this(Node.objectNode());
    }

    public static final class Provider extends AnnotationTrait.Provider<ControlPlaneTrait> {
        public Provider() {
            super(ID, ControlPlaneTrait::new);
        }
    }
}
