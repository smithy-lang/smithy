/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Indicates that an operation / service supports unauthenticated access.
 */
public final class OptionalAuthTrait extends AnnotationTrait {
    public static final ShapeId ID = ShapeId.from("smithy.api#optionalAuth");

    public OptionalAuthTrait() {
        this(Node.objectNode());
    }

    public OptionalAuthTrait(ObjectNode node) {
        super(ID, node);
    }

    public static final class Provider extends AnnotationTrait.Provider<OptionalAuthTrait> {
        public Provider() {
            super(ID, OptionalAuthTrait::new);
        }
    }
}
