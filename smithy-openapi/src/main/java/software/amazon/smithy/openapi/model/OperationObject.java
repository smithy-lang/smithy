/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.openapi.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.Pair;
import software.amazon.smithy.utils.ToSmithyBuilder;

public final class OperationObject extends Component implements ToSmithyBuilder<OperationObject> {
    private final String summary;
    private final String description;
    private final ExternalDocumentation externalDocs;
    private final String operationId;
    private final RequestBodyObject requestBody;
    private final boolean deprecated;
    private final List<String> tags;
    private final List<ParameterObject> parameters;
    private final Map<String, ResponseObject> responses;
    private final Map<String, CallbackObject> callbacks;
    private final List<Map<String, List<String>>> security;
    private final List<ServerObject> servers;

    private OperationObject(Builder builder) {
        super(builder);
        tags = ListUtils.copyOf(builder.tags);
        summary = builder.summary;
        description = builder.description;
        externalDocs = builder.externalDocs;
        operationId = builder.operationId;
        parameters = ListUtils.copyOf(builder.parameters);
        requestBody = builder.requestBody;
        responses = Collections.unmodifiableMap(new TreeMap<>(builder.responses));
        deprecated = builder.deprecated;
        callbacks = Collections.unmodifiableMap(new TreeMap<>(builder.callbacks));
        if (builder.security != null) {
            security = ListUtils.copyOf(builder.security);
        } else {
            security = null;
        }
        servers = ListUtils.copyOf(builder.servers);
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<String> getTags() {
        return tags;
    }

    public Optional<String> getSummary() {
        return Optional.ofNullable(summary);
    }

    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }

    public Optional<ExternalDocumentation> getExternalDocs() {
        return Optional.ofNullable(externalDocs);
    }

    public Optional<String> getOperationId() {
        return Optional.ofNullable(operationId);
    }

    public List<ParameterObject> getParameters() {
        return parameters;
    }

    public Optional<RequestBodyObject> getRequestBody() {
        return Optional.ofNullable(requestBody);
    }

    public Map<String, ResponseObject> getResponses() {
        return responses;
    }

    public Map<String, CallbackObject> getCallbacks() {
        return callbacks;
    }

    public boolean isDeprecated() {
        return deprecated;
    }

    public Optional<List<Map<String, List<String>>>> getSecurity() {
        return Optional.ofNullable(security);
    }

    public List<ServerObject> getServers() {
        return servers;
    }

    @Override
    protected ObjectNode.Builder createNodeBuilder() {
        ObjectNode.Builder builder = Node.objectNodeBuilder()
                .withOptionalMember("description", getDescription().map(Node::from))
                .withOptionalMember("summary", getSummary().map(Node::from))
                .withOptionalMember("externalDocs", getExternalDocs())
                .withOptionalMember("operationId", getOperationId().map(Node::from))
                .withOptionalMember("requestBody", getRequestBody());

        if (isDeprecated()) {
            builder.withMember("deprecated", Node.from(true));
        }

        if (!parameters.isEmpty()) {
            builder.withMember("parameters", getParameters().stream().collect(ArrayNode.collect()));
        }

        if (!responses.isEmpty()) {
            builder.withMember("responses",
                    getResponses().entrySet()
                            .stream()
                            .collect(ObjectNode.collectStringKeys(Map.Entry::getKey, Map.Entry::getValue)));
        }

        if (!callbacks.isEmpty()) {
            builder.withMember("callbacks",
                    getCallbacks().entrySet()
                            .stream()
                            .collect(ObjectNode.collectStringKeys(Map.Entry::getKey, Map.Entry::getValue)));
        }

        if (getSecurity().isPresent()) {
            builder.withMember("security",
                    getSecurity().get()
                            .stream()
                            .map(map -> map.entrySet()
                                    .stream()
                                    .sorted(Comparator.comparing(Map.Entry::getKey))
                                    .map(entry -> Pair.of(entry.getKey(),
                                            entry.getValue()
                                                    .stream()
                                                    .map(Node::from)
                                                    .collect(ArrayNode.collect())))
                                    .collect(ObjectNode.collectStringKeys(Pair::getLeft, Pair::getRight)))
                            .collect(ArrayNode.collect()));
        }

        if (!servers.isEmpty()) {
            builder.withMember("servers",
                    getServers().stream()
                            .map(ServerObject::toNode)
                            .collect(ArrayNode.collect()));
        }

        if (!tags.isEmpty()) {
            builder.withMember("tags", getTags().stream().map(Node::from).collect(ArrayNode.collect()));
        }

        return builder;
    }

