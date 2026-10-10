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
 * Indicates that the order of the items in a list carries no meaning.
 */
public final class UnorderedTrait extends AnnotationTrait {
    public static final ShapeId ID = ShapeId.from("smithy.api#unordered");

    private UnorderedTrait(ObjectNode node) {
        super(ID, node);
    }

    public UnorderedTrait() {
        this(Node.objectNode());
    }

    public UnorderedTrait(SourceLocation sourceLocation) {
        this(new ObjectNode(Collections.emptyMap(), sourceLocation));
    }

    public static final class Provider extends AnnotationTrait.Provider<UnorderedTrait> {
        public Provider() {
            super(ID, UnorderedTrait::new);
        }
    }
}
