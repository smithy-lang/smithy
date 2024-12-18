/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Binds a member to an HTTP query string.
 */
public final class HttpQueryTrait extends StringTrait {
    public static final ShapeId ID = ShapeId.from("smithy.api#httpQuery");

    public HttpQueryTrait(String value, SourceLocation sourceLocation) {
        super(ID, value, sourceLocation);
        if (getValue().isEmpty()) {
            throw new SourceException("httpQuery parameter name binding must not be empty", getSourceLocation());
        }
    }

    public HttpQueryTrait(String value) {
        this(value, SourceLocation.NONE);
    }

    public static final class Provider extends StringTrait.Provider<HttpQueryTrait> {
        public Provider() {
            super(ID, HttpQueryTrait::new);
        }
    }
}
