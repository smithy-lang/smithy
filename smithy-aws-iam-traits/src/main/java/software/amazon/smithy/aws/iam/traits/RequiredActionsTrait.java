/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.iam.traits;

import java.util.List;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.StringListTrait;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Use the {@code @iamAction} trait's {@code requiredActions} property instead.
 *
 * @deprecated As of release 1.44.0, replaced by {@link IamActionTrait#resolveRequiredActions}.
 */
@Deprecated
public final class RequiredActionsTrait extends StringListTrait implements ToSmithyBuilder<RequiredActionsTrait> {
    public static final ShapeId ID = ShapeId.from("aws.iam#requiredActions");

    public RequiredActionsTrait(List<String> actions, FromSourceLocation sourceLocation) {
        super(ID, actions, sourceLocation);
    }

    public RequiredActionsTrait(List<String> actions) {
        this(actions, SourceLocation.NONE);
    }

    public static final class Provider extends StringListTrait.Provider<RequiredActionsTrait> {
        public Provider() {
            super(ID, RequiredActionsTrait::new);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Builder toBuilder() {
        return builder().sourceLocation(getSourceLocation()).values(getValues());
    }

    public static final class Builder extends StringListTrait.Builder<RequiredActionsTrait, Builder> {
        @Override
        public RequiredActionsTrait build() {
            return new RequiredActionsTrait(getValues(), getSourceLocation());
        }
    }
}
