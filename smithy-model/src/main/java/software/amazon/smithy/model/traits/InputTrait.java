/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.MapUtils;

/**
 * Specializes a structure as the input of a single operation.
 */
public final class InputTrait extends AnnotationTrait {
    public static final ShapeId ID = ShapeId.from("smithy.api#input");

    public InputTrait(ObjectNode node) {
        super(ID, node);
    }

    public InputTrait() {
        this(Node.objectNode());
    }

    public InputTrait(SourceLocation sourceLocation) {
        this(new ObjectNode(MapUtils.of(), sourceLocation));
    }

    public static final class Provider extends AnnotationTrait.Provider<InputTrait> {
        public Provider() {
            super(ID, InputTrait::new);
        }
    }
}
