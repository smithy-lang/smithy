/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.aws.language.functions.partition;

import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.rulesengine.language.RulesComponentBuilder;
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

    /**
     * Builder to create a {@link RegionOverride} instance.
     *
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder(SourceLocation.none());
    }

    /**
     * Creates a {@link RegionOverride} instance from the given Node information.
     *
     * @param node the node to deserialize.
     * @return the created RegionOverride.
     */
    public static RegionOverride fromNode(Node node) {
        return new RegionOverride(node.getSourceLocation());
    }

    @Override
    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }

    @Override
    public Builder toBuilder() {
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
