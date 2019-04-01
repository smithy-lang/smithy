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

package software.amazon.smithy.aws.traits;

import java.util.List;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.ToSmithyBuilder;
import software.amazon.smithy.model.traits.StringListTrait;
import software.amazon.smithy.model.traits.TraitService;

public final class CognitoUserPoolsProviderArnsTrait
        extends StringListTrait
        implements ToSmithyBuilder<CognitoUserPoolsProviderArnsTrait> {

    public static final String TRAIT = "aws.api#cognitoUserPoolsProviderArns";

    private CognitoUserPoolsProviderArnsTrait(List<String> values, FromSourceLocation sourceLocation) {
        super(TRAIT, values, sourceLocation);
    }

    public static TraitService provider() {
        return TraitService.createStringListProvider(TRAIT, CognitoUserPoolsProviderArnsTrait::new);
    }

    @Override
    public Builder toBuilder() {
        return builder().values(getValues());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder extends StringListTrait.Builder<CognitoUserPoolsProviderArnsTrait, Builder> {
        private Builder() {}

        @Override
        public CognitoUserPoolsProviderArnsTrait build() {
            return new CognitoUserPoolsProviderArnsTrait(getValues(), getSourceLocation());
        }
    }
}
