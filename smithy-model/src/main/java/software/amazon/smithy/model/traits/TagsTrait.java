/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import java.util.List;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.Tagged;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Applies tags to a shape.
 */
public final class TagsTrait extends StringListTrait implements ToSmithyBuilder<TagsTrait>, Tagged {
    public static final ShapeId ID = ShapeId.from("smithy.api#tags");

    private TagsTrait(List<String> values, FromSourceLocation sourceLocation) {
        super(ID, values, sourceLocation);
    }

    public static final class Provider extends StringListTrait.Provider<TagsTrait> {
        public Provider() {
            super(ID, TagsTrait::new);
        }
    }

    @Override
    public List<String> getTags() {
        return getValues();
    }

    @Override
    public Builder toBuilder() {
        return builder().sourceLocation(getSourceLocation()).values(getValues());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder extends StringListTrait.Builder<TagsTrait, Builder> {
        private Builder() {}

        @Override
        public TagsTrait build() {
            return new TagsTrait(getValues(), getSourceLocation());
        }
    }
}
