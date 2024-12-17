/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.protocoltests.traits;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.Tagged;

public abstract class HttpMessageTestCase implements ToNode, Tagged {

    private static final String ID = "id";
    private static final String PROTOCOL = "protocol";
    private static final String DOCUMENTATION = "documentation";
    private static final String AUTH_SCHEME = "authScheme";
    private static final String BODY = "body";
    private static final String BODY_MEDIA_TYPE = "bodyMediaType";
    private static final String PARAMS = "params";
    private static final String VENDOR_PARAMS_SHAPE = "vendorParamsShape";
    private static final String VENDOR_PARAMS = "vendorParams";
    private static final String HEADERS = "headers";
    private static final String FORBID_HEADERS = "forbidHeaders";
    private static final String REQUIRE_HEADERS = "requireHeaders";
    private static final String TAGS = "tags";
    private static final String APPLIES_TO = "appliesTo";

    private final String id;
    private final String documentation;
    private final ShapeId protocol;
    private final ShapeId authScheme;
    private final String body;
    private final String bodyMediaType;
    private final ObjectNode params;
    private final ShapeId vendorParamsShape;
    private final ObjectNode vendorParams;
    private final Map<String, String> headers;
    private final List<String> forbidHeaders;
    private final List<String> requireHeaders;
    private final List<String> tags;
    private final AppliesTo appliesTo;

    HttpMessageTestCase(Builder<?, ?> builder) {
        id = SmithyBuilder.requiredState(ID, builder.id);
        protocol = SmithyBuilder.requiredState(PROTOCOL, builder.protocol);
        documentation = builder.documentation;
        authScheme = builder.authScheme;
        body = builder.body;
        bodyMediaType = builder.bodyMediaType;
        params = builder.params;
        vendorParamsShape = builder.vendorParamsShape;
        vendorParams = builder.vendorParams;
        headers = Collections.unmodifiableMap(new TreeMap<>(builder.headers));
        forbidHeaders = ListUtils.copyOf(builder.forbidHeaders);
        requireHeaders = ListUtils.copyOf(builder.requireHeaders);
        tags = ListUtils.copyOf(builder.tags);
        appliesTo = builder.appliesTo;
    }

    public String getId() {
        return id;
    }

    public Optional<String> getDocumentation() {
        return Optional.ofNullable(documentation);
    }

    public ShapeId getProtocol() {
        return protocol;
    }

    public Optional<ShapeId> getAuthScheme() {
        return Optional.ofNullable(authScheme);
    }

    public Optional<String> getBody() {
        return Optional.ofNullable(body);
    }

    public Optional<String> getBodyMediaType() {
        return Optional.ofNullable(bodyMediaType);
    }

    public ObjectNode getParams() {
        return params;
    }

    public Optional<ShapeId> getVendorParamsShape() {
        return Optional.ofNullable(vendorParamsShape);
    }

