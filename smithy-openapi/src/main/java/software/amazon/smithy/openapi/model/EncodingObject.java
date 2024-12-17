/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.openapi.model;

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.utils.ToSmithyBuilder;

public final class EncodingObject extends Component implements ToSmithyBuilder<EncodingObject> {
    private final Map<String, ParameterObject> headers = new TreeMap<>();
    private final String contentType;
    private final String style;
    private final boolean explode;
    private final boolean allowReserved;

    private EncodingObject(Builder builder) {
        super(builder);
        headers.putAll(builder.headers);
        contentType = builder.contentType;
        style = builder.style;
        explode = builder.explode;
        allowReserved = builder.allowReserved;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Map<String, ParameterObject> getHeaders() {
        return headers;
    }

    public Optional<String> getContentType() {
        return Optional.ofNullable(contentType);
    }

    public Optional<String> getStyle() {
        return Optional.ofNullable(style);
    }

    public boolean isExplode() {
        return explode;
    }

    public boolean isAllowReserved() {
        return allowReserved;
    }

    @Override
    protected ObjectNode.Builder createNodeBuilder() {
        ObjectNode.Builder builder = Node.objectNodeBuilder()
                .withOptionalMember("contentType", getContentType().map(Node::from))
                .withOptionalMember("style", getStyle().map(Node::from));

        if (!headers.isEmpty()) {
            builder.withMember("headers",
                    headers.entrySet()
                            .stream()
                            .collect(ObjectNode.collectStringKeys(Map.Entry::getKey, Map.Entry::getValue)));
        }

        if (allowReserved) {
            builder.withMember("allowReserved", Node.from(true));
        }

        if (explode) {
            builder.withMember("explode", Node.from(true));
        }

        return builder;
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .extensions(getExtensions())
                .headers(headers)
                .contentType(contentType)
                .style(style)
                .explode(explode)
                .allowReserved(allowReserved);
    }

    public static final class Builder extends Component.Builder<Builder, EncodingObject> {
        private final Map<String, ParameterObject> headers = new TreeMap<>();
        private String contentType;
        private String style;
        private boolean explode;
        private boolean allowReserved;

        private Builder() {}

        @Override
        public EncodingObject build() {
            return new EncodingObject(this);
        }

        public Builder headers(Map<String, ParameterObject> headers) {
            this.headers.clear();
            this.headers.putAll(headers);
            return this;
        }

        public Builder putHeader(String name, ParameterObject header) {
            headers.put(name, header);
            return this;
        }

        public Builder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public Builder style(String style) {
            this.style = style;
            return this;
        }

        public Builder explode(boolean explode) {
            this.explode = explode;
            return this;
        }

        public Builder allowReserved(boolean allowReserved) {
            this.allowReserved = allowReserved;
            return this;
        }
    }
}
