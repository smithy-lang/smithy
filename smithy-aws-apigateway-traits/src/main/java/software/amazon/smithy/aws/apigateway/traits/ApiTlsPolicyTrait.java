/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.apigateway.traits;

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
 * Defines the TLS policy and endpoint access mode for an API Gateway REST API.
 *
 * <p>This trait maps to the {@code x-amazon-apigateway-security-policy} and
 * {@code x-amazon-apigateway-endpoint-access-mode} OpenAPI extensions.
 */
public final class ApiTlsPolicyTrait extends AbstractTrait implements ToSmithyBuilder<ApiTlsPolicyTrait> {
    public static final ShapeId ID = ShapeId.from("aws.apigateway#apiTlsPolicy");

    private final String securityPolicy;
    private final String endpointAccessMode;

    private ApiTlsPolicyTrait(Builder builder) {
        super(ID, builder.getSourceLocation());
        securityPolicy = SmithyBuilder.requiredState("securityPolicy", builder.securityPolicy);
        endpointAccessMode = builder.endpointAccessMode;
    }

    public static final class Provider extends AbstractTrait.Provider {
        public Provider() {
            super(ID);
        }

        @Override
        public Trait createTrait(ShapeId target, Node value) {
            ApiTlsPolicyTrait result = new NodeMapper().deserialize(value, ApiTlsPolicyTrait.class);
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
     * Gets the security policy for the API.
     *
     * @return Returns the security policy string.
     */
    public String getSecurityPolicy() {
        return securityPolicy;
    }

    /**
     * Gets the endpoint access mode for the API.
     *
     * @return Returns the optional endpoint access mode.
     */
    public Optional<String> getEndpointAccessMode() {
        return Optional.ofNullable(endpointAccessMode);
    }

    @Override
    protected ObjectNode createNode() {
        NodeMapper mapper = new NodeMapper();
        mapper.disableToNodeForClass(ApiTlsPolicyTrait.class);
        mapper.setOmitEmptyValues(true);
        return mapper.serialize(this).expectObjectNode();
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .sourceLocation(getSourceLocation())
                .securityPolicy(securityPolicy)
                .endpointAccessMode(endpointAccessMode);
    }

    public static final class Builder extends AbstractTraitBuilder<ApiTlsPolicyTrait, Builder> {
        private String securityPolicy;
        private String endpointAccessMode;

        @Override
        public ApiTlsPolicyTrait build() {
            return new ApiTlsPolicyTrait(this);
        }

        /**
         * Sets the security policy.
         *
         * @param securityPolicy The security policy to set.
         * @return Returns the builder.
         */
        public Builder securityPolicy(String securityPolicy) {
            this.securityPolicy = securityPolicy;
            return this;
        }

        /**
         * Sets the endpoint access mode.
         *
         * @param endpointAccessMode The endpoint access mode to set.
         * @return Returns the builder.
         */
        public Builder endpointAccessMode(String endpointAccessMode) {
            this.endpointAccessMode = endpointAccessMode;
            return this;
        }
    }
}
