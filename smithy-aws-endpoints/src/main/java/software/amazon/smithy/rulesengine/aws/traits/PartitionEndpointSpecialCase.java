/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.rulesengine.aws.traits;

import java.util.Objects;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * A special case that does not follow the services standard patterns
 * or are located in a region other than the partition's default global region.
 */
public final class PartitionEndpointSpecialCase
    implements FromSourceLocation, ToNode, ToSmithyBuilder<PartitionEndpointSpecialCase> {

    private static final String ENDPOINT = "endpoint";
    private static final String REGION = "region";
    private static final String DUAL_STACK = "dualStack";
    private static final String FIPS = "fips";
    private final String endpoint;
    private final String region;
    private final Boolean dualStack;
    private final Boolean fips;
    private final SourceLocation sourceLocation;

    private PartitionEndpointSpecialCase(Builder builder) {
        this.endpoint = builder.endpoint;
        this.region = builder.region;
        this.dualStack = builder.dualStack;
        this.fips = builder.fips;
        this.sourceLocation = Objects.requireNonNull(builder.sourceLocation);
    }

    /**
     * Gets the endpoint.
     *
     * @return Returns the endpoint
     */
    public String getEndpoint() {
        return endpoint;
    }

    /**
     * Gets the dualStack.
     *
     * @return Returns the dualStack
     */
    public Boolean getDualStack() {
        return dualStack;
    }

    /**
     * Gets the fips.
     *
     * @return Returns the fips
     */
    public Boolean getFips() {
        return fips;
    }

    /**
     * Gets the region.
     *
     * @return Returns the region
     */
    public String getRegion() {
        return region;
    }

    @Override
    public Node toNode() {
        return Node.objectNodeBuilder()
            .withMember(ENDPOINT, endpoint)
            .withMember(REGION, region)
            .withMember(DUAL_STACK, dualStack.toString())
            .withMember(FIPS, fips.toString())
            .build();
    }

    @Override
    public SmithyBuilder<PartitionEndpointSpecialCase> toBuilder() {
        return new Builder()
            .endpoint(endpoint)
            .region(region)
            .dualStack(dualStack)
            .fips(fips)
            .sourceLocation(sourceLocation);
    }

    @Override
    public SourceLocation getSourceLocation() {
        return FromSourceLocation.super.getSourceLocation();
    }

    /**
     * Creates a {@link PartitionEndpointSpecialCase} instance from the given Node information.
     *
     * @param node the node to deserialize.
     * @return Returns a PartitionEndpointSpecialCase
     */
    public static PartitionEndpointSpecialCase fromNode(Node node) {
        ObjectNode objectNode = node.expectObjectNode();
        return builder()
            .sourceLocation(objectNode.getSourceLocation())
            .endpoint(objectNode.expectStringMember(ENDPOINT).getValue())
            .region(objectNode.expectStringMember(REGION).getValue())
            .dualStack(objectNode.getBooleanMemberOrDefault(DUAL_STACK, null))
            .fips(objectNode.getBooleanMemberOrDefault(FIPS, null))
            .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder implements SmithyBuilder<PartitionEndpointSpecialCase> {
        private String endpoint;
        private String region;
        private Boolean dualStack;
        private Boolean fips;
        private SourceLocation sourceLocation = SourceLocation.none();

        @Override
        public PartitionEndpointSpecialCase build() {
            return new PartitionEndpointSpecialCase(this);
        }

        /**
         * Sets the special case endpoint template.
         *
         * @param endpoint Special case endpoint template to set.
         * @return Returns the builder.
         */
        public Builder endpoint(String endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        /**
         * Sets the dualstack.
         *
         * @param dualStack dualstack to set.
         * @return Returns the builder.
         */
        public Builder dualStack(Boolean dualStack) {
            this.dualStack = dualStack;
            return this;
        }

        /**
         * Sets the fips.
         *
         * @param fips fips to set.
         * @return Returns the builder.
         */
        public Builder fips(Boolean fips) {
            this.fips = fips;
            return this;
        }

        /**
         * Sets the region.
         *
         * @param region region to set.
         * @return Returns the builder.
         */
        public Builder region(String region) {
            this.region = region;
            return this;
        }

        Builder sourceLocation(SourceLocation sourceLocation) {
            this.sourceLocation = sourceLocation;
            return this;
        }
    }
}
