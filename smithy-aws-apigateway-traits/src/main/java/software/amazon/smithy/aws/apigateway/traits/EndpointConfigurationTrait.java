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
import software.amazon.smithy.utils.BuilderRef;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Defines the endpoint configuration for an API Gateway REST API, including
 * the endpoint type, VPC endpoint IDs, whether the default execute-api
 * endpoint is disabled, and the IP address type.
 */
public final class EndpointConfigurationTrait extends AbstractTrait
        implements ToSmithyBuilder<EndpointConfigurationTrait> {
    public static final ShapeId ID = ShapeId.from("aws.apigateway#endpointConfiguration");

    private final List<String> types;
    private final List<String> vpcEndpointIds;
    private final Boolean disableExecuteApiEndpoint;
    private final String ipAddressType;

    private EndpointConfigurationTrait(Builder builder) {
        super(ID, builder.getSourceLocation());
        types = SmithyBuilder.requiredState("types", builder.types.copy());
        vpcEndpointIds = builder.vpcEndpointIds.hasValue() ? builder.vpcEndpointIds.copy() : null;
        disableExecuteApiEndpoint = builder.disableExecuteApiEndpoint;
        ipAddressType = builder.ipAddressType;
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

    /**
     * Gets the IP address type that can invoke the API.
     *
     * <p>Supported values are {@code ipv4} and {@code dualstack}.
     *
     * @return Returns the optional IP address type.
     */
    public Optional<String> getIpAddressType() {
        return Optional.ofNullable(ipAddressType);
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
        return new Builder(this);
    }

    public static final class Builder extends AbstractTraitBuilder<EndpointConfigurationTrait, Builder> {
        private final BuilderRef<List<String>> types = BuilderRef.forList();
        private final BuilderRef<List<String>> vpcEndpointIds = BuilderRef.forList();
        private Boolean disableExecuteApiEndpoint;
        private String ipAddressType;

        private Builder() {}

        private Builder(EndpointConfigurationTrait trait) {
            sourceLocation(trait.getSourceLocation());
            this.types.setBorrowed(trait.types);
            if (trait.vpcEndpointIds != null) {
                this.vpcEndpointIds.setBorrowed(trait.vpcEndpointIds);
            }
            this.disableExecuteApiEndpoint = trait.disableExecuteApiEndpoint;
            this.ipAddressType = trait.ipAddressType;
        }

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
            clearTypes();
            this.types.get().addAll(types);
            return this;
        }

        /**
         * Adds an endpoint type.
         *
         * @param type The endpoint type to add.
         * @return Returns the builder.
         */
        public Builder addType(String type) {
            this.types.get().add(type);
            return this;
        }

        /**
         * Clears all of the endpoint types in the builder.
         *
         * @return Returns the builder.
         */
        public Builder clearTypes() {
            this.types.clear();
            return this;
        }

        /**
         * Sets the VPC endpoint IDs.
         *
         * @param vpcEndpointIds The VPC endpoint IDs to set.
         * @return Returns the builder.
         */
        public Builder vpcEndpointIds(List<String> vpcEndpointIds) {
            clearVpcEndpointIds();
            if (vpcEndpointIds != null) {
                this.vpcEndpointIds.get().addAll(vpcEndpointIds);
            }
            return this;
        }

        /**
         * Adds a VPC endpoint ID.
         *
         * @param vpcEndpointId The VPC endpoint ID to add.
         * @return Returns the builder.
         */
        public Builder addVpcEndpointId(String vpcEndpointId) {
            this.vpcEndpointIds.get().add(vpcEndpointId);
            return this;
        }

        /**
         * Clears all of the VPC endpoint IDs in the builder.
         *
         * @return Returns the builder.
         */
        public Builder clearVpcEndpointIds() {
            this.vpcEndpointIds.clear();
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

        /**
         * Sets the IP address type that can invoke the API.
         *
         * @param ipAddressType The IP address type to set ({@code ipv4} or {@code dualstack}).
         * @return Returns the builder.
         */
        public Builder ipAddressType(String ipAddressType) {
            this.ipAddressType = ipAddressType;
            return this;
        }
    }
}
