/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.aws.traits.protocols;

import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.BooleanTrait;

/**
 * Indicates that the operation requires Content-MD5 header in its
 * HTTP request.
 */
public final class HttpContentMd5Trait extends BooleanTrait {
    public static final ShapeId ID = ShapeId.from("aws.protocols#httpContentMd5");

    public HttpContentMd5Trait(SourceLocation sourceLocation) {
        super(ID, sourceLocation);
    }

    public HttpContentMd5Trait() {
        this(SourceLocation.NONE);
    }

    public static final class Provider extends BooleanTrait.Provider<HttpContentMd5Trait> {
        public Provider() {
            super(ID, HttpContentMd5Trait::new);
        }
    }
}
