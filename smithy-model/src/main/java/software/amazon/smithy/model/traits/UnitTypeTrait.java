/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;

public final class UnitTypeTrait extends AnnotationTrait {

    /** The unitType shape ID. */
    public static final ShapeId ID = ShapeId.from("smithy.api#unitType");

    /** The shape ID of the built-in Unit type that uses this trait. */
    public static final ShapeId UNIT = ShapeId.from("smithy.api#Unit");

    public UnitTypeTrait(ObjectNode node) {
        super(ID, node);
    }

    public UnitTypeTrait() {
        this(Node.objectNode());
    }

    public static final class Provider extends AnnotationTrait.Provider<UnitTypeTrait> {
        public Provider() {
            super(ID, UnitTypeTrait::new);
        }
    }
}
