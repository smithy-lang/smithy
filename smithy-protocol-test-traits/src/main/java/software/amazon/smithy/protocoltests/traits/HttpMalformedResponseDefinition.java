/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.SmithyUnstableApi;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Defines the response expected by an HttpMalformedRequest test case.
 */
@SmithyUnstableApi
public final class HttpMalformedResponseDefinition implements ToNode, ToSmithyBuilder<HttpMalformedResponseDefinition> {

    private static final String BODY = "body";
    private static final String CODE = "code";
    private static final String HEADERS = "headers";

    private final HttpMalformedResponseBodyDefinition body;
    private final int code;
    private final Map<String, String> headers;

    private HttpMalformedResponseDefinition(Builder builder) {
        body = builder.body;
        code = builder.code;
        headers = MapUtils.copyOf(builder.headers);
    }

    public Optional<HttpMalformedResponseBodyDefinition> getBody() {
        return Optional.ofNullable(body);
    }

    public int getCode() {
        return code;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public static HttpMalformedResponseDefinition fromNode(Node node) {
        HttpMalformedResponseDefinition.Builder builder = builder();
        ObjectNode o = node.expectObjectNode();
        o.getObjectMember(BODY).ifPresent(body -> builder.body(HttpMalformedResponseBodyDefinition.fromNode(body)));
        o.getNumberMember(CODE).ifPresent(numberNode -> builder.code(numberNode.getValue().intValue()));
        o.getObjectMember(HEADERS).ifPresent(headers -> {
            headers.getStringMap().forEach((k, v) -> {
                builder.putHeader(k, v.expectStringNode().getValue());
            });
        });
        return builder.build();
    }

    @Override
    public Node toNode() {
        return Node.objectNodeBuilder()
                .withOptionalMember(BODY, getBody().map(HttpMalformedResponseBodyDefinition::toNode))
                .withMember(CODE, getCode())
                .withOptionalMember(HEADERS,
                        headers.isEmpty() ? Optional.empty() : Optional.of(ObjectNode.fromStringMap(getHeaders())))
                .build();
    }

    @Override
    public Builder toBuilder() {
        Builder builder = builder()
                .headers(getHeaders())
                .code(getCode());
        getBody().ifPresent(builder::body);
        return builder;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder used to create a HttpMalformedResponseDefinition.
     */
    public static final class Builder implements SmithyBuilder<HttpMalformedResponseDefinition> {

        private HttpMalformedResponseBodyDefinition body;
        private int code;
        private final Map<String, String> headers = new HashMap<>();

        private Builder() {}

        public Builder body(HttpMalformedResponseBodyDefinition body) {
            this.body = body;
            return this;
        }

        public Builder code(int code) {
            this.code = code;
            return this;
        }

        public Builder headers(Map<String, String> headers) {
            this.headers.clear();
            this.headers.putAll(headers);
            return this;
        }

        public Builder putHeader(String key, String value) {
            this.headers.put(key, value);
            return this;
        }

        @Override
        public HttpMalformedResponseDefinition build() {
            return new HttpMalformedResponseDefinition(this);
        }
    }
}
