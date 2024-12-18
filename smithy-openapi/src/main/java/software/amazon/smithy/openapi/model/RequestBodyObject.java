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

public final class RequestBodyObject extends Component implements ToSmithyBuilder<RequestBodyObject> {
    private final String description;
    private final Map<String, MediaTypeObject> content = new TreeMap<>();
    private final boolean required;

    private RequestBodyObject(Builder builder) {
        super(builder);
        description = builder.description;
        content.putAll(builder.content);
        required = builder.required;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }

    public Map<String, MediaTypeObject> getContent() {
        return content;
    }

    public boolean isRequired() {
        return required;
    }

    @Override
    protected ObjectNode.Builder createNodeBuilder() {
        ObjectNode.Builder builder = Node.objectNodeBuilder()
                .withOptionalMember("description", getDescription().map(Node::from))
                .withMember("content",
                        content.entrySet()
                                .stream()
                                .collect(ObjectNode.collectStringKeys(Map.Entry::getKey, Map.Entry::getValue)));

        if (required) {
            builder.withMember("required", Node.from(true));
        }

        return builder;
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .extensions(getExtensions())
                .description(description)
                .content(content)
                .required(required);
    }

    public static final class Builder extends Component.Builder<Builder, RequestBodyObject> {
        private final Map<String, MediaTypeObject> content = new TreeMap<>();
        private String description;
        private boolean required;

        private Builder() {}

        @Override
        public RequestBodyObject build() {
            return new RequestBodyObject(this);
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder content(Map<String, MediaTypeObject> content) {
            this.content.clear();
            this.content.putAll(content);
            return this;
        }

        public Builder putContent(String name, MediaTypeObject content) {
            this.content.put(name, content);
            return this;
        }

        public Builder required(boolean required) {
            this.required = required;
            return this;
        }
    }
}
