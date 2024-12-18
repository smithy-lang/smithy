/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.language.evaluation.value;

import static software.amazon.smithy.rulesengine.language.RulesComponentBuilder.javaLocation;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.rulesengine.language.RulesComponentBuilder;
import software.amazon.smithy.rulesengine.language.evaluation.type.Type;
import software.amazon.smithy.utils.BuilderRef;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.StringUtils;

/**
 * An endpoint value, containing a URL as well as headers that MUST be sent.
 */
public final class EndpointValue extends Value {
    private static final String PROPERTIES = "properties";
    private static final String URL = "URL";
    private static final String HEADERS = "headers";
    private static final List<String> NODE_PROPERTIES = ListUtils.of(PROPERTIES, URL, HEADERS);

    private final String url;
    private final Map<String, Value> properties;
    private final Map<String, List<String>> headers;

    private EndpointValue(Builder builder) {
        super(builder.getSourceLocation());
        this.url = SmithyBuilder.requiredState("url", builder.url);
        this.properties = builder.properties.copy();
        this.headers = builder.headers.copy();
    }

    /**
     * Builder to create a {@link EndpointValue} instance.
     *
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder(javaLocation());
    }

    /**
     * Creates an {@link EndpointValue} instance from the given Node information.
     *
     * @param node the node to deserialize.
     * @return the created EndpointValue.
     */
    public static EndpointValue fromNode(Node node) {
        Builder builder = new Builder(node);
        ObjectNode objectNode = node.expectObjectNode("endpoints are object nodes");
        objectNode.expectNoAdditionalProperties(NODE_PROPERTIES);

        objectNode.expectStringMember("url", builder::url);
        objectNode.getObjectMember(PROPERTIES, props -> {
            for (Map.Entry<String, Node> entry : props.getStringMap().entrySet()) {
                builder.putProperty(entry.getKey(), Value.fromNode(entry.getValue()));
            }
        });

        objectNode.getObjectMember("headers", headers -> {
            for (Map.Entry<String, Node> entry : headers.getStringMap().entrySet()) {
                builder.putHeader(entry.getKey(),
                        entry.getValue()
                                .expectArrayNode("Header values must be an array")
                                .getElementsAs(n -> n.expectStringNode().getValue()));
            }
        });
        return builder.build();
    }

    /**
     * Gets the properties of this endpoint.
     *
     * @return the properties of this endpoint.
     */
    public Map<String, Value> getProperties() {
        return properties;
    }

    /**
     * Gets the URL of this endpoint.
     *
     * @return the URL of this endpoint.
     */
    public String getUrl() {
        return url;
    }

    /**
     * Gets the headers to set when sending HTTP requests to the URL.
     *
     * @return a map of header names to a list of values to set on those headers.
     */
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
    public Node toNode() {
        ObjectNode.Builder propertiesBuilder = ObjectNode.builder();
        properties.forEach(propertiesBuilder::withMember);

        ObjectNode.Builder headersBuilder = ObjectNode.builder();
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            headersBuilder.withMember(entry.getKey(), ArrayNode.fromStrings(entry.getValue()));
        }

        return ObjectNode.builder()
                .withMember(URL, url)
                .withMember(PROPERTIES, propertiesBuilder.build())
                .withMember(HEADERS, headersBuilder.build())
                .build();
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
    public int hashCode() {
        return Objects.hash(url, properties, headers);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("url: ")
                .append(url)
                .append("\nproperties:\n")
                .append(StringUtils.indent(properties.toString(), 2));
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            sb.append(StringUtils.indent(String.format("%s:%s", entry.getKey(), entry.getValue()), 2));
        }
        return sb.toString();
    }

    /**
     * A builder used to create an {@link EndpointValue} class.
     */
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

        public Builder putHeader(String name, List<String> values) {
            this.headers.get().put(name, values);
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
