/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.openapi.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.ToSmithyBuilder;

public final class PathItem extends Component implements ToSmithyBuilder<PathItem> {
    private final String summary;
    private final String description;
    private final List<ServerObject> servers;
    private final List<Ref<ParameterObject>> parameters;
    private final OperationObject get;
    private final OperationObject put;
    private final OperationObject post;
    private final OperationObject delete;
    private final OperationObject options;
    private final OperationObject head;
    private final OperationObject patch;
    private final OperationObject trace;

    private PathItem(Builder builder) {
        super(builder);
        summary = builder.summary;
        description = builder.description;
        servers = ListUtils.copyOf(builder.servers);
        parameters = ListUtils.copyOf(builder.parameters);
        get = builder.get;
        put = builder.put;
        post = builder.post;
        delete = builder.delete;
        options = builder.options;
        head = builder.head;
        patch = builder.patch;
        trace = builder.trace;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Optional<String> getSummary() {
        return Optional.ofNullable(summary);
    }

    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }

    public List<ServerObject> getServers() {
        return servers;
    }

    public List<Ref<ParameterObject>> getParameters() {
        return parameters;
    }

    public Optional<OperationObject> getGet() {
        return Optional.ofNullable(get);
    }

    public Optional<OperationObject> getPut() {
        return Optional.ofNullable(put);
    }

    public Optional<OperationObject> getPost() {
        return Optional.ofNullable(post);
    }

    public Optional<OperationObject> getDelete() {
        return Optional.ofNullable(delete);
    }

    public Optional<OperationObject> getOptions() {
        return Optional.ofNullable(options);
    }

    public Optional<OperationObject> getHead() {
        return Optional.ofNullable(head);
    }

    public Optional<OperationObject> getPatch() {
        return Optional.ofNullable(patch);
    }

    public Optional<OperationObject> getTrace() {
        return Optional.ofNullable(trace);
    }

    public Map<String, OperationObject> getOperations() {
        Map<String, OperationObject> operations = new HashMap<>();
        getGet().ifPresent(operation -> operations.put("GET", operation));
        getPut().ifPresent(operation -> operations.put("PUT", operation));
        getPost().ifPresent(operation -> operations.put("POST", operation));
        getDelete().ifPresent(operation -> operations.put("DELETE", operation));
        getOptions().ifPresent(operation -> operations.put("OPTIONS", operation));
        getHead().ifPresent(operation -> operations.put("HEAD", operation));
        getPatch().ifPresent(operation -> operations.put("PATCH", operation));
        getTrace().ifPresent(operation -> operations.put("TRACE", operation));
        return operations;
    }

    public Stream<OperationObject> operations() {
        return getOperations().values().stream();
    }

    @Override
    protected ObjectNode.Builder createNodeBuilder() {
        ObjectNode.Builder builder = Node.objectNodeBuilder()
                .withOptionalMember("description", getDescription().map(Node::from))
                .withOptionalMember("summary", getSummary().map(Node::from))
                .withOptionalMember("delete", getDelete())
                .withOptionalMember("get", getGet())
                .withOptionalMember("head", getHead())
                .withOptionalMember("options", getOptions())
                .withOptionalMember("patch", getPatch())
                .withOptionalMember("post", getPost())
                .withOptionalMember("put", getPut())
                .withOptionalMember("trace", getTrace());

        if (!parameters.isEmpty()) {
            builder.withMember("parameters", getParameters().stream().map(Ref::toNode).collect(ArrayNode.collect()));
        }

        if (!servers.isEmpty()) {
            builder.withMember("servers",
                    getServers().stream()
                            .map(ServerObject::toNode)
                            .collect(ArrayNode.collect()));
        }

        return builder;
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .extensions(getExtensions())
                .summary(summary)
                .description(description)
                .servers(servers)
                .parameters(parameters)
                .get(get)
                .put(put)
                .post(post)
                .delete(delete)
                .options(options)
                .head(head)
                .patch(patch)
                .trace(trace);
    }

    public static final class Builder extends Component.Builder<Builder, PathItem> {
        private String summary;
        private String description;
        private List<ServerObject> servers = new ArrayList<>();
        private List<Ref<ParameterObject>> parameters = new ArrayList<>();
        private OperationObject get;
        private OperationObject put;
        private OperationObject post;
        private OperationObject delete;
        private OperationObject options;
        private OperationObject head;
        private OperationObject patch;
        private OperationObject trace;

        private Builder() {}

        @Override
        public PathItem build() {
            return new PathItem(this);
        }

        public Builder summary(String summary) {
            this.summary = summary;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder servers(List<ServerObject> servers) {
            this.servers.clear();
            this.servers.addAll(servers);
            return this;
        }

        public Builder addServer(ServerObject server) {
            servers.add(server);
            return this;
        }

        public Builder parameters(List<Ref<ParameterObject>> parameters) {
            this.parameters.clear();
            this.parameters.addAll(parameters);
            return this;
        }

        public Builder addParameter(ParameterObject parameter) {
            return addParameter(Ref.local(parameter));
        }

        public Builder addParameter(Ref<ParameterObject> parameter) {
            parameters.add(parameter);
            return this;
        }

        public Builder get(OperationObject get) {
            this.get = get;
            return this;
        }

        public Builder put(OperationObject put) {
            this.put = put;
            return this;
        }

        public Builder post(OperationObject post) {
            this.post = post;
            return this;
        }

        public Builder delete(OperationObject delete) {
            this.delete = delete;
            return this;
        }

        public Builder options(OperationObject options) {
            this.options = options;
            return this;
        }

        public Builder head(OperationObject head) {
            this.head = head;
            return this;
        }

        public Builder patch(OperationObject patch) {
            this.patch = patch;
            return this;
        }

        public Builder trace(OperationObject trace) {
            this.trace = trace;
            return this;
        }
    }
}
