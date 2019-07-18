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
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.StringListTrait;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Indicates that the payload of an operation is not to be signed.
 *
 * <p>Providing a list of strings will limit the effect of this trait to
 * only specific authentication schemes by name.
 */
public final class UnsignedPayloadTrait extends StringListTrait implements ToSmithyBuilder<UnsignedPayloadTrait> {
    public static final ShapeId ID = ShapeId.from("aws.api#unsignedPayload");

    public UnsignedPayloadTrait(List<String> values, FromSourceLocation sourceLocation) {
        super(ID, values, sourceLocation);
    }

    public UnsignedPayloadTrait(FromSourceLocation sourceLocation) {
        this(ListUtils.of(), sourceLocation);
    }

    public UnsignedPayloadTrait() {
        this(ListUtils.of(), SourceLocation.NONE);
    }

    public static final class Provider extends StringListTrait.Provider<UnsignedPayloadTrait> {
        public Provider() {
            super(ID, UnsignedPayloadTrait::new);
        }
    }

    @Override
    public Builder toBuilder() {
        return builder().values(getValues());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder extends StringListTrait.Builder<UnsignedPayloadTrait, Builder> {
        private Builder() {}

        @Override
        public UnsignedPayloadTrait build() {
            return new UnsignedPayloadTrait(getValues(), getSourceLocation());
        }
    }
}
