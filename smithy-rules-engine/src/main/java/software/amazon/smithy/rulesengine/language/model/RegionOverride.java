/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.rulesengine.language.model;

import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.rulesengine.language.SourceAwareBuilder;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

public final class RegionOverride implements ToSmithyBuilder<RegionOverride>, FromSourceLocation {
    private final SourceLocation sourceLocation;

    private RegionOverride(Builder builder) {
        this.sourceLocation = builder.getSourceLocation();
    }

    public static Builder builder() {
        return new Builder(SourceLocation.none());
    }

    public static RegionOverride fromNode(Node node) {
        Builder b = new Builder(node);
        return b.build();
    }

    @Override
    public SmithyBuilder<RegionOverride> toBuilder() {
        return new Builder(getSourceLocation());
    }

    @Override
    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }

    @Override
    public int hashCode() {
        return 7;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof RegionOverride;
    }

    public static class Builder extends SourceAwareBuilder<Builder, RegionOverride> {

        public Builder(FromSourceLocation sourceLocation) {
            super(sourceLocation);
        }

        @Override
        public RegionOverride build() {
            return new RegionOverride(this);
        }
    }
}
