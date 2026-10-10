/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.apigateway.traits;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AbstractTrait;
import software.amazon.smithy.model.traits.AbstractTraitBuilder;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Declares the API Gateway configuration version used by a service.
 */
public final class ApiGatewayConfigTrait extends AbstractTrait
        implements ToSmithyBuilder<ApiGatewayConfigTrait> {
    public static final ShapeId ID = ShapeId.from("aws.apigateway#apiGatewayConfig");
    private static final String VERSION = "version";

    private final String version;

    private ApiGatewayConfigTrait(Builder builder) {
        super(ID, builder.getSourceLocation());
        version = SmithyBuilder.requiredState(VERSION, builder.version);
    }

    public static final class Provider extends AbstractTrait.Provider {
        public Provider() {
            super(ID);
        }

        @Override
        public Trait createTrait(ShapeId target, Node value) {
            ObjectNode objectNode = value.expectObjectNode();
            ApiGatewayConfigTrait result = builder()
                    .sourceLocation(value)
                    .version(objectNode.expectStringMember(VERSION).getValue())
                    .build();
            result.setNodeCache(objectNode);
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
     * Gets the API Gateway configuration version.
     *
     * @return Returns the version.
     */
    public String getVersion() {
        return version;
    }

    @Override
    protected ObjectNode createNode() {
        return Node.objectNodeBuilder()
                .sourceLocation(getSourceLocation())
                .withMember(VERSION, version)
                .build();
    }

    @Override
    public Builder toBuilder() {
        return new Builder(this);
    }

    public static final class Builder extends AbstractTraitBuilder<ApiGatewayConfigTrait, Builder> {
        private String version;

        private Builder() {}

        private Builder(ApiGatewayConfigTrait trait) {
            sourceLocation(trait.getSourceLocation());
            version = trait.version;
        }

        @Override
        public ApiGatewayConfigTrait build() {
            return new ApiGatewayConfigTrait(this);
        }

        /**
         * Sets the API Gateway configuration version.
         *
         * @param version Version to set.
         * @return Returns the builder.
         */
        public Builder version(String version) {
            this.version = version;
            return this;
        }
    }
}
