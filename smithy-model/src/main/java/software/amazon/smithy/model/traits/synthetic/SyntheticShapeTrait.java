/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits.synthetic;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AnnotationTrait;

/**
 * Marks a shape as assembler-generated (synthetic).
 *
 * <p>This trait is applied by the assembler to shapes created from inline
 * collection syntax (e.g., {@code [String]} or {@code {String: String}}).
 * It is not user-applicable and does not need a definition in the prelude.
 *
 * <p>Unlike other synthetic traits, this trait is persisted in the JSON AST
 * so that IDL serializers can reconstruct inline syntax from it.
 */
public final class SyntheticShapeTrait extends AnnotationTrait {

    public static final ShapeId ID = ShapeId.from("smithy.synthetic#generated");

    public SyntheticShapeTrait() {
        super(ID, Node.objectNode());
    }

    public SyntheticShapeTrait(ObjectNode node) {
        super(ID, node);
    }

    @Override
    public boolean isSynthetic() {
        return true;
    }

    @Override
    public SerializationMode serializationMode() {
        return SerializationMode.AST_ONLY;
    }

    public static final class Provider extends AnnotationTrait.Provider<SyntheticShapeTrait> {
        public Provider() {
            super(ID, SyntheticShapeTrait::new);
        }
    }
}
