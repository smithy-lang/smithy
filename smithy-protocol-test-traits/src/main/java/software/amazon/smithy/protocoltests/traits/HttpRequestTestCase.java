/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.protocoltests.traits;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Defines a test case for an HTTP request.
 */
public final class HttpRequestTestCase extends HttpMessageTestCase implements ToSmithyBuilder<HttpRequestTestCase> {

    private static final String METHOD = "method";
    private static final String URI = "uri";
    private static final String HOST = "host";
    private static final String RESOLVED_HOST = "resolvedHost";
    private static final String QUERY_PARAMS = "queryParams";
    private static final String FORBID_QUERY_PARAMS = "forbidQueryParams";
    private static final String REQUIRE_QUERY_PARAMS = "requireQueryParams";

    private final String method;
    private final String uri;
    private final String host;
    private final String resolvedHost;
    private final List<String> queryParams;
    private final List<String> forbidQueryParams;
    private final List<String> requireQueryParams;

    private HttpRequestTestCase(Builder builder) {
        super(builder);
        method = SmithyBuilder.requiredState(METHOD, builder.method);
        uri = SmithyBuilder.requiredState(URI, builder.uri);
        host = builder.host;
        resolvedHost = builder.resolvedHost;
        queryParams = ListUtils.copyOf(builder.queryParams);
        forbidQueryParams = ListUtils.copyOf(builder.forbidQueryParams);
        requireQueryParams = ListUtils.copyOf(builder.requireQueryParams);
    }

    public String getMethod() {
        return method;
    }

    public String getUri() {
        return uri;
    }

    public Optional<String> getHost() {
        return Optional.ofNullable(host);
    }

    public Optional<String> getResolvedHost() {
        return Optional.ofNullable(resolvedHost);
    }

    public List<String> getQueryParams() {
        return queryParams;
    }

    public List<String> getForbidQueryParams() {
        return forbidQueryParams;
    }

    public List<String> getRequireQueryParams() {
        return requireQueryParams;
    }

    public static HttpRequestTestCase fromNode(Node node) {
        HttpRequestTestCase.Builder builder = builder();
        updateBuilderFromNode(builder, node);
        ObjectNode o = node.expectObjectNode();
        builder.method(o.expectStringMember(METHOD).getValue());
        builder.uri(o.expectStringMember(URI).getValue());
        o.getStringMember(HOST).ifPresent(stringNode -> builder.host(stringNode.getValue()));
        o.getStringMember(RESOLVED_HOST).ifPresent(stringNode -> builder.resolvedHost(stringNode.getValue()));
        o.getArrayMember(QUERY_PARAMS).ifPresent(queryParams -> {
            builder.queryParams(queryParams.getElementsAs(StringNode::getValue));
        });
        o.getArrayMember(FORBID_QUERY_PARAMS).ifPresent(params -> {
            builder.forbidQueryParams(params.getElementsAs(StringNode::getValue));
        });
        o.getArrayMember(REQUIRE_QUERY_PARAMS).ifPresent(params -> {
            builder.requireQueryParams(params.getElementsAs(StringNode::getValue));
        });
        return builder.build();
    }

    @Override
    public Node toNode() {
        ObjectNode.Builder node = super.toNode().expectObjectNode().toBuilder();
        node.withMember(METHOD, getMethod());
        node.withMember(URI, getUri());
        node.withOptionalMember(HOST, getHost().map(StringNode::from));
        node.withOptionalMember(RESOLVED_HOST, getResolvedHost().map(StringNode::from));
        if (!queryParams.isEmpty()) {
            node.withMember(QUERY_PARAMS, ArrayNode.fromStrings(getQueryParams()));
        }
        if (!forbidQueryParams.isEmpty()) {
            node.withMember(FORBID_QUERY_PARAMS, ArrayNode.fromStrings(getForbidQueryParams()));
        }
        if (!requireQueryParams.isEmpty()) {
            node.withMember(REQUIRE_QUERY_PARAMS, ArrayNode.fromStrings(getRequireQueryParams()));
        }
        return node.build();
    }

    @Override
    public Builder toBuilder() {
        Builder builder = builder()
                .method(getMethod())
                .uri(getUri())
                .queryParams(getQueryParams())
                .forbidQueryParams(getForbidQueryParams())
                .requireQueryParams(getRequireQueryParams());
        getHost().ifPresent(builder::host);
        getResolvedHost().ifPresent(builder::resolvedHost);
        updateBuilder(builder);
        return builder;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder used to create a HttpRequestTestsTrait.
     */
    public static final class Builder extends HttpMessageTestCase.Builder<Builder, HttpRequestTestCase> {

        private String method;
        private String uri;
        private String host;
        private String resolvedHost;
        private final List<String> queryParams = new ArrayList<>();
        private final List<String> forbidQueryParams = new ArrayList<>();
        private final List<String> requireQueryParams = new ArrayList<>();

        private Builder() {}

        public Builder method(String method) {
            this.method = method;
            return this;
        }

        public Builder uri(String uri) {
            this.uri = uri;
            return this;
        }

        public Builder host(String host) {
            this.host = host;
            return this;
        }

        public Builder resolvedHost(String resolvedHost) {
            this.resolvedHost = resolvedHost;
            return this;
        }

        public Builder queryParams(List<String> queryParams) {
            this.queryParams.clear();
            this.queryParams.addAll(queryParams);
            return this;
        }

        public Builder forbidQueryParams(List<String> forbidQueryParams) {
            this.forbidQueryParams.clear();
            this.forbidQueryParams.addAll(forbidQueryParams);
            return this;
        }

        public Builder requireQueryParams(List<String> requireQueryParams) {
            this.requireQueryParams.clear();
            this.requireQueryParams.addAll(requireQueryParams);
            return this;
        }

        @Override
        public HttpRequestTestCase build() {
            return new HttpRequestTestCase(this);
        }
    }
}
