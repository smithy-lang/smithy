/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.traits;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.utils.BuilderRef;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.SmithyUnstableApi;
import software.amazon.smithy.utils.StringUtils;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * An endpoint test-case expectation.
 */
@SmithyUnstableApi
public final class ExpectedEndpoint implements ToNode, FromSourceLocation, ToSmithyBuilder<ExpectedEndpoint> {
    private final SourceLocation sourceLocation;
    private final String url;
    private final Map<String, List<String>> headers;
    private final Map<String, Node> properties;

    public ExpectedEndpoint(Builder builder) {
        this.sourceLocation = builder.sourceLocation;
        this.url = SmithyBuilder.requiredState("url", builder.url);
        this.headers = builder.headers.copy();
        this.properties = builder.properties.copy();
    }

    public String getUrl() {
        return url;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public Map<String, Node> getProperties() {
        return properties;
    }

    @Override
    public Node toNode() {
        ObjectNode.Builder builder = Node.objectNodeBuilder();
        if (!headers.isEmpty()) {
            ObjectNode.Builder headersBuilder = ObjectNode.builder();
            for (Map.Entry<String, List<String>> kvp : headers.entrySet()) {
                StringNode headerName = Node.from(kvp.getKey());
                ArrayNode.Builder valuesBuilder = ArrayNode.builder();
                for (String value : kvp.getValue()) {
                    valuesBuilder.withValue(Node.from(value));
                }
                headersBuilder.withMember(headerName, valuesBuilder.build());
            }
            builder.withMember("headers", headersBuilder.build());
        }
        if (!properties.isEmpty()) {
            ObjectNode.Builder propertiesBuilder = ObjectNode.builder();
            for (Map.Entry<String, Node> kvp : properties.entrySet()) {
                propertiesBuilder.withMember(kvp.getKey(), kvp.getValue());
            }
            builder.withMember("properties", propertiesBuilder.build());
        }
        if (url != null) {
            builder.withMember("url", url);
        }
        return builder.build();
    }

    @Override
    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }

    @Override
    public Builder toBuilder() {
        return new Builder(this);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUrl(), getHeaders(), getProperties());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ExpectedEndpoint that = (ExpectedEndpoint) o;
        return getUrl().equals(that.getUrl()) && Objects.equals(getHeaders(), that.getHeaders())
                && Objects.equals(getProperties(), that.getProperties());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("url: ").append(url).append("\n");
        if (!headers.isEmpty()) {
            headers.forEach(
                    (key, value) -> {
                        sb.append(StringUtils.indent(String.format("%s:%s", key, value), 2));
                    });
        }
        if (!properties.isEmpty()) {
            sb.append("properties:\n");
            properties.forEach((k, v) -> sb
                    .append(
                            StringUtils.indent(
                                    String.format("%s: %s", k, Node.prettyPrintJson(v)),
                                    2)));
        }
        return sb.toString();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static ExpectedEndpoint fromNode(Node node) {
        ObjectNode obj = node.expectObjectNode();
        Builder builder = builder().sourceLocation(node);
        obj.expectStringMember("url", builder::url);
        obj.getObjectMember("headers", headersNode -> {
            for (Map.Entry<String, Node> entry : headersNode.getStringMap().entrySet()) {
                List<String> values = new ArrayList<>();
                entry.getValue()
                        .expectArrayNode()
                        .getElements()
                        .forEach(n -> values.add(n.expectStringNode().getValue()));
                builder.putHeader(entry.getKey(), values);
            }
        });
        obj.getObjectMember("properties", propsNode -> {
            for (Map.Entry<String, Node> entry : propsNode.getStringMap().entrySet()) {
                builder.putProperty(entry.getKey(), entry.getValue());
            }
        });
        return builder.build();
    }

    public static final class Builder implements SmithyBuilder<ExpectedEndpoint> {
        private final BuilderRef<Map<String, List<String>>> headers = BuilderRef.forOrderedMap();
        private final BuilderRef<Map<String, Node>> properties = BuilderRef.forOrderedMap();
        private SourceLocation sourceLocation = SourceLocation.none();
        private String url;

        private Builder() {}

        private Builder(ExpectedEndpoint endpoint) {
            this.sourceLocation = endpoint.sourceLocation;
            this.url = endpoint.url;
            this.headers.setBorrowed(endpoint.headers);
            this.properties.setBorrowed(endpoint.properties);
        }

        public Builder sourceLocation(FromSourceLocation fromSourceLocation) {
            this.sourceLocation = fromSourceLocation.getSourceLocation();
            return this;
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

        public Builder putHeader(String header, List<String> values) {
            this.headers.get().put(header, values);
            return this;
        }

        public Builder removeHeader(String header) {
            this.headers.get().remove(header);
            return this;
        }

        public Builder properties(Map<String, Node> properties) {
            this.properties.clear();
            this.properties.get().putAll(properties);
            return this;
        }

        public Builder putProperty(String property, Node value) {
            this.properties.get().put(property, value);
            return this;
        }

        public Builder removeProperty(String property) {
            this.properties.get().remove(property);
            return this;
        }

        @Override
        public ExpectedEndpoint build() {
            return new ExpectedEndpoint(this);
        }
    }
}
