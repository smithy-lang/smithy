/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.aws.traits;

import java.util.Objects;
import java.util.Optional;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * A special case where endpoints for a region that do not follow the standard patterns.
 */
public final class RegionSpecialCase implements FromSourceLocation, ToNode, ToSmithyBuilder<RegionSpecialCase> {
    private static final String ENDPOINT = "endpoint";
    private static final String DUAL_STACK = "dualStack";
    private static final String FIPS = "fips";
    private static final String SIGNING_REGION = "signingRegion";
    private final String endpoint;
    private final Boolean dualStack;
    private final Boolean fips;
    private final String signingRegion;
    private final SourceLocation sourceLocation;

    private RegionSpecialCase(Builder builder) {
        this.endpoint = builder.endpoint;
        this.dualStack = builder.dualStack;
        this.fips = builder.fips;
        this.signingRegion = builder.signingRegion;
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
     * Gets the signing region.
     *
     * @return Returns the signing region
     */
    public String getSigningRegion() {
        return signingRegion;
    }

    @Override
    public Node toNode() {
        return Node.objectNodeBuilder()
                .withMember(ENDPOINT, endpoint)
                .withOptionalMember(DUAL_STACK, Optional.ofNullable(dualStack).map(Node::from))
                .withOptionalMember(FIPS, Optional.ofNullable(fips).map(Node::from))
                .withOptionalMember(DUAL_STACK, Optional.ofNullable(dualStack).map(Node::from))
                .withOptionalMember(SIGNING_REGION, Optional.ofNullable(signingRegion).map(Node::from))
                .build();
    }

    @Override
    public Builder toBuilder() {
        return new Builder()
                .dualStack(dualStack)
                .endpoint(endpoint)
                .fips(fips)
                .signingRegion(signingRegion)
                .sourceLocation(sourceLocation);
    }

    @Override
    public SourceLocation getSourceLocation() {
        return FromSourceLocation.super.getSourceLocation();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RegionSpecialCase that = (RegionSpecialCase) o;
        return Objects.equals(endpoint, that.endpoint)
                && Objects.equals(dualStack, that.dualStack)
                && Objects.equals(fips, that.fips)
                && Objects.equals(signingRegion, that.signingRegion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(endpoint, dualStack, fips, signingRegion);
    }

    /**
     * Creates a {@link RegionSpecialCase} instance from the given Node information.
     *
     * @param node the node to deserialize.
     * @return Returns a RegionSpecialCase
     */
    public static RegionSpecialCase fromNode(Node node) {
        ObjectNode objectNode = node.expectObjectNode();
        return builder()
                .sourceLocation(objectNode.getSourceLocation())
                .endpoint(objectNode.expectStringMember(ENDPOINT).getValue())
                .dualStack(objectNode.getBooleanMemberOrDefault(DUAL_STACK, null))
                .fips(objectNode.getBooleanMemberOrDefault(FIPS, null))
                .signingRegion(objectNode.getStringMemberOrDefault(SIGNING_REGION, null))
                .build();
    }

    public static Builder builder() {
        return new RegionSpecialCase.Builder();
    }

    public static final class Builder implements SmithyBuilder<RegionSpecialCase> {
        private String endpoint;
        private Boolean dualStack;
        private Boolean fips;
        private String signingRegion;
        private SourceLocation sourceLocation = SourceLocation.none();

        @Override
        public RegionSpecialCase build() {
            return new RegionSpecialCase(this);
        }

        /**
         * Sets the special case endpoint template.
         *
         * @param endpoint Special case endpoint template to set.
         * @return Returns the builder.
         */
        public Builder endpoint(String endpoint) {
            this.endpoint = Objects.requireNonNull(endpoint);
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
         * Sets the signing region.
         *
         * @param signingRegion region to set.
         * @return Returns the builder.
         */
        public Builder signingRegion(String signingRegion) {
            this.signingRegion = signingRegion;
            return this;
        }

        public Builder sourceLocation(SourceLocation sourceLocation) {
            this.sourceLocation = sourceLocation;
            return this;
        }

    }
}
