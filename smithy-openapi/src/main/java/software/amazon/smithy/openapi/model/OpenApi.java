/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.openapi.model;

import java.util.ArrayList;
import java.util.Collection;
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
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

public final class OpenApi extends Component implements ToSmithyBuilder<OpenApi> {
    private final String openapi;
    private final InfoObject info;
    private final List<ServerObject> servers;
    private final Map<String, PathItem> paths = new TreeMap<>();
    private final ComponentsObject components;
    private final List<Map<String, List<String>>> security;
    private final List<TagObject> tags;
    private final ExternalDocumentation externalDocs;

    private OpenApi(Builder builder) {
        super(builder);
        openapi = SmithyBuilder.requiredState("openapi", builder.openapi);
        info = SmithyBuilder.requiredState("info", builder.info);
        servers = ListUtils.copyOf(builder.servers);
        paths.putAll(builder.paths);
        components = builder.components == null ? ComponentsObject.builder().build() : builder.components;
        security = ListUtils.copyOf(builder.security);
        tags = ListUtils.copyOf(builder.tags);
        externalDocs = builder.externalDocs;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getOpenapi() {
        return openapi;
    }

    public InfoObject getInfo() {
        return info;
    }

    public List<ServerObject> getServers() {
        return servers;
    }

    public Map<String, PathItem> getPaths() {
        return paths;
    }

    public ComponentsObject getComponents() {
        return components;
    }

    public List<Map<String, List<String>>> getSecurity() {
        return security;
    }

    public List<TagObject> getTags() {
        return tags;
    }

    public Optional<ExternalDocumentation> getExternalDocs() {
        return Optional.ofNullable(externalDocs);
    }

    @Override
    public Builder toBuilder() {
        Builder builder = builder()
                .openapi(openapi)
                .info(info)
                .paths(paths)
                .components(components)
                .externalDocs(externalDocs)
                .extensions(getExtensions());
        security.forEach(builder::addSecurity);
        servers.forEach(builder::addServer);
        tags.forEach(builder::addTag);
        return builder;
    }

    @Override
    protected ObjectNode.Builder createNodeBuilder() {
        ObjectNode.Builder builder = Node.objectNodeBuilder()
                .withMember("openapi", openapi)
                .withMember("info", info)
                .withOptionalMember("externalDocumentation", getExternalDocs());

        if (!servers.isEmpty()) {
            builder.withMember("servers", servers.stream().collect(ArrayNode.collect()));
        }

        if (!paths.isEmpty()) {
            builder.withMember("paths", paths.entrySet().stream()
                    .collect(ObjectNode.collectStringKeys(Map.Entry::getKey, Map.Entry::getValue)));
        }

        builder.withMember("components", components);

        if (!security.isEmpty()) {
            builder.withMember("security", security.stream()
                    .map(mapping -> mapping.entrySet().stream()
                            .sorted(Comparator.comparing(Map.Entry::getKey))
                            .collect(ObjectNode.collectStringKeys(
                                    Map.Entry::getKey,
                                    entry -> entry.getValue().stream().map(Node::from).collect(ArrayNode.collect()))))
                    .collect(ArrayNode.collect()));
        }

        if (!tags.isEmpty()) {
            builder.withMember("tags", tags.stream().collect(ArrayNode.collect()));
        }

        return builder;
    }

    public static final class Builder extends Component.Builder<Builder, OpenApi> {
        private String openapi;
        private InfoObject info;
        private final List<ServerObject> servers = new ArrayList<>();
        private Map<String, PathItem> paths = new TreeMap<>();
        private ComponentsObject components;
        // Use a set for security as duplicate entries are unnecessary (effectively
        // represent an "A or A" security posture) and can cause downstream issues.
        private final Set<Map<String, List<String>>> security = new LinkedHashSet<>();
        private final List<TagObject> tags = new ArrayList<>();
        private ExternalDocumentation externalDocs;

        private Builder() {}

        @Override
        public OpenApi build() {
            return new OpenApi(this);
        }

        public Builder openapi(String openapi) {
            this.openapi = openapi;
            return this;
        }

        public Builder info(InfoObject info) {
            this.info = info;
            return this;
        }

        public Builder paths(Map<String, PathItem> paths) {
            this.paths = paths;
            return this;
        }

        public Builder putPath(String path, PathItem item) {
            paths.put(path, item);
            return this;
        }

        public Builder removePath(String path) {
            paths.remove(path);
            return this;
        }

        public Builder components(ComponentsObject components) {
            this.components = components;
            return this;
        }

        public Builder externalDocs(ExternalDocumentation externalDocs) {
            this.externalDocs = externalDocs;
            return this;
        }

        public Builder addServer(ServerObject server) {
            this.servers.add(server);
            return this;
        }

        public Builder clearServer() {
            servers.clear();
            return this;
        }

        public Builder addSecurity(Map<String, List<String>> requirement) {
            this.security.add(requirement);
            return this;
        }

        public Builder security(Collection<Map<String, List<String>>> security) {
            this.security.clear();
            this.security.addAll(security);
            return this;
        }

        public Builder clearSecurity() {
            security.clear();
            return this;
        }

        public Builder addTag(TagObject tag) {
            this.tags.add(tag);
            return this;
        }

        public Builder clearTags() {
            tags.clear();
            return this;
        }
    }
}
