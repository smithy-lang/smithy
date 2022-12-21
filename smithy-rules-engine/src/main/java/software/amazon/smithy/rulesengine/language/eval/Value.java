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

package software.amazon.smithy.rulesengine.language.eval;

import static software.amazon.smithy.rulesengine.language.util.StringUtils.indent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.BooleanNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodeVisitor;
import software.amazon.smithy.model.node.NullNode;
import software.amazon.smithy.model.node.NumberNode;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.util.SourceLocationTrackingBuilder;
import software.amazon.smithy.rulesengine.language.util.SourceLocationUtils;
import software.amazon.smithy.utils.BuilderRef;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * An abstract representing a typed value.
 */
@SmithyUnstableApi
public abstract class Value implements FromSourceLocation, ToNode {
    private SourceLocation sourceLocation;

    public Value(SourceLocation sourceLocation) {
        this.sourceLocation = sourceLocation;
    }

    public static Value fromNode(Node source) {
        Value value = source.accept(new NodeVisitor<Value>() {
            @Override
            public Value arrayNode(ArrayNode node) {
                return new Array(node.getElements().stream().map(Value::fromNode).collect(Collectors.toList()));
            }

            @Override
            public Value booleanNode(BooleanNode node) {
                return bool(node.getValue());
            }

            @Override
            public Value nullNode(NullNode node) {
                throw new RuntimeException("null cannot be used as literal");
            }

            @Override
            public Value numberNode(NumberNode node) {
                if (!node.isNaturalNumber()) {
                    throw new RuntimeException("only integers >=0 are supported");
                }
                return Value.integer(node.getValue().intValue());
            }

            @Override
            public Value objectNode(ObjectNode node) {
                HashMap<Identifier, Value> out = new LinkedHashMap<>();
                node.getMembers().forEach((name, member) -> out.put(Identifier.of(name), Value.fromNode(member)));
                return Value.record(out);
            }

            @Override
            public Value stringNode(StringNode node) {
                return Value.string(node.getValue());
            }
        });
        value.sourceLocation = source.getSourceLocation();
        return value;
    }

    public static Endpoint endpointFromNode(Node source) {
        Endpoint ep = Endpoint.fromNode(source);
        ((Value) ep).sourceLocation = source.getSourceLocation();
        return ep;
    }

    public static Value none() {
        return new None();
    }

    public static String string(java.lang.String value) {
        return new String(value);
    }

    public static Record record(Map<Identifier, Value> value) {
        return new Record(value);
    }

    public static Bool bool(boolean value) {
        return new Bool(value);
    }

    public static Array array(List<Value> value) {
        return new Array(value);
    }

    public static Integer integer(int value) {
        return new Integer(value);
    }

    public abstract Type type();

    public java.lang.String expectString() {
        throw new RuntimeException("Expected string but was: " + this);
    }

    public boolean expectBool() {
        throw new RuntimeException("Expected bool but was: " + this);
    }

    @Override
    public SourceLocation getSourceLocation() {
        return Optional.ofNullable(sourceLocation).orElse(SourceLocation.none());
    }

    public Record expectRecord() {
        throw new RuntimeException("Expected object but was: " + this);
    }

    public boolean isNone() {
        return false;
    }

    public Endpoint expectEndpoint() {
        throw new RuntimeException("Expected endpoint, found " + this);
    }

    public Array expectArray() {
        throw new RuntimeException("Expected array, found " + this);
    }

    public int expectInteger() {
        throw new RuntimeException("Expected int, found " + this);
    }

    public static final class Integer extends Value {
        private final int value;

        private Integer(int value) {
            super(SourceLocation.none());
            this.value = value;
        }

        @Override
        public Type type() {
            return Type.integer();
        }

        @Override
        public int expectInteger() {
            return value;
        }

        @Override
        public Node toNode() {
            return Node.from(value);
        }
    }

    public static final class String extends Value {
        private final java.lang.String value;

        private String(java.lang.String value) {
            super(SourceLocation.none());
            this.value = value;
        }

        @Override
        public Type type() {
            return Type.string();
        }

        @Override
        public java.lang.String expectString() {
            return value();
        }

        public java.lang.String value() {
            return value;
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            String string = (String) o;
            return value.equals(string.value);
        }

        @Override
        public java.lang.String toString() {
            return value;
        }

        @Override
        public Node toNode() {
            return StringNode.from(value);
        }
    }

    public static final class Bool extends Value {

        private final boolean value;

        private Bool(boolean value) {
            super(SourceLocation.none());
            this.value = value;
        }

        @Override
        public Type type() {
            return Type.bool();
        }

        @Override
        public boolean expectBool() {
            return value();
        }

        private boolean value() {
            return this.value;
        }

        @Override
        public Node toNode() {
            return BooleanNode.from(value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Bool bool = (Bool) o;

            return value == bool.value;
        }

        @Override
        public java.lang.String toString() {
            return java.lang.String.valueOf(value);
        }
    }

    public static final class Record extends Value {
        private final Map<Identifier, Value> value;

        private Record(Map<Identifier, Value> value) {
            super(SourceLocation.none());
            this.value = value;
        }

        @Override
        public Type type() {
            Map<Identifier, Type> type = value.entrySet()
                    .stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, value -> value.getValue().type()));
            return new Type.Record(type);
        }

        @Override
        public Record expectRecord() {
            return this;
        }

        public Value get(java.lang.String key) {
            return get(Identifier.of(key));
        }

        public Value get(Identifier key) {
            return this.value.get(key);
        }

        public void forEach(BiConsumer<Identifier, Value> fn) {
            value.forEach(fn);
        }