    @Override
    public Builder toBuilder() {
        Builder builder = builder()
                .extensions(getExtensions())
                .callbacks(callbacks)
                .responses(responses)
                .parameters(parameters)
                .servers(servers)
                .summary(summary)
                .tags(tags)
                .deprecated(deprecated)
                .description(description)
                .externalDocs(externalDocs)
                .operationId(operationId)
                .requestBody(requestBody);
        getSecurity().ifPresent(builder::security);
        return builder;
    }

    public static final class Builder extends Component.Builder<Builder, OperationObject> {
        private final List<String> tags = new ArrayList<>();
        private final List<ParameterObject> parameters = new ArrayList<>();
        private final Map<String, ResponseObject> responses = new TreeMap<>();
        private final Map<String, CallbackObject> callbacks = new TreeMap<>();
        // Use a set for security as duplicate entries are unnecessary (effectively
        // represent an "A or A" security posture) and can cause downstream issues.
        private Set<Map<String, List<String>>> security;
        private final List<ServerObject> servers = new ArrayList<>();
        private String summary;
        private String description;
        private ExternalDocumentation externalDocs;
        private String operationId;
        private RequestBodyObject requestBody;
        private boolean deprecated;

        private Builder() {}

        @Override
        public OperationObject build() {
            return new OperationObject(this);
        }

        public Builder tags(Collection<String> tags) {
            this.tags.clear();
            this.tags.addAll(tags);
            return this;
        }

        public Builder addTag(String tag) {
            tags.add(tag);
            return this;
        }

        public Builder summary(String summary) {
            this.summary = summary;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder externalDocs(ExternalDocumentation externalDocs) {
            this.externalDocs = externalDocs;
            return this;
        }

        public Builder operationId(String operationId) {
            this.operationId = operationId;
            return this;
        }

        public Builder parameters(Collection<ParameterObject> parameters) {
            this.parameters.clear();
            this.parameters.addAll(parameters);
            return this;
        }

        public Builder addParameter(ParameterObject parameter) {
            parameters.add(parameter);
            return this;
        }

        public Builder requestBody(RequestBodyObject requestBody) {
            this.requestBody = requestBody;
            return this;
        }

        public Builder responses(Map<String, ResponseObject> responses) {
            this.responses.clear();
            this.responses.putAll(responses);
            return this;
        }

        public Builder putResponse(String statusCode, ResponseObject response) {
            responses.put(statusCode, response);
            return this;
        }

        public Builder callbacks(Map<String, CallbackObject> callbacks) {
            this.callbacks.clear();
            this.callbacks.putAll(callbacks);
            return this;
        }

        public Builder putCallback(String expression, CallbackObject callback) {
            callbacks.put(expression, callback);
            return this;
        }

        public Builder deprecated(boolean deprecated) {
            this.deprecated = deprecated;
            return this;
        }

        public Builder security(Collection<Map<String, List<String>>> security) {
            this.security = new LinkedHashSet<>();
            this.security.addAll(security);
            return this;
        }

        public Builder addSecurity(Map<String, List<String>> security) {
            if (this.security == null) {
                this.security = new LinkedHashSet<>();
            }
            this.security.add(security);
            return this;
        }

        public Builder servers(Collection<ServerObject> servers) {
            this.servers.clear();
            this.servers.addAll(servers);
            return this;
        }

        public Builder addServer(ServerObject server) {
            servers.add(server);
            return this;
        }
    }
}
