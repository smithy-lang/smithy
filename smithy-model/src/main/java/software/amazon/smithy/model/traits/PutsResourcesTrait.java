/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.SmithyUnstableApi;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Declares which resources an operation creates or replaces (put semantics)
 * and where to find their identifiers in the operation input.
 */
@SmithyUnstableApi
public final class PutsResourcesTrait extends AbstractResourceLifecycleTrait
        implements ToSmithyBuilder<PutsResourcesTrait> {

    public static final ShapeId ID = ShapeId.from("smithy.api#putsResources");

    private PutsResourcesTrait(Builder builder) {
        super(ID, builder);
    }

    public static final class Provider extends AbstractResourceLifecycleTrait.Provider<PutsResourcesTrait> {
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

    public static final class Builder extends AbstractResourceLifecycleTrait.Builder<PutsResourcesTrait, Builder> {
        private Builder() {}

        private Builder(PutsResourcesTrait trait) {
            super(trait);
        }

        @Override
        public PutsResourcesTrait build() {
            return new PutsResourcesTrait(this);
        }
    }
}
