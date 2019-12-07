/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.protocoltests.traits;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private static final String QUERY_PARAMS = "queryParams";
    private static final String FORBID_QUERY_PARAMS = "forbidQueryParams";
    private static final String REQUIRE_QUERY_PARAMS = "requireQueryParams";

    private final String method;
    private final String uri;
    private final Map<String, String> queryParams;
    private final List<String> forbidQueryParams;
    private final List<String> requireQueryParams;

    private HttpRequestTestCase(Builder builder) {
        super(builder);
        method = SmithyBuilder.requiredState(METHOD, builder.method);
        uri = SmithyBuilder.requiredState(URI, builder.uri);
        queryParams = Collections.unmodifiableMap(new LinkedHashMap<>(builder.queryParams));
        forbidQueryParams = ListUtils.copyOf(builder.forbidQueryParams);
        requireQueryParams = ListUtils.copyOf(builder.requireQueryParams);
    }

    public String getMethod() {
        return method;
    }

    public String getUri() {
        return uri;
    }

    public Map<String, String> getQueryParams() {
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
        o.getObjectMember(QUERY_PARAMS).ifPresent(headers -> {
            headers.getStringMap().forEach((k, v) -> {
                builder.putQueryParam(k, v.expectStringNode().getValue());
            });
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
        if (!queryParams.isEmpty()) {
            node.withMember(QUERY_PARAMS, ObjectNode.fromStringMap(getQueryParams()));
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
        private final Map<String, String> queryParams = new LinkedHashMap<>();
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

        public Builder queryParams(Map<String, String> queryParams) {
            this.queryParams.clear();
            this.queryParams.putAll(queryParams);
            return this;
        }

        public Builder putQueryParam(String key, String value) {
            queryParams.put(key, value);
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
