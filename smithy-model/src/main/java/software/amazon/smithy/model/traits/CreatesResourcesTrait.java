/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.SmithyUnstableApi;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Declares which resources an operation creates and where to find their
 * identifiers in the operation output.
 */
@SmithyUnstableApi
public final class CreatesResourcesTrait extends AbstractResourceLifecycleTrait
        implements ToSmithyBuilder<CreatesResourcesTrait> {

    public static final ShapeId ID = ShapeId.from("smithy.api#createsResources");

    private CreatesResourcesTrait(Builder builder) {
        super(ID, builder);
    }

    public static final class Provider extends AbstractResourceLifecycleTrait.Provider<CreatesResourcesTrait> {
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

    public static final class Builder extends AbstractResourceLifecycleTrait.Builder<CreatesResourcesTrait, Builder> {
        private Builder() {}

        private Builder(CreatesResourcesTrait trait) {
            super(trait);
        }

        @Override
        public CreatesResourcesTrait build() {
            return new CreatesResourcesTrait(this);
        }
    }
}
