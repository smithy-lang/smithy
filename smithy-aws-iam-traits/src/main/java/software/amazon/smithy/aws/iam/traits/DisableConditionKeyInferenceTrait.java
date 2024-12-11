/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.iam.traits;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AnnotationTrait;

/**
 * Disables the automatic inference of condition keys of a resource.
 */
public final class DisableConditionKeyInferenceTrait extends AnnotationTrait {
    public static final ShapeId ID = ShapeId.from("aws.iam#disableConditionKeyInference");

    public DisableConditionKeyInferenceTrait(ObjectNode node) {
        super(ID, node);
    }

    public DisableConditionKeyInferenceTrait() {
        this(Node.objectNode());
    }

    public static final class Provider extends AnnotationTrait.Provider<DisableConditionKeyInferenceTrait> {
        public Provider() {
            super(ID, DisableConditionKeyInferenceTrait::new);
        }
    }
}
