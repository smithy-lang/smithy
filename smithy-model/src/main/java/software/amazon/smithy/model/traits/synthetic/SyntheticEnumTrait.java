/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *   http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.model.traits.synthetic;

import software.amazon.smithy.model.shapes.EnumShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.EnumTrait;

/**
 * A synthetic copy of the {@link EnumTrait} for use in the {@link EnumShape}.
 *
 * This exists only to bridge compatibility between IDL 1.0 and 2.0. This
 * synthetic trait will be applied to enum shapes so that code generators
 * can treat enum shapes as string shapes with the enum trait. We set synthetic
 * to true so that it won't get serialized. We change the shape id so that
 * it doesn't trip up selector validation for the enum trait, which does
 * not allow targeting enum shapes.
 */
public final class SyntheticEnumTrait extends EnumTrait {

    public static final ShapeId ID = ShapeId.from("smithy.synthetic#enum");

    private SyntheticEnumTrait(Builder builder) {
        super(ID, builder);
    }

    @Override
    public boolean isSynthetic() {
        return true;
    }

    @Override
    public Builder toBuilder() {
        Builder builder = (Builder) builder().sourceLocation(getSourceLocation());
        getValues().forEach(builder::addEnum);
        return builder;
    }

    /**
     * @return Returns a synthetic enum trait builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends EnumTrait.Builder {
        @Override
        public SyntheticEnumTrait build() {
            return new SyntheticEnumTrait(this);
        }
    }
}
