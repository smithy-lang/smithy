/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.protocoltests.traits;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Defines a test case for an HTTP response.
 */
public final class HttpResponseTestCase extends HttpMessageTestCase implements ToSmithyBuilder<HttpResponseTestCase> {

    private static final String CODE = "code";
    private final int code;

    private HttpResponseTestCase(Builder builder) {
        super(builder);
        code = SmithyBuilder.requiredState(CODE, builder.code);
    }

    public int getCode() {
        return code;
    }

    static HttpResponseTestCase fromNode(Node node) {
        HttpResponseTestCase.Builder builder = builder();
        ObjectNode o = node.expectObjectNode();
        builder.code(o.expectNumberMember(CODE).getValue().intValue());
        updateBuilderFromNode(builder, node);
        return builder.build();
    }

    @Override
    public Node toNode() {
        return super.toNode().expectObjectNode().withMember(CODE, getCode());
    }

    @Override
    public Builder toBuilder() {
        Builder builder = builder().code(getCode());
        updateBuilder(builder);
        return builder;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder used to create a HttpResponseTestCase.
     */
    public static final class Builder extends HttpMessageTestCase.Builder<Builder, HttpResponseTestCase> {

        private Integer code;

        private Builder() {}

        public Builder code(Integer code) {
            this.code = code;
            return this;
        }

        @Override
        public HttpResponseTestCase build() {
            return new HttpResponseTestCase(this);
        }
    }
}
