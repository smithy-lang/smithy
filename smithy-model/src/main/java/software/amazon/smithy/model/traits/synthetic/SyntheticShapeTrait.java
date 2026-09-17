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
 * It is not user-applicable.
 *
 * <p>The trait is defined in the prelude ({@code smithy.api#synthetic}) and is
 * a normal persisted trait: it is serialized in the JSON AST so that AST
 * consumers can tell generated shapes apart from authored ones. It is never
 * serialized in the IDL; the IDL 2.1 inline collection syntax carries the same
 * information.
 */
public final class SyntheticShapeTrait extends AnnotationTrait {

    public static final ShapeId ID = ShapeId.from("smithy.api#synthetic");

    public SyntheticShapeTrait() {
        super(ID, Node.objectNode());
    }

    public SyntheticShapeTrait(ObjectNode node) {
        super(ID, node);
    }

    public static final class Provider extends AnnotationTrait.Provider<SyntheticShapeTrait> {
        public Provider() {
            super(ID, SyntheticShapeTrait::new);
        }
    }
}
