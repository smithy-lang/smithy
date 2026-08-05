/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.SmithyUnstableApi;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Declares which resources an operation updates outside the standard lifecycle
 * binding, and where to find their identifiers in the operation input.
 */
@SmithyUnstableApi
public final class UpdatesResourcesTrait extends AbstractResourceLifecycleTrait
        implements ToSmithyBuilder<UpdatesResourcesTrait> {

    public static final ShapeId ID = ShapeId.from("smithy.api#updatesResources");

    private UpdatesResourcesTrait(Builder builder) {
        super(ID, builder);
    }

    public static final class Provider extends AbstractResourceLifecycleTrait.Provider<UpdatesResourcesTrait> {
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

    public static final class Builder extends AbstractResourceLifecycleTrait.Builder<UpdatesResourcesTrait, Builder> {
        private Builder() {}

        private Builder(UpdatesResourcesTrait trait) {
            super(trait);
        }

        @Override
        public UpdatesResourcesTrait build() {
            return new UpdatesResourcesTrait(this);
        }
    }
}
