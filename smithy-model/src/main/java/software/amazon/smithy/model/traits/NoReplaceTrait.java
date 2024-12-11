/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Indicates that the put lifecycle operation of a resource
 * can only be used to create a resource and cannot replace
 * an existing resource.
 */
public final class NoReplaceTrait extends AnnotationTrait {
    public static final ShapeId ID = ShapeId.from("smithy.api#noReplace");

    public NoReplaceTrait(ObjectNode node) {
        super(ID, node);
    }

    public NoReplaceTrait() {
        this(Node.objectNode());
    }

    public static final class Provider extends AnnotationTrait.Provider<NoReplaceTrait> {
        public Provider() {
            super(ID, NoReplaceTrait::new);
        }
    }
}
