/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Indicates that the data stored in the shape or member is sensitive and
 * should be handled with care.
 */
public final class SensitiveTrait extends AnnotationTrait {
    public static final ShapeId ID = ShapeId.from("smithy.api#sensitive");

    public SensitiveTrait(ObjectNode node) {
        super(ID, node);
    }

    public SensitiveTrait() {
        this(Node.objectNode());
    }

    public static final class Provider extends AnnotationTrait.Provider<SensitiveTrait> {
        public Provider() {
            super(ID, SensitiveTrait::new);
        }
    }
}
