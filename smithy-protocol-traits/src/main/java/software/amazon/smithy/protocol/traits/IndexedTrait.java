/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.protocol.traits;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AbstractTrait;
import software.amazon.smithy.model.traits.AnnotationTrait;

/**
 * Requires structures and unions in a service closure to use member indexes.
 */
public final class IndexedTrait extends AnnotationTrait {
    public static final ShapeId ID = ShapeId.from("smithy.protocols#indexed");

    public IndexedTrait(ObjectNode node) {
        super(ID, node);
    }

    public IndexedTrait() {
        this(Node.objectNode());
    }

    /**
     * Implements the {@link AbstractTrait.Provider}.
     */
    public static final class Provider extends AnnotationTrait.Provider<IndexedTrait> {

        public Provider() {
            super(ID, IndexedTrait::new);
        }
    }
}
