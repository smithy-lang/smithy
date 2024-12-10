/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.apigateway.traits;

import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.StringTrait;

/**
 * Attaches an API Gateway authorizer to a service, resource, or operation.
 */
public final class AuthorizerTrait extends StringTrait {
    public static final ShapeId ID = ShapeId.from("aws.apigateway#authorizer");

    public AuthorizerTrait(String value, SourceLocation sourceLocation) {
        super(ID, value, sourceLocation);
    }

    public AuthorizerTrait(String value) {
        this(value, SourceLocation.NONE);
    }

    public static final class Provider extends StringTrait.Provider<AuthorizerTrait> {
        public Provider() {
            super(ID, AuthorizerTrait::new);
        }
    }
}
