/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.traits.protocols;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AbstractTrait;
import software.amazon.smithy.model.traits.AbstractTraitBuilder;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Provides the value in the "Code" distinguishing field and HTTP response
 * code for an operation error.
 */
public final class AwsQueryErrorTrait extends AbstractTrait implements ToSmithyBuilder<AwsQueryErrorTrait> {
    public static final ShapeId ID = ShapeId.from("aws.protocols#awsQueryError");

    private final String code;
    private final int httpResponseCode;

    public AwsQueryErrorTrait(AwsQueryErrorTrait.Builder builder) {
        super(ID, builder.getSourceLocation());
        this.code = SmithyBuilder.requiredState("code", builder.code);
        this.httpResponseCode = SmithyBuilder.requiredState("httpResponseCode", builder.httpResponseCode);
    }

    public static final class Provider extends AbstractTrait.Provider {
        public Provider() {
            super(ID);
        }

        @Override
        public Trait createTrait(ShapeId target, Node value) {
            AwsQueryErrorTrait.Builder builder = builder().sourceLocation(value);
            ObjectNode objectNode = value.expectObjectNode();
            builder.code(objectNode.expectStringMember("code").getValue());
            builder.httpResponseCode(objectNode.expectNumberMember("httpResponseCode").getValue().intValue());
            AwsQueryErrorTrait result = builder.build();
            result.setNodeCache(value);
            return result;
        }
    }

    public String getCode() {
        return code;
    }

    public int getHttpResponseCode() {
        return httpResponseCode;
    }

    @Override
    protected Node createNode() {
        return new ObjectNode(MapUtils.of(), getSourceLocation())
                .withMember("code", getCode())
                .withMember("httpResponseCode", getHttpResponseCode());
    }

    @Override
    public Builder toBuilder() {
        return builder().code(code).httpResponseCode(httpResponseCode).sourceLocation(getSourceLocation());
    }

    /**
     * @return Returns a builder to create an awsQueryError trait.
     */
    public static AwsQueryErrorTrait.Builder builder() {
        return new Builder();
    }

    /**
     * Builder used to create an AwsQueryErrorTrait.
     */
    public static final class Builder extends AbstractTraitBuilder<AwsQueryErrorTrait, Builder> {
        private String code;
        private int httpResponseCode;

        public Builder() {}

        public Builder code(String code) {
            this.code = code;
            return this;
        }

        public Builder httpResponseCode(int httpResponseCode) {
            this.httpResponseCode = httpResponseCode;
            return this;
        }

        @Override
        public AwsQueryErrorTrait build() {
            return new AwsQueryErrorTrait(this);
        }
    }
}
