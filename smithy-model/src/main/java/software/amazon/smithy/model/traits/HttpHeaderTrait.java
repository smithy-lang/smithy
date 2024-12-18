/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Binds a member to an HTTP header.
 */
public final class HttpHeaderTrait extends StringTrait {
    public static final ShapeId ID = ShapeId.from("smithy.api#httpHeader");

    public HttpHeaderTrait(String value, SourceLocation sourceLocation) {
        super(ID, value, sourceLocation);

        if (getValue().isEmpty()) {
            throw new SourceException("httpHeader field name binding must not be empty", getSourceLocation());
        }
    }

    public HttpHeaderTrait(String value) {
        this(value, SourceLocation.NONE);
    }

    public static final class Provider extends StringTrait.Provider<HttpHeaderTrait> {
        public Provider() {
            super(ID, HttpHeaderTrait::new);
        }
    }
}
