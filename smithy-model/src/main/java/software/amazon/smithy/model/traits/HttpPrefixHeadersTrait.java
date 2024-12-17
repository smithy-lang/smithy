/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Binds a map structure member to prefixed HTTP headers.
 */
public final class HttpPrefixHeadersTrait extends StringTrait {
    public static final ShapeId ID = ShapeId.from("smithy.api#httpPrefixHeaders");

    public HttpPrefixHeadersTrait(String value, SourceLocation sourceLocation) {
        super(ID, value, sourceLocation);
    }

    public HttpPrefixHeadersTrait(String value) {
        this(value, SourceLocation.NONE);
    }

    public static final class Provider extends StringTrait.Provider<HttpPrefixHeadersTrait> {
        public Provider() {
            super(ID, HttpPrefixHeadersTrait::new);
        }
    }
}
