/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Binds an operation input member to a named identifier of the resource to
 * which the operation is bound.
 */
public final class ResourceIdentifierTrait extends StringTrait {
    public static final ShapeId ID = ShapeId.from("smithy.api#resourceIdentifier");

    public ResourceIdentifierTrait(String value, SourceLocation sourceLocation) {
        super(ID, value, sourceLocation);
    }

    public ResourceIdentifierTrait(String value) {
        this(value, SourceLocation.NONE);
    }

    public static final class Provider extends StringTrait.Provider<ResourceIdentifierTrait> {
        public Provider() {
            super(ID, ResourceIdentifierTrait::new);
        }
    }
}
