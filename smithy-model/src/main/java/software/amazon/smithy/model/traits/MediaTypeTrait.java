/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Documents the media type of a blob shape.
 */
public final class MediaTypeTrait extends StringTrait {
    public static final ShapeId ID = ShapeId.from("smithy.api#mediaType");

    public MediaTypeTrait(String value, SourceLocation sourceLocation) {
        super(ID, value, sourceLocation);
    }

    public MediaTypeTrait(String value) {
        this(value, SourceLocation.NONE);
    }

    public static final class Provider extends StringTrait.Provider<MediaTypeTrait> {
        public Provider() {
            super(ID, MediaTypeTrait::new);
        }
    }
}