        @Override
        public Node toNode() {
            ObjectNode.Builder builder = ObjectNode.builder();
            value.forEach((k, v) -> builder.withMember(k.getName(), v));
            return builder.build();
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Record record = (Record) o;
            return Objects.equals(value, record.value);
        }

        @Override
        public java.lang.String toString() {
            return value.toString();
        }

        public Map<Identifier, Value> getValue() {
            return this.value;
        }
    }

    public static final class Array extends Value {
        private final List<Value> inner;

        private Array(List<Value> values) {
            super(SourceLocation.none());
            this.inner = values;
        }

        public List<Value> getValues() {
            return inner;
        }

        @Override
        public Type type() {
            if (inner.isEmpty()) {
                return Type.array(Type.empty());
            } else {
                Type first = inner.get(0).type();
                if (inner.stream().allMatch(item -> item.type() == first)) {
                    return Type.array(first);
                } else {
                    throw new SourceException("An array cannot contain different types", this);
                }
            }
        }

        @Override
        public Array expectArray() {
            return this;
        }

        public Value get(int idx) {
            if (this.inner.size() > idx) {
                return this.inner.get(idx);
            } else {
                return new Value.None();
            }
        }

        @Override
        public Node toNode() {
            return inner.stream()
                    .map(ToNode::toNode)
                    .collect(ArrayNode.collect());
        }

        @Override
        public int hashCode() {
            return Objects.hash(inner);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Array array = (Array) o;
            return inner.equals(array.inner);
        }

        @Override
        public java.lang.String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            sb.append(inner.stream().map(Object::toString).collect(Collectors.joining(", ")));
            sb.append("]");
            return sb.toString();
        }
    }

    public static final class None extends Value {

        public None() {
            super(SourceLocation.none());
        }

        @Override
        public Type type() {
            return Type.empty();
        }

        @Override
        public boolean isNone() {
            return true;
        }

        @Override
        public Node toNode() {
            return Node.nullNode();
        }
    }

    public static final class Endpoint extends Value {
        private final java.lang.String url;
        private final Map<java.lang.String, Value> properties;
        private final Map<java.lang.String, List<java.lang.String>> headers;

        private Endpoint(Builder builder) {
            super(builder.getSourceLocation());
            this.url = SmithyBuilder.requiredState("url", builder.url);
            this.properties = builder.properties.copy();
            this.headers = builder.headers.copy();
        }

        public static Endpoint fromNode(Node node) {
            Builder builder = new Builder(node);
            ObjectNode on = node.expectObjectNode("endpoints are object nodes");
            on.expectNoAdditionalProperties(Arrays.asList("properties", "url", "headers"));
            builder.url(on.expectStringMember("url").getValue());
            on.getObjectMember("properties").ifPresent(props -> {
                props.getMembers().forEach((k, v) -> {
                    builder.addProperty(k.getValue(), Value.fromNode(v));
                });

            });

            on.getObjectMember("headers").ifPresent(headers -> headers.getMembers().forEach(((key, value) -> {
                java.lang.String name = key.getValue();
                value.expectArrayNode("Header values must be an array").getElements()
                        .forEach(e -> builder.addHeader(name, e.expectStringNode().getValue()));
            })));
            return builder.build();
        }

        public static Builder builder() {
            return new Builder(SourceLocationUtils.javaLocation());
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

            headers.forEach((k, v) -> {
                ArrayNode valuesNode = v.stream().map(StringNode::from).collect(ArrayNode.collect());
                builder.withMember(k, valuesNode);
            });

            return builder.build();
        }

        public Map<java.lang.String, Value> getProperties() {
            return properties;
        }

        public java.lang.String getUrl() {
            return url;
        }

        public Map<java.lang.String, List<java.lang.String>> getHeaders() {
            return headers;
        }

        @Override
        public Type type() {
            return Type.endpoint();
        }

        @Override
        public Endpoint expectEndpoint() {
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
            Endpoint endpoint = (Endpoint) o;
            return url.equals(endpoint.url)
                   && properties.equals(endpoint.properties)
                   && headers.equals(endpoint.headers);
        }

        @Override
        public java.lang.String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("url: ").append(url).append("\n");
            sb.append("properties:\n");
            sb.append(indent(properties.toString(), 2));
            // todo(rcoh)
            if (!headers.isEmpty()) {
                headers.forEach((key, value) -> {
                    sb.append(indent(java.lang.String.format("%s:%s", key, value), 2));
                });
            }
            return sb.toString();
        }

        public static final class Builder extends SourceLocationTrackingBuilder<Builder, Endpoint> {
            private final BuilderRef<Map<java.lang.String, Value>> properties = BuilderRef.forOrderedMap();
            private final BuilderRef<Map<java.lang.String, List<java.lang.String>>> headers =
                    BuilderRef.forOrderedMap();
            private java.lang.String url;

            public Builder(FromSourceLocation sourceLocation) {
                super(sourceLocation);
            }

            public Builder url(java.lang.String url) {
                this.url = url;
                return this;
            }

            public Builder headers(Map<java.lang.String, List<java.lang.String>> headers) {
                this.headers.clear();
                this.headers.get().putAll(headers);
                return this;
            }

            public Builder addHeader(java.lang.String name, java.lang.String value) {
                List<java.lang.String> values = this.headers.get().computeIfAbsent(name, (k) -> new ArrayList<>());
                values.add(value);
                return this;
            }

            public Builder properties(Map<java.lang.String, Value> properties) {
                this.properties.clear();
                this.properties.get().putAll(properties);
                return this;
            }

            public Builder addProperty(java.lang.String value, Value fromNode) {
                this.properties.get().put(value, fromNode);
                return this;
            }

            @Override
            public Endpoint build() {
                return new Endpoint(this);
            }

        }

    }
}
