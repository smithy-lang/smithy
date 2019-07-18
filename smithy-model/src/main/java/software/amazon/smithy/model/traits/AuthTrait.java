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

package software.amazon.smithy.model.traits;

import java.util.List;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Represents the Smithy {@code auth} trait, used to specify the auth
 * schemes supported by default for operations bound to a service.
 */
public final class AuthTrait extends StringListTrait implements ToSmithyBuilder<AuthTrait> {
    public static final ShapeId ID = ShapeId.from("smithy.api#auth");

    private AuthTrait(List<String> values, FromSourceLocation sourceLocation) {
        super(ID, values, sourceLocation);
    }

    public static final class Provider extends StringListTrait.Provider<AuthTrait> {
        public Provider() {
            super(ID, AuthTrait::new);
        }
    }

    @Override
    public Builder toBuilder() {
        return builder().values(getValues());
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builds an {@link AuthTrait}.
     */
    public static final class Builder extends StringListTrait.Builder<AuthTrait, Builder> {
        private Builder() {}

        @Override
        public AuthTrait build() {
            return new AuthTrait(getValues(), getSourceLocation());
        }
    }
}
