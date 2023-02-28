/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.rulesengine.language.eval.value;

import static software.amazon.smithy.rulesengine.language.RulesComponentBuilder.javaLocation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.rulesengine.language.RulesComponentBuilder;
import software.amazon.smithy.rulesengine.language.eval.type.Type;
import software.amazon.smithy.utils.BuilderRef;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.StringUtils;

public final class EndpointValue extends Value {
    private final String url;
    private final Map<String, Value> properties;
    private final Map<String, List<String>> headers;

    private EndpointValue(Builder builder) {
        super(builder.getSourceLocation());
        this.url = SmithyBuilder.requiredState("url", builder.url);
        this.properties = builder.properties.copy();
        this.headers = builder.headers.copy();
    }

    public static EndpointValue fromNode(Node node) {
        Builder builder = new Builder(node);
        ObjectNode on = node.expectObjectNode("endpoints are object nodes");
        on.expectNoAdditionalProperties(Arrays.asList("properties", "url", "headers"));
        builder.url(on.expectStringMember("url").getValue());
        on.getObjectMember("properties").ifPresent(props -> {
            props.getMembers().forEach((k, v) -> {
                builder.putProperty(k.getValue(), Value.fromNode(v));
            });

        });

        on.getObjectMember("headers").ifPresent(headers -> headers.getMembers().forEach(((key, value) -> {
            String name = key.getValue();
            value.expectArrayNode("Header values must be an array").getElements()
                    .forEach(e -> builder.addHeader(name, e.expectStringNode().getValue()));
        })));
        return builder.build();
    }

    public static Builder builder() {
        return new Builder(javaLocation());
    }

    @Override
    public Node toNode() {
        return ObjectNode.builder()
                .withMember("url", url)
                .withMember("properties", propertiesNode())
                .withMember("headers", headersNode())
                .build();
    }

    private Node propertiesNode() {
        ObjectNode.Builder b = ObjectNode.builder();
        properties.forEach(b::withMember);
        return b.build();
    }

    private Node headersNode() {
        ObjectNode.Builder builder = ObjectNode.builder();

        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            builder.withMember(entry.getKey(), ArrayNode.fromStrings(entry.getValue()));
        }

        return builder.build();
    }

    public Map<String, Value> getProperties() {
        return properties;
    }

    public String getUrl() {
        return url;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    @Override
    public Type getType() {
        return Type.endpointType();
    }

    @Override
    public EndpointValue expectEndpointValue() {
        return this;
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, properties, headers);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EndpointValue endpoint = (EndpointValue) o;
        return url.equals(endpoint.url)
                       && properties.equals(endpoint.properties)
                       && headers.equals(endpoint.headers);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("url: ").append(url).append("\n");
        sb.append("properties:\n");
        sb.append(StringUtils.indent(properties.toString(), 2));
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            sb.append(StringUtils.indent(String.format("%s:%s", entry.getKey(), entry.getValue()), 2));
        }
        return sb.toString();
    }

    public static final class Builder extends RulesComponentBuilder<Builder, EndpointValue> {
        private final BuilderRef<Map<String, Value>> properties = BuilderRef.forOrderedMap();
        private final BuilderRef<Map<String, List<String>>> headers =
                BuilderRef.forOrderedMap();
        private String url;

        public Builder(FromSourceLocation sourceLocation) {
            super(sourceLocation);
        }

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder headers(Map<String, List<String>> headers) {
            this.headers.clear();
            this.headers.get().putAll(headers);
            return this;
        }

        public Builder addHeader(String name, String value) {
            List<String> values = this.headers.get().computeIfAbsent(name, (k) -> new ArrayList<>());
            values.add(value);
            return this;
        }

        public Builder properties(Map<String, Value> properties) {
            this.properties.clear();
            this.properties.get().putAll(properties);
            return this;
        }

        public Builder putProperty(String value, Value fromNode) {
            this.properties.get().put(value, fromNode);
            return this;
        }

        @Override
        public EndpointValue build() {
            return new EndpointValue(this);
        }
    }
}
