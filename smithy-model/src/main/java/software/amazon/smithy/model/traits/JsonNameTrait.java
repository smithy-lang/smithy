/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Provides a custom name to use when serializing a structure member
 * name as a JSON object property.
 */
public final class JsonNameTrait extends StringTrait {
    public static final ShapeId ID = ShapeId.from("smithy.api#jsonName");

    public JsonNameTrait(String value, SourceLocation sourceLocation) {
        super(ID, value, sourceLocation);
    }

    public JsonNameTrait(String value) {
        this(value, SourceLocation.NONE);
    }

    public static final class Provider extends StringTrait.Provider<JsonNameTrait> {
        public Provider() {
            super(ID, JsonNameTrait::new);
        }
    }
}
