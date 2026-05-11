/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.traits.auth;

import java.util.List;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.StringListTrait;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Defines the list of OAuth scopes required to invoke an operation that uses
 * an Amazon Cognito User Pools authorizer.
 *
 * <p>Applied to an operation in a service that uses the
 * {@link CognitoUserPoolsTrait}. The scopes flow through to the
 * {@code security} requirement of the generated OpenAPI operation.
 */
public final class CognitoUserPoolsScopesTrait extends StringListTrait
        implements ToSmithyBuilder<CognitoUserPoolsScopesTrait> {

    public static final ShapeId ID = ShapeId.from("aws.auth#cognitoUserPoolsScopes");

    public CognitoUserPoolsScopesTrait(List<String> scopes, FromSourceLocation sourceLocation) {
        super(ID, scopes, sourceLocation);
    }

    public CognitoUserPoolsScopesTrait(List<String> scopes) {
        this(scopes, SourceLocation.NONE);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Provider extends StringListTrait.Provider<CognitoUserPoolsScopesTrait> {
        public Provider() {
            super(ID, CognitoUserPoolsScopesTrait::new);
        }
    }

    @Override
    public Builder toBuilder() {
        return builder().sourceLocation(getSourceLocation()).values(getValues());
    }

    public static final class Builder
            extends StringListTrait.Builder<CognitoUserPoolsScopesTrait, Builder> {
        private Builder() {}

        @Override
        public CognitoUserPoolsScopesTrait build() {
            return new CognitoUserPoolsScopesTrait(getValues(), getSourceLocation());
        }
    }
}
