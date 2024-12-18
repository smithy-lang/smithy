/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.traits.auth;

import java.util.List;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AbstractTrait;
import software.amazon.smithy.model.traits.AbstractTraitBuilder;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.utils.BuilderRef;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Configures an Amazon Cognito User Pools auth scheme.
 */
public final class CognitoUserPoolsTrait extends AbstractTrait implements ToSmithyBuilder<CognitoUserPoolsTrait> {

    public static final ShapeId ID = ShapeId.from("aws.auth#cognitoUserPools");
    private static final String PROVIDER_ARNS = "providerArns";

    private final List<String> providerArns;

    private CognitoUserPoolsTrait(Builder builder) {
        super(ID, builder.getSourceLocation());
        this.providerArns = builder.providerArns.copy();
    }

    public static final class Provider extends AbstractTrait.Provider {
        public Provider() {
            super(ID);
        }

        @Override
        public Trait createTrait(ShapeId target, Node value) {
            ObjectNode objectNode = value.expectObjectNode();
            CognitoUserPoolsTrait result = builder()
                    .sourceLocation(value)
                    .providerArns(objectNode.expectArrayMember(PROVIDER_ARNS).getElementsAs(StringNode::getValue))
                    .build();
            result.setNodeCache(objectNode);
            return result;
        }
    }

    /**
     * @return Creates a builder used to build a {@link CognitoUserPoolsTrait}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Get the list of provider ARNs.
     *
     * @return Returns the ARNs.
     */
    public List<String> getProviderArns() {
        return providerArns;
    }

    @Override
    public Builder toBuilder() {
        return builder().sourceLocation(getSourceLocation()).providerArns(providerArns);
    }

    @Override
    protected Node createNode() {
        return Node.objectNodeBuilder()
                .sourceLocation(getSourceLocation())
                .withMember(PROVIDER_ARNS, providerArns.stream().map(Node::from).collect(ArrayNode.collect()))
                .build();
    }

    /** Builder for {@link CognitoUserPoolsTrait}. */
    public static final class Builder extends AbstractTraitBuilder<CognitoUserPoolsTrait, Builder> {
        private final BuilderRef<List<String>> providerArns = BuilderRef.forList();

        private Builder() {}

        @Override
        public CognitoUserPoolsTrait build() {
            return new CognitoUserPoolsTrait(this);
        }

        /**
         * Sets the provider ARNs.
         *
         * @param providerArns ARNS to set.
         * @return Returns the builder.
         */
        public Builder providerArns(List<String> providerArns) {
            clearProviderArns();
            this.providerArns.get().addAll(providerArns);
            return this;
        }

        /**
         * Adds a provider ARN.
         *
         * @param arn ARN to add.
         * @return Returns the builder.
         */
        public Builder addProviderArn(String arn) {
            providerArns.get().add(arn);
            return this;
        }

        /**
         * Clears all provider ARNs from the builder.
         *
         * @return Returns the builder.
         */
        public Builder clearProviderArns() {
            providerArns.clear();
            return this;
        }
    }
}
