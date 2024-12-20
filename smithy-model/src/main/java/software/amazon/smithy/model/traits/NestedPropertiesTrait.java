/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Indicates that the contents of a structure member contain the top-level
 * properties of the associated resource.
 */
public final class NestedPropertiesTrait extends AnnotationTrait {
    public static final ShapeId ID = ShapeId.from("smithy.api#nestedProperties");

    public NestedPropertiesTrait(ObjectNode node) {
        super(ID, node);
    }

    public static final class Provider extends AnnotationTrait.Provider<NestedPropertiesTrait> {
        public Provider() {
            super(ID, NestedPropertiesTrait::new);
        }
    }
}
