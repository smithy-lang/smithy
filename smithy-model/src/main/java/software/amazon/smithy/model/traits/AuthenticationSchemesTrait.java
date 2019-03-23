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
import software.amazon.smithy.model.ToSmithyBuilder;

public final class AuthenticationSchemesTrait
        extends StringListTrait implements ToSmithyBuilder<AuthenticationSchemesTrait> {

    private static final String TRAIT = "smithy.api#authenticationSchemes";

    private AuthenticationSchemesTrait(List<String> values, FromSourceLocation sourceLocation) {
        super(TRAIT, values, sourceLocation);
    }

    public static TraitService provider() {
        return TraitService.createStringListProvider(TRAIT, AuthenticationSchemesTrait::new);
    }

    @Override
    public Builder toBuilder() {
        return builder().values(getValues());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder extends StringListTrait.Builder<AuthenticationSchemesTrait, Builder> {
        private Builder() {}

        @Override
        public AuthenticationSchemesTrait build() {
            return new AuthenticationSchemesTrait(getValues(), getSourceLocation());
        }
    }
}
