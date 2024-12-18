/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import software.amazon.smithy.model.knowledge.NullableIndex;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Indicates that a shape is boxed, meaning a value may or may not be present.
 *
 * <p>This trait is only used in Smithy IDL 1.0 models and is not allowed in
 * 2.0 models.
 *
 * @deprecated Use {@link NullableIndex} instead.
 */
@Deprecated
public final class BoxTrait extends AnnotationTrait {
    public static final ShapeId ID = ShapeId.from("smithy.api#box");

    public BoxTrait(ObjectNode node) {
        super(ID, node);
    }

    public BoxTrait() {
        this(Node.objectNode());
    }

    // The box trait was removed in IDL 2.0. It can be present in the
    // loaded semantic model so that Smithy can load 1.0 and 2.0 models
    // at the same time, but this implementation of Smithy only serializes
    // 2.0 models.
    @Override
    public boolean isSynthetic() {
        return true;
    }

    public static final class Provider extends AnnotationTrait.Provider<BoxTrait> {
        public Provider() {
            super(ID, BoxTrait::new);
        }
    }
}
