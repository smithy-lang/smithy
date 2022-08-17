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

package software.amazon.smithy.rulesengine.reterminus;

import static software.amazon.smithy.rulesengine.reterminus.error.RuleError.ctx;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.rulesengine.reterminus.eval.Scope;
import software.amazon.smithy.rulesengine.reterminus.eval.Type;
import software.amazon.smithy.rulesengine.reterminus.eval.Typecheck;
import software.amazon.smithy.rulesengine.reterminus.lang.Identifier;
import software.amazon.smithy.rulesengine.reterminus.lang.expr.Expr;
import software.amazon.smithy.rulesengine.reterminus.lang.expr.Literal;
import software.amazon.smithy.rulesengine.reterminus.util.StringUtils;
import software.amazon.smithy.utils.BuilderRef;
import software.amazon.smithy.utils.Pair;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * An Endpoint as returned by EndpointRules.
 */
public final class Endpoint implements ToSmithyBuilder<Endpoint>, FromSourceLocation, Typecheck, ToNode {
    private static final String URL = "url";
    private static final String PROPERTIES = "properties";
    private static final String HEADERS = "headers";

    private final Expr url;
    private final Map<String, List<Expr>> headers;
    private final Map<Identifier, Literal> properties;
    private final SourceLocation sourceLocation;

    private Endpoint(Builder builder) {
        this.url = SmithyBuilder.requiredState("url", builder.url);
        Map<Identifier, Literal> properties = new LinkedHashMap<>(builder.properties.copy());
        List<Literal> authSchemes =
                builder.authSchemes.copy().stream()
                        .map(
                                authScheme -> {
                                    Map<Identifier, Literal> base = new LinkedHashMap<>();
                                    base.put(Identifier.of("name"), Literal.of(authScheme.left.asString()));
                                    base.putAll(authScheme.right);
                                    return Literal.record(base);
                                })
                        .collect(Collectors.toList());
        if (!authSchemes.isEmpty()) {
            properties.put(Identifier.of("authSchemes"), Literal.tuple(authSchemes));
        }

        this.properties = properties;
        this.headers = builder.headers.copy();
        this.sourceLocation = SmithyBuilder.requiredState("sourceLocation", builder.getSourceLocation());
    }

    public static Endpoint fromNode(Node node) {
        ObjectNode on = node.expectObjectNode();

        Builder builder = builder(node);

        builder.url(Expr.fromNode(on.expectMember(URL, "URL must be included in endpoint")));
        on.expectNoAdditionalProperties(Arrays.asList(PROPERTIES, HEADERS, URL));

        on.getObjectMember(PROPERTIES)
                .ifPresent(
                        props -> {
                            Map<Identifier, Literal> members = new LinkedHashMap<>();
                            props.getMembers()
                                    .forEach((k, v) -> members.put(Identifier.of(k), Literal.fromNode(v)));
                            builder.properties(members);
                        });

        on.getObjectMember(HEADERS).ifPresent(objectNode -> {
            objectNode.getMembers().forEach((headerName, headerValues) -> {
                builder.addHeader(headerName.getValue(),
                        headerValues.expectArrayNode("header values should be an array")
                                .getElements().stream().map(Expr::fromNode).collect(Collectors.toList()));
            });
        });

        return builder.build();
    }

    /**
     * Create a new Endpoint builder.
     *
     * @param location SourceLocation to attach to this endpoint
     * @return Endpoint builder
     */
    public static Builder builder(FromSourceLocation location) {
        return new Builder(location);
    }

    public static Builder builder() {
        return new Builder(SourceLocation.none());
    }

