/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.traits;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AnnotationTrait;

public final class DataPlaneTrait extends AnnotationTrait {
    public static final ShapeId ID = ShapeId.from("aws.api#dataPlane");

    public DataPlaneTrait(ObjectNode node) {
        super(ID, node);
    }

    public DataPlaneTrait() {
        this(Node.objectNode());
    }

    public static final class Provider extends AnnotationTrait.Provider<DataPlaneTrait> {
        public Provider() {
            super(ID, DataPlaneTrait::new);
        }
    }
}
