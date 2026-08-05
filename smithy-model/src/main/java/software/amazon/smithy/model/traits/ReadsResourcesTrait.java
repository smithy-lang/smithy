/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.SmithyUnstableApi;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Declares which resources an operation reads outside the standard lifecycle
 * binding, and where to find their identifiers in the operation input.
 */
@SmithyUnstableApi
public final class ReadsResourcesTrait extends AbstractResourceLifecycleTrait
        implements ToSmithyBuilder<ReadsResourcesTrait> {

    public static final ShapeId ID = ShapeId.from("smithy.api#readsResources");

    private ReadsResourcesTrait(Builder builder) {
        super(ID, builder);
    }

    public static final class Provider extends AbstractResourceLifecycleTrait.Provider<ReadsResourcesTrait> {
        public Provider() {
            super(ID, Builder::new);
        }
    }

    @Override
    public Builder toBuilder() {
        return new Builder(this);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder extends AbstractResourceLifecycleTrait.Builder<ReadsResourcesTrait, Builder> {
        private Builder() {}

        private Builder(ReadsResourcesTrait trait) {
            super(trait);
        }

        @Override
        public ReadsResourcesTrait build() {
            return new ReadsResourcesTrait(this);
        }
    }
}