    public ObjectNode getVendorParams() {
        return vendorParams;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public List<String> getForbidHeaders() {
        return forbidHeaders;
    }

    public List<String> getRequireHeaders() {
        return requireHeaders;
    }

    @Override
    public List<String> getTags() {
        return tags;
    }

    public Optional<AppliesTo> getAppliesTo() {
        return Optional.ofNullable(appliesTo);
    }

    static void updateBuilderFromNode(Builder<?, ?> builder, Node node) {
        ObjectNode o = node.expectObjectNode();
        builder.id(o.expectStringMember(ID).getValue());
        builder.protocol(o.expectStringMember(PROTOCOL).expectShapeId());
        o.getStringMember(DOCUMENTATION).map(StringNode::getValue).ifPresent(builder::documentation);
        o.getStringMember(AUTH_SCHEME).map(StringNode::expectShapeId).ifPresent(builder::authScheme);
        o.getStringMember(BODY).map(StringNode::getValue).ifPresent(builder::body);
        o.getStringMember(BODY_MEDIA_TYPE).map(StringNode::getValue).ifPresent(builder::bodyMediaType);
        o.getObjectMember(PARAMS).ifPresent(builder::params);
        o.getStringMember(VENDOR_PARAMS_SHAPE).map(StringNode::expectShapeId).ifPresent(builder::vendorParamsShape);
        o.getObjectMember(VENDOR_PARAMS).ifPresent(builder::vendorParams);
        o.getStringMember(APPLIES_TO).map(AppliesTo::fromNode).ifPresent(builder::appliesTo);

        o.getObjectMember(HEADERS).ifPresent(headers -> {
            headers.getStringMap().forEach((k, v) -> {
                builder.putHeader(k, v.expectStringNode().getValue());
            });
        });

        o.getArrayMember(FORBID_HEADERS).ifPresent(headers -> {
            builder.forbidHeaders(headers.getElementsAs(StringNode::getValue));
        });

        o.getArrayMember(REQUIRE_HEADERS).ifPresent(headers -> {
            builder.requireHeaders(headers.getElementsAs(StringNode::getValue));
        });

        o.getArrayMember(TAGS).ifPresent(tags -> {
            builder.tags(tags.getElementsAs(StringNode::getValue));
        });
    }

    @Override
    public Node toNode() {
        ObjectNode.Builder builder = Node.objectNodeBuilder()
                .withMember(ID, getId())
                .withMember(PROTOCOL, getProtocol().toString())
                .withOptionalMember(DOCUMENTATION, getDocumentation().map(Node::from))
                .withOptionalMember(AUTH_SCHEME, getAuthScheme().map(ShapeId::toString).map(Node::from))
                .withOptionalMember(BODY, getBody().map(Node::from))
                .withOptionalMember(BODY_MEDIA_TYPE, getBodyMediaType().map(Node::from))
                .withOptionalMember(APPLIES_TO, getAppliesTo())
                .withOptionalMember(VENDOR_PARAMS_SHAPE, getVendorParamsShape().map(ShapeId::toString).map(Node::from));

        if (!headers.isEmpty()) {
            builder.withMember(HEADERS, ObjectNode.fromStringMap(getHeaders()));
        }

        if (!forbidHeaders.isEmpty()) {
            builder.withMember(FORBID_HEADERS, ArrayNode.fromStrings(forbidHeaders));
        }

        if (!requireHeaders.isEmpty()) {
            builder.withMember(REQUIRE_HEADERS, ArrayNode.fromStrings(requireHeaders));
        }

        if (!params.isEmpty()) {
            builder.withMember(PARAMS, getParams());
        }

        if (!vendorParams.isEmpty()) {
            builder.withMember(VENDOR_PARAMS, getVendorParams());
        }

        if (!tags.isEmpty()) {
            builder.withMember(TAGS, ArrayNode.fromStrings(tags));
        }

        return builder.build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o == null || o.getClass() != getClass()) {
            return false;
        } else {
            return toNode().equals(((HttpMessageTestCase) o).toNode());
        }
    }

    @Override
    public int hashCode() {
        return toNode().hashCode();
    }

    void updateBuilder(Builder<?, ?> builder) {
        builder
                .id(id)
                .headers(headers)
                .forbidHeaders(forbidHeaders)
                .requireHeaders(requireHeaders)
                .params(params)
                .vendorParamsShape(vendorParamsShape)
                .vendorParams(vendorParams)
                .documentation(documentation)
                .authScheme(authScheme)
                .protocol(protocol)
                .body(body)
                .bodyMediaType(bodyMediaType)
                .tags(tags)
                .appliesTo(appliesTo);
    }

    abstract static class Builder<B extends Builder<?, ?>, T extends HttpMessageTestCase> implements SmithyBuilder<T> {

        private String id;
        private String documentation;
        private ShapeId protocol;
        private ShapeId authScheme;
        private String body;
        private String bodyMediaType;
        private ObjectNode params = Node.objectNode();
        private ShapeId vendorParamsShape;
        private ObjectNode vendorParams = Node.objectNode();
        private AppliesTo appliesTo;
        private final Map<String, String> headers = new TreeMap<>();
        private final List<String> forbidHeaders = new ArrayList<>();
        private final List<String> requireHeaders = new ArrayList<>();
        private final List<String> tags = new ArrayList<>();

        @SuppressWarnings("unchecked")
        public B id(String id) {
            this.id = id;
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B documentation(String documentation) {
            this.documentation = documentation;
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B protocol(ShapeId protocol) {
            this.protocol = protocol;
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B authScheme(ShapeId authScheme) {
            this.authScheme = authScheme;
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B body(String body) {
            this.body = body;
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B bodyMediaType(String bodyMediaType) {
            this.bodyMediaType = bodyMediaType;
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B params(ObjectNode params) {
            this.params = params;
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B vendorParamsShape(ShapeId vendorParamsShape) {
            this.vendorParamsShape = vendorParamsShape;
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B vendorParams(ObjectNode vendorParams) {
            this.vendorParams = vendorParams;
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B headers(Map<String, String> headers) {
            this.headers.clear();
            this.headers.putAll(headers);
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B putHeader(String key, String value) {
            headers.put(key, value);
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B forbidHeaders(List<String> forbidHeaders) {
            this.forbidHeaders.clear();
            this.forbidHeaders.addAll(forbidHeaders);
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B requireHeaders(List<String> requireHeaders) {
            this.requireHeaders.clear();
            this.requireHeaders.addAll(requireHeaders);
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B tags(List<String> tags) {
            this.tags.clear();
            this.tags.addAll(tags);
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B appliesTo(AppliesTo appliesTo) {
            this.appliesTo = appliesTo;
            return (B) this;
        }
    }
}