    @Override
    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }

    public Expr getUrl() {
        return url;
    }

    @Override
    public Builder toBuilder() {
        return builder(this.sourceLocation).url(url).properties(properties);
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
        return url.equals(endpoint.url) && properties.equals(endpoint.properties) && headers.equals(endpoint.headers);
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
            properties.forEach((k, v) -> sb.append(StringUtils.indent(String.format("%s: %s", k, v), 2)));
        }
        return sb.toString();
    }

    @Override
    public Type typecheck(Scope<Type> scope) {
        ctx("while checking the URL", url, () -> url.typecheck(scope).expectString());
        ctx("while checking properties", () -> {
            properties.forEach((k, lit) -> {
                lit.typecheck(scope);
            });
            return null;
        });
        ctx("while checking headers", () -> {
            for (List<Expr> headerList : headers.values()) {
                for (Expr header : headerList) {
                    header.typecheck(scope).expectString();
                }
            }
            return null;
        });
        return Type.endpoint();
    }

    @Override
    public Node toNode() {
        return ObjectNode.builder()
                .withMember(URL, url)
                .withMember(PROPERTIES, propertiesNode())
                .withMember(HEADERS, headersNode())
                .build();
    }

    private Node propertiesNode() {
        ObjectNode.Builder on = ObjectNode.builder();
        properties.forEach((k, v) ->
                on.withMember(k.toString(), v.toNode())
        );
        return on.build();
    }

    private Node headersNode() {
        return exprMapNode(headers);
    }

    private Node exprMapNode(Map<String, List<Expr>> m) {
        ObjectNode.Builder mapNode = ObjectNode.builder();
        m.forEach((k, v) -> mapNode.withMember(k, ArrayNode.fromNodes(v.stream()
                .map(Expr::toNode)
                .collect(Collectors.toList()))));
        return mapNode.build();
    }

    public Map<Identifier, Literal> getProperties() {
        return properties;
    }

    public Map<String, List<Expr>> getHeaders() {
        return headers;
    }

    /**
     * Builder for {@link Endpoint}.
     */
    public static class Builder extends SourceAwareBuilder<Builder, Endpoint> {
        private final BuilderRef<Map<String, List<Expr>>> headers = BuilderRef.forOrderedMap();
        private final BuilderRef<Map<Identifier, Literal>> properties = BuilderRef.forOrderedMap();
        private final BuilderRef<List<Pair<Identifier, Map<Identifier, Literal>>>> authSchemes = BuilderRef.forList();
        private Expr url;

        public Builder(FromSourceLocation sourceLocation) {
            super(sourceLocation);
        }

        public Builder url(Expr url) {
            this.url = url;
            return this;
        }

        public Builder properties(Map<Identifier, Literal> properties) {
            this.properties.clear();
            this.properties.get().putAll(properties);
            return this;
        }

        public Builder authSchemes(List<Identifier> schemes, Map<Identifier, Map<Identifier, Literal>> params) {
            this.authSchemes.clear();
            schemes.forEach(scheme -> addAuthScheme(scheme, params.get(scheme)));
            return this;
        }

        public Builder addAuthScheme(Identifier scheme, Map<Identifier, Literal> parameters) {
            this.authSchemes.get().add(Pair.of(scheme, parameters));
            return this;
        }

        public Builder addAuthScheme(String scheme, Map<String, Literal> parameters) {
            this.authSchemes.get().add(Pair.of(Identifier.of(scheme),
                    parameters.entrySet().stream()
                            .collect(Collectors.toMap(k -> Identifier.of(k.getKey()), Map.Entry::getValue))));
            return this;
        }

        public Builder headers(Map<String, List<Expr>> headers) {
            this.headers.clear();
            this.headers.get().putAll(headers);
            return this;
        }

        public Builder addHeader(String name, List<Expr> value) {
            this.headers.get().put(name, value);
            return this;
        }

        public Builder addHeader(String name, Literal value) {
            // Note: if we want to add multi-header support in the future we'll need to tackle that separately
            if (this.headers.get().containsKey(name)) {
                throw new RuntimeException(String.format("A header already exists for %s", name));
            }
            this.headers.get().put(name, Collections.singletonList(value));
            return this;
        }

        @Override
        public Endpoint build() {
            return new Endpoint(this);
        }
    }
}
