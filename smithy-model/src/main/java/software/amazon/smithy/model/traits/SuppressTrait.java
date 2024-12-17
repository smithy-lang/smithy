/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import java.util.List;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.ToSmithyBuilder;

public final class SuppressTrait extends StringListTrait implements ToSmithyBuilder<SuppressTrait> {
    public static final ShapeId ID = ShapeId.from("smithy.api#suppress");

    private SuppressTrait(List<String> values, FromSourceLocation sourceLocation) {
        super(ID, values, sourceLocation);
    }

    public static final class Provider extends StringListTrait.Provider<SuppressTrait> {
        public Provider() {
            super(ID, SuppressTrait::new);
        }
    }

    @Override
    public Builder toBuilder() {
        return builder().sourceLocation(getSourceLocation()).values(getValues());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder extends StringListTrait.Builder<SuppressTrait, Builder> {
        private Builder() {}

        @Override
        public SuppressTrait build() {
            return new SuppressTrait(getValues(), getSourceLocation());
        }
    }
}
