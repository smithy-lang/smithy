/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.apigateway.traits;

import java.util.List;
import java.util.Optional;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodeMapper;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AbstractTrait;
import software.amazon.smithy.model.traits.AbstractTraitBuilder;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Defines the endpoint configuration for an API Gateway REST API, including
 * the endpoint type, VPC endpoint IDs, and whether the default execute-api
 * endpoint is disabled.
 */
public final class EndpointConfigurationTrait extends AbstractTrait
        implements ToSmithyBuilder<EndpointConfigurationTrait> {
    public static final ShapeId ID = ShapeId.from("aws.apigateway#endpointConfiguration");

    private final List<String> types;
    private final List<String> vpcEndpointIds;
    private final Boolean disableExecuteApiEndpoint;

    private EndpointConfigurationTrait(Builder builder) {
        super(ID, builder.getSourceLocation());
        types = SmithyBuilder.requiredState("types", builder.types);
        vpcEndpointIds = builder.vpcEndpointIds;
        disableExecuteApiEndpoint = builder.disableExecuteApiEndpoint;
    }

    public static final class Provider extends AbstractTrait.Provider {
        public Provider() {
            super(ID);
        }

        @Override
        public Trait createTrait(ShapeId target, Node value) {
            EndpointConfigurationTrait result = new NodeMapper()
                    .deserialize(value, EndpointConfigurationTrait.class);
            result.setNodeCache(value);
            return result;
        }
    }

    /**
     * Creates a builder for the trait.
     *
     * @return Returns the created builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Gets the endpoint types for the API.
     *
     * @return Returns the list of endpoint types.
     */
    public List<String> getTypes() {
        return types;
    }

    /**
     * Gets the VPC endpoint IDs for PRIVATE endpoint type APIs.
     *
     * @return Returns the optional list of VPC endpoint IDs.
     */
    public Optional<List<String>> getVpcEndpointIds() {
        return Optional.ofNullable(vpcEndpointIds);
    }

    /**
     * Gets whether the default execute-api endpoint is disabled.
     *
     * @return Returns the optional disable flag.
     */
    public Optional<Boolean> getDisableExecuteApiEndpoint() {
        return Optional.ofNullable(disableExecuteApiEndpoint);
    }

    @Override
    protected ObjectNode createNode() {
        NodeMapper mapper = new NodeMapper();
        mapper.disableToNodeForClass(EndpointConfigurationTrait.class);
        mapper.setOmitEmptyValues(true);
        return mapper.serialize(this).expectObjectNode();
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .sourceLocation(getSourceLocation())
                .types(types)
                .vpcEndpointIds(vpcEndpointIds)
                .disableExecuteApiEndpoint(disableExecuteApiEndpoint);
    }

    public static final class Builder extends AbstractTraitBuilder<EndpointConfigurationTrait, Builder> {
        private List<String> types;
        private List<String> vpcEndpointIds;
        private Boolean disableExecuteApiEndpoint;

        @Override
        public EndpointConfigurationTrait build() {
            return new EndpointConfigurationTrait(this);
        }

        /**
         * Sets the endpoint types.
         *
         * @param types The endpoint types to set.
         * @return Returns the builder.
         */
        public Builder types(List<String> types) {
            this.types = types;
            return this;
        }

        /**
         * Sets the VPC endpoint IDs.
         *
         * @param vpcEndpointIds The VPC endpoint IDs to set.
         * @return Returns the builder.
         */
        public Builder vpcEndpointIds(List<String> vpcEndpointIds) {
            this.vpcEndpointIds = vpcEndpointIds;
            return this;
        }

        /**
         * Sets whether the default execute-api endpoint is disabled.
         *
         * @param disableExecuteApiEndpoint The disable flag to set.
         * @return Returns the builder.
         */
        public Builder disableExecuteApiEndpoint(Boolean disableExecuteApiEndpoint) {
            this.disableExecuteApiEndpoint = disableExecuteApiEndpoint;
            return this;
        }
    }
}
