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
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.ToSmithyBuilder;
import software.amazon.smithy.model.traits.StringListTrait;
import software.amazon.smithy.model.traits.TraitService;

/**
 * Indicates that the payload of an operation is not to be signed.
 *
 * <p>Providing a list of strings will limit the effect of this trait to
 * only specific authentication schemes by name.
 */
public final class UnsignedPayload extends StringListTrait implements ToSmithyBuilder<UnsignedPayload> {
    private static final String TRAIT = "aws.api#unsignedPayload";

    public UnsignedPayload(List<String> values, FromSourceLocation sourceLocation) {
        super(TRAIT, values, sourceLocation);
    }

    public UnsignedPayload(FromSourceLocation sourceLocation) {
        this(List.of(), sourceLocation);
    }

    public UnsignedPayload() {
        this(List.of(), SourceLocation.NONE);
    }

    public static TraitService provider() {
        return TraitService.createStringListProvider(TRAIT, UnsignedPayload::new);
    }

    @Override
    public Builder toBuilder() {
        return builder().values(getValues());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder extends StringListTrait.Builder<UnsignedPayload, Builder> {
        private Builder() {}

        @Override
        public UnsignedPayload build() {
            return new UnsignedPayload(getValues(), getSourceLocation());
        }
    }
}
