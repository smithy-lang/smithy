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
 * Indicates that non-authoritative code generators should treat a member
 * as optional even if it's required or default.
 *
 * <p>Because this trait is added by default to the members of a structure
 * marked with the input trait, this trait can be defined as either
 * synthetic or non-synthetic.
 */
public final class ClientOptionalTrait extends AnnotationTrait {
    public static final ShapeId ID = ShapeId.from("smithy.api#clientOptional");

    public ClientOptionalTrait() {
        this(Node.objectNode());
    }

    public ClientOptionalTrait(ObjectNode node) {
        super(ID, node);
    }

    public ClientOptionalTrait(SourceLocation location) {
        this(new ObjectNode(Collections.emptyMap(), location));
    }

    public static final class Provider extends AnnotationTrait.Provider<ClientOptionalTrait> {
        public Provider() {
            super(ID, ClientOptionalTrait::new);
        }
    }
}
