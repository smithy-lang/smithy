/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.apigateway.traits;

import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.StringTrait;

public final class RequestValidatorTrait extends StringTrait {
    public static final ShapeId ID = ShapeId.from("aws.apigateway#requestValidator");

    public RequestValidatorTrait(String value, FromSourceLocation sourceLocation) {
        super(ID, value, sourceLocation);
    }

    public RequestValidatorTrait(String value) {
        this(value, SourceLocation.NONE);
    }

    public static final class Provider extends StringTrait.Provider<RequestValidatorTrait> {
        public Provider() {
            super(ID, RequestValidatorTrait::new);
        }
    }
}
