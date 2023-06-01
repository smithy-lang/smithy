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
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.rulesengine.language.RulesComponentBuilder;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.SmithyUnstableApi;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Provides a facility for overriding a partition's regions.
 */
@SmithyUnstableApi
public final class RegionOverride implements ToSmithyBuilder<RegionOverride>, FromSourceLocation, ToNode {
    private final SourceLocation sourceLocation;

    private RegionOverride(Builder builder) {
        this.sourceLocation = builder.getSourceLocation();
    }

    private RegionOverride(SourceLocation sourceLocation) {
        this.sourceLocation = sourceLocation;
    }

    public static Builder builder() {
        return new Builder(SourceLocation.none());
    }

    public static RegionOverride fromNode(Node node) {
        return new RegionOverride(node.getSourceLocation());
    }

    @Override
    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }

    @Override
    public SmithyBuilder<RegionOverride> toBuilder() {
        return new Builder(getSourceLocation());
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof RegionOverride;
    }

    @Override
    public int hashCode() {
        return 7;
    }

    @Override
    public Node toNode() {
        return Node.objectNode();
    }

    public static class Builder extends RulesComponentBuilder<Builder, RegionOverride> {

        public Builder(FromSourceLocation sourceLocation) {
            super(sourceLocation);
        }

        @Override
        public RegionOverride build() {
            return new RegionOverride(this);
        }
    }
}
