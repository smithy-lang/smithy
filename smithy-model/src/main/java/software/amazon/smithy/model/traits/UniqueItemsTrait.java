/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import java.util.Collections;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Indicates that the members of a list must be unique.
 */
public final class UniqueItemsTrait extends AnnotationTrait {
    public static final ShapeId ID = ShapeId.from("smithy.api#uniqueItems");

    private UniqueItemsTrait(ObjectNode node) {
        super(ID, node);
    }

    public UniqueItemsTrait() {
        this(Node.objectNode());
    }

    public UniqueItemsTrait(SourceLocation sourceLocation) {
        this(new ObjectNode(Collections.emptyMap(), sourceLocation));
    }

    public static final class Provider extends AnnotationTrait.Provider<UniqueItemsTrait> {
        public Provider() {
            super(ID, UniqueItemsTrait::new);
        }
    }
}
