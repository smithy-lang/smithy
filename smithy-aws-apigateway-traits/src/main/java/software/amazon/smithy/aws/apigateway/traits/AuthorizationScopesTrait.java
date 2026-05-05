/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.apigateway.traits;

import java.util.List;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.StringListTrait;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Defines the list of OAuth scopes required for an API Gateway operation
 * that uses a Cognito authorizer. Applied alongside the
 * {@link AuthorizerTrait} to specify which scopes the caller must have.
 */
public final class AuthorizationScopesTrait extends StringListTrait
        implements ToSmithyBuilder<AuthorizationScopesTrait> {
    public static final ShapeId ID = ShapeId.from("aws.apigateway#authorizationScopes");

    private AuthorizationScopesTrait(List<String> values, FromSourceLocation sourceLocation) {
        super(ID, values, sourceLocation);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Builder toBuilder() {
        return builder().sourceLocation(getSourceLocation()).values(getValues());
    }

    public static final class Provider extends StringListTrait.Provider<AuthorizationScopesTrait> {
        public Provider() {
            super(ID, AuthorizationScopesTrait::new);
        }
    }

    public static final class Builder extends StringListTrait.Builder<AuthorizationScopesTrait, Builder> {
        private Builder() {}

        @Override
        public AuthorizationScopesTrait build() {
            return new AuthorizationScopesTrait(getValues(), getSourceLocation());
        }
    }
}
