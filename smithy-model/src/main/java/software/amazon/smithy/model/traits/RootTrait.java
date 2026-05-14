/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Indicates that the targeted trait is a rooting trait. Shapes targeted by a
 * rooting trait are considered root shapes. Root shapes and shapes transitively
 * connected to them are never considered to be unreferenced.
 */
public final class RootTrait extends AnnotationTrait {
    public static final ShapeId ID = ShapeId.from("smithy.api#root");

    public RootTrait(ObjectNode node) {
        super(ID, node);
    }

    public RootTrait() {
        this(Node.objectNode());
    }

    public static final class Provider extends AnnotationTrait.Provider<RootTrait> {
        public Provider() {
            super(ID, RootTrait::new);
        }
    }
}
