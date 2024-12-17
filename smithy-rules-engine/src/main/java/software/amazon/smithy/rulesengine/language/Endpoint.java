/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.language;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.rulesengine.language.error.RuleError;
import software.amazon.smithy.rulesengine.language.evaluation.Scope;
import software.amazon.smithy.rulesengine.language.evaluation.TypeCheck;
import software.amazon.smithy.rulesengine.language.evaluation.type.Type;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.literal.Literal;
import software.amazon.smithy.utils.BuilderRef;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.SmithyUnstableApi;
import software.amazon.smithy.utils.StringUtils;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * An EndpointType as returned by EndpointRules.
 */
@SmithyUnstableApi
public final class Endpoint implements FromSourceLocation, ToNode, ToSmithyBuilder<Endpoint>, TypeCheck {
    private static final String URL = "url";
    private static final String PROPERTIES = "properties";
    private static final String HEADERS = "headers";
    private static final Identifier ID_AUTH_SCHEMES = Identifier.of("authSchemes");
    private static final Identifier ID_NAME = Identifier.of("name");

    private final Map<String, List<Expression>> headers;
    private final Map<Identifier, Literal> properties;
    private final SourceLocation sourceLocation;
    private final Expression url;

    private Endpoint(Builder builder) {
        super();
        this.headers = builder.headers.copy();
        this.sourceLocation = SmithyBuilder.requiredState("source", builder.getSourceLocation());
        this.url = SmithyBuilder.requiredState("url", builder.url);

        List<Literal> authSchemes = new ArrayList<>();
        for (Map.Entry<Identifier, Map<Identifier, Literal>> authScheme : builder.authSchemes.get().entrySet()) {
            Map<Identifier, Literal> base = new TreeMap<>(Comparator.comparing(Identifier::toString));
            base.put(ID_NAME, Literal.of(authScheme.getKey().toString()));
            base.putAll(authScheme.getValue());
            authSchemes.add(Literal.recordLiteral(base));
        }
        if (!authSchemes.isEmpty()) {
            builder.putProperty(ID_AUTH_SCHEMES, Literal.tupleLiteral(authSchemes));
        }

        this.properties = builder.properties.copy();
    }

    /**
     * Constructs an {@link Endpoint} from a {@link Node}. Node must be an {@link ObjectNode}.
     *
     * @param node the object node.
     * @return the node as an {@link Endpoint}.
     */
    public static Endpoint fromNode(Node node) {
        ObjectNode objectNode = node.expectObjectNode();
        Builder builder = builder().sourceLocation(node);

        builder.url(Expression.fromNode(objectNode.expectMember(URL, "URL must be included in endpoint")));
        objectNode.expectNoAdditionalProperties(Arrays.asList(PROPERTIES, HEADERS, URL));

        objectNode.getObjectMember(PROPERTIES, properties -> {
            for (Map.Entry<StringNode, Node> member : properties.getMembers().entrySet()) {
                builder.putProperty(Identifier.of(member.getKey()), Literal.fromNode(member.getValue()));
            }
        });

        objectNode.getObjectMember(HEADERS, headers -> {
            for (Map.Entry<String, Node> header : headers.getStringMap().entrySet()) {
                builder.putHeader(header.getKey(),
                        header.getValue()
                                .expectArrayNode("header values should be an array")
                                .getElementsAs(Expression::fromNode));
            }
        });

        return builder.build();
    }

    /**
     * Create a new EndpointType builder.
     *
     * @return EndpointType builder
     */
    public static Builder builder() {
        return new Builder(SourceLocation.none());
    }

