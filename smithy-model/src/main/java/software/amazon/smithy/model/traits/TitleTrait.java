/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Provides a human-readable proper noun title to services and resources.
 */
public final class TitleTrait extends StringTrait {
    public static final ShapeId ID = ShapeId.from("smithy.api#title");

    public TitleTrait(String value, SourceLocation sourceLocation) {
        super(ID, value, sourceLocation);
    }

    public TitleTrait(String value) {
        this(value, SourceLocation.NONE);
    }

    public static final class Provider extends StringTrait.Provider<TitleTrait> {
        public Provider() {
            super(ID, TitleTrait::new);
        }
    }
}
