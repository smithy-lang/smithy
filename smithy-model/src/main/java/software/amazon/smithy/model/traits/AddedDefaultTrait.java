/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;

public final class AddedDefaultTrait extends AnnotationTrait {
    public static final ShapeId ID = ShapeId.from("smithy.api#addedDefault");

    public AddedDefaultTrait(ObjectNode node) {
        super(ID, node);
    }

    public AddedDefaultTrait() {
        this(Node.objectNode());
    }

    public static final class Provider extends AnnotationTrait.Provider<AddedDefaultTrait> {
        public Provider() {
            super(ID, AddedDefaultTrait::new);
        }
    }
}