    @Override
    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }

    /**
     * Returns the EndpointType URL as an expression.
     *
     * @return the endpoint URL expression.
     */
    public Expression getUrl() {
        return url;
    }

    /**
     * Get the endpoint headers as a map of {@link String} to list of {@link Expression} values.
     *
     * @return the endpoint headers.
     */
    public Map<String, List<Expression>> getHeaders() {
        return headers;
    }

    /**
     * Get the endpoint properties as a map of {@link Identifier} to {@link Literal} values.
     *
     * @return the endpoint properties.
     */
    public Map<Identifier, Literal> getProperties() {
        return properties;
    }

    /**
     * Get the endpoint {@code authSchemes} property as a map of {@link Identifier} to {@link Literal} values.
     *
     * @return the list of endpoint {@code authSchemes}.
     */
    public List<Map<Identifier, Literal>> getEndpointAuthSchemes() {
        return Optional.ofNullable(getProperties().get(ID_AUTH_SCHEMES))
                .map(a -> a.asTupleLiteral()
                        .get()
                        .stream()
                        .map(l -> l.asRecordLiteral().get())
                        .collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    @Override
    public Builder toBuilder() {
        return builder().sourceLocation(sourceLocation).url(url).headers(headers).properties(properties);
    }

    @Override
    public Node toNode() {
        ObjectNode.Builder propertiesBuilder = ObjectNode.builder();
        for (Map.Entry<Identifier, Literal> entry : properties.entrySet()) {
            propertiesBuilder.withMember(entry.getKey().toString(), entry.getValue().toNode());
        }

        ObjectNode.Builder headersBuilder = ObjectNode.builder();
        for (Map.Entry<String, List<Expression>> entry : headers.entrySet()) {
            List<Node> expressionNodes = new ArrayList<>();
            for (Expression expression : entry.getValue()) {
                expressionNodes.add(expression.toNode());
            }
            headersBuilder.withMember(entry.getKey(), ArrayNode.fromNodes(expressionNodes));
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
        Endpoint endpoint = (Endpoint) o;
        return url.equals(endpoint.url) && properties.equals(endpoint.properties) && headers.equals(endpoint.headers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, properties, headers);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("url: ").append(url).append("\n");

        if (!headers.isEmpty()) {
            sb.append("headers:\n");
            for (Map.Entry<String, List<Expression>> entry : headers.entrySet()) {
                sb.append(StringUtils.indent(String.format("%s: %s", entry.getKey(), entry.getValue()), 2))
                        .append("\n");
            }
        }

        if (!properties.isEmpty()) {
            sb.append("properties:\n");
            for (Map.Entry<Identifier, Literal> entry : properties.entrySet()) {
                sb.append(StringUtils.indent(String.format("%s: %s", entry.getKey(), entry.getValue()), 2))
                        .append("\n");
            }
        }

        return sb.toString();
    }

    @Override
    public Type typeCheck(Scope<Type> scope) {
        RuleError.context("while checking the URL", url, () -> url.typeCheck(scope).expectStringType());

        RuleError.context("while checking properties", () -> {
            for (Literal literal : properties.values()) {
                literal.typeCheck(scope);
            }
            return null;
        });

        RuleError.context("while checking headers", () -> {
            for (List<Expression> headerList : headers.values()) {
                for (Expression header : headerList) {
                    header.typeCheck(scope).expectStringType();
                }
            }
            return null;
        });

        return Type.endpointType();
    }

    /**
     * Builder for {@link Endpoint}.
     */
    public static class Builder extends RulesComponentBuilder<Builder, Endpoint> {
        private final BuilderRef<Map<String, List<Expression>>> headers = BuilderRef.forOrderedMap();
        private final BuilderRef<Map<Identifier, Literal>> properties = BuilderRef.forOrderedMap();
        private final BuilderRef<Map<Identifier, Map<Identifier, Literal>>> authSchemes = BuilderRef.forOrderedMap();
        private Expression url;

        public Builder(FromSourceLocation sourceLocation) {
            super(sourceLocation);
        }

        public Builder url(Expression url) {
            this.url = url;
            return this;
        }

        public Builder putProperty(Identifier identifier, Literal value) {
            this.properties.get().put(identifier, value);
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
            this.authSchemes.get().put(scheme, parameters);
            return this;
        }

        public Builder addAuthScheme(String scheme, Map<String, Literal> parameters) {
            Map<Identifier, Literal> transformedParameters = new HashMap<>();
            for (Map.Entry<String, Literal> parameter : parameters.entrySet()) {
                transformedParameters.put(Identifier.of(parameter.getKey()), parameter.getValue());
            }
            return addAuthScheme(Identifier.of(scheme), transformedParameters);
        }

        public Builder headers(Map<String, List<Expression>> headers) {
            this.headers.clear();
            this.headers.get().putAll(headers);
            return this;
        }

        public Builder putHeader(String name, List<Expression> value) {
            this.headers.get().put(name, value);
            return this;
        }

        public Builder putHeader(String name, Literal value) {
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
