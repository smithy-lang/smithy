/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Defines when a shape or member was added to the model.
 */
public final class SinceTrait extends StringTrait {
    public static final ShapeId ID = ShapeId.from("smithy.api#since");

    public SinceTrait(String value, SourceLocation sourceLocation) {
        super(ID, value, sourceLocation);
    }

    public SinceTrait(String value) {
        this(value, SourceLocation.NONE);
    }

    public static final class Provider extends StringTrait.Provider<SinceTrait> {
        public Provider() {
            super(ID, SinceTrait::new);
        }
    }
}
