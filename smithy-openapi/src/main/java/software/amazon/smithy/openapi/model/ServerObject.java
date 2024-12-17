/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.openapi.model;

import java.util.Map;
import java.util.Optional;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

public final class ServerObject extends Component implements ToSmithyBuilder<ServerObject> {
    private final String url;
    private final String description;
    private final Map<String, ObjectNode> variables;

    private ServerObject(Builder builder) {
        super(builder);
        url = SmithyBuilder.requiredState("url", builder.url);
        description = builder.description;
        variables = MapUtils.copyOf(builder.variables);
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isEmpty() {
        return false;
    }

    public String getUrl() {
        return url;
    }

    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }

    public Map<String, ObjectNode> getVariables() {
        return variables;
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .extensions(getExtensions())
                .url(url)
                .description(description)
                .variables(variables);
    }

    @Override
    protected ObjectNode.Builder createNodeBuilder() {
        ObjectNode.Builder builder = Node.objectNodeBuilder()
                .withMember("url", getUrl())
                .withOptionalMember("description", getDescription().map(Node::from));

        if (!variables.isEmpty()) {
            builder.withMember("variables",
                    getVariables().entrySet()
                            .stream()
                            .collect(ObjectNode.collectStringKeys(Map.Entry::getKey, Map.Entry::getValue)));
        }

        return builder;
    }

    public static final class Builder extends Component.Builder<Builder, ServerObject> {
        private String url;
        private String description;
        private Map<String, ObjectNode> variables;

        @Override
        public ServerObject build() {
            return new ServerObject(this);
        }

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder variables(Map<String, ObjectNode> variables) {
            this.variables = variables;
            return this;
        }
    }
}
