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
 * A special case where endpoints for a partition that do not follow the standard patterns.
 */
public final class PartitionSpecialCase implements FromSourceLocation, ToNode, ToSmithyBuilder<PartitionSpecialCase> {

    private static final String ENDPOINT = "endpoint";
    private static final String DUAL_STACK = "dualStack";
    private static final String FIPS = "fips";
    private final String endpoint;
    private final Boolean dualStack;
    private final Boolean fips;
    private final SourceLocation sourceLocation;

    private PartitionSpecialCase(Builder builder) {
        this.endpoint = builder.endpoint;
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

    @Override
    public Node toNode() {
        return Node.objectNodeBuilder()
                .withMember(ENDPOINT, endpoint)
                .withOptionalMember(DUAL_STACK, Optional.ofNullable(dualStack).map(Node::from))
                .withOptionalMember(FIPS, Optional.ofNullable(fips).map(Node::from))
                .build();
    }

    @Override
    public Builder toBuilder() {
        return new Builder()
                .dualStack(dualStack)
                .endpoint(endpoint)
                .fips(fips)
                .sourceLocation(sourceLocation);
    }

    @Override
    public SourceLocation getSourceLocation() {
        return FromSourceLocation.super.getSourceLocation();
    }

    /**
     * Creates a {@link PartitionSpecialCase} instance from the given Node information.
     *
     * @param node the node to deserialize.
     * @return Returns a PartitionSpecialCase
     */
    public static PartitionSpecialCase fromNode(Node node) {
        ObjectNode objectNode = node.expectObjectNode();
        return builder()
                .sourceLocation(objectNode.getSourceLocation())
                .endpoint(objectNode.expectStringMember(ENDPOINT).getValue())
                .dualStack(objectNode.getBooleanMemberOrDefault(DUAL_STACK, null))
                .fips(objectNode.getBooleanMemberOrDefault(FIPS, null))
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder implements SmithyBuilder<PartitionSpecialCase> {
        private String endpoint;
        private Boolean dualStack;
        private Boolean fips;
        private SourceLocation sourceLocation = SourceLocation.none();

        @Override
        public PartitionSpecialCase build() {
            return new PartitionSpecialCase(this);
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

        Builder sourceLocation(SourceLocation sourceLocation) {
            this.sourceLocation = sourceLocation;
            return this;
        }

    }
}
