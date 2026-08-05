/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.SmithyUnstableApi;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Declares which resources an operation deletes and where to find their
 * identifiers in the operation input.
 */
@SmithyUnstableApi
public final class DeletesResourcesTrait extends AbstractResourceLifecycleTrait
        implements ToSmithyBuilder<DeletesResourcesTrait> {

    public static final ShapeId ID = ShapeId.from("smithy.api#deletesResources");

    private DeletesResourcesTrait(Builder builder) {
        super(ID, builder);
    }

    public static final class Provider extends AbstractResourceLifecycleTrait.Provider<DeletesResourcesTrait> {
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

    public static final class Builder extends AbstractResourceLifecycleTrait.Builder<DeletesResourcesTrait, Builder> {
        private Builder() {}

        private Builder(DeletesResourcesTrait trait) {
            super(trait);
        }

        @Override
        public DeletesResourcesTrait build() {
            return new DeletesResourcesTrait(this);
        }
    }
}
