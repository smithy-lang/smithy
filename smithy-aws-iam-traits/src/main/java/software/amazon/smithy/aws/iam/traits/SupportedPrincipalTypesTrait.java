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

public final class SupportedPrincipalTypesTrait extends StringListTrait
        implements ToSmithyBuilder<SupportedPrincipalTypesTrait> {
    public static final ShapeId ID = ShapeId.from("aws.iam#supportedPrincipalTypes");

    public SupportedPrincipalTypesTrait(List<String> principals, FromSourceLocation sourceLocation) {
        super(ID, principals, sourceLocation);
    }

    public SupportedPrincipalTypesTrait(List<String> principals) {
        this(principals, SourceLocation.NONE);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Provider extends StringListTrait.Provider<SupportedPrincipalTypesTrait> {
        public Provider() {
            super(ID, SupportedPrincipalTypesTrait::new);
        }
    }

    @Override
    public Builder toBuilder() {
        return builder().sourceLocation(getSourceLocation()).values(getValues());
    }

    public static final class Builder extends StringListTrait.Builder<SupportedPrincipalTypesTrait, Builder> {
        private Builder() {}

        @Override
        public SupportedPrincipalTypesTrait build() {
            return new SupportedPrincipalTypesTrait(getValues(), getSourceLocation());
        }
    }
}
