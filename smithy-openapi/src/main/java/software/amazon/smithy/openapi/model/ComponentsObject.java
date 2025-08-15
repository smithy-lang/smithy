/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.openapi.model;

import java.util.Map;
import java.util.TreeMap;
import software.amazon.smithy.jsonschema.Schema;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.utils.BuilderRef;
import software.amazon.smithy.utils.ToSmithyBuilder;

public final class ComponentsObject extends Component implements ToSmithyBuilder<ComponentsObject> {
    private final Map<String, Schema> schemas;
    private final Map<String, ResponseObject> responses;
    private final Map<String, ParameterObject> parameters;
    private final Map<String, RequestBodyObject> requestBodies;
    private final Map<String, ParameterObject> headers;
    private final Map<String, SecurityScheme> securitySchemes;
    private final Map<String, LinkObject> links;
    private final Map<String, CallbackObject> callbacks;

    private ComponentsObject(Builder builder) {
        super(builder);
        schemas = new TreeMap<>(builder.schemas.peek());
        responses = new TreeMap<>(builder.responses.peek());
        parameters = new TreeMap<>(builder.parameters.peek());
        requestBodies = new TreeMap<>(builder.requestBodies.peek());
        headers = new TreeMap<>(builder.headers.peek());
        securitySchemes = new TreeMap<>(builder.securitySchemes.peek());
        links = new TreeMap<>(builder.links.peek());
        callbacks = new TreeMap<>(builder.callbacks.peek());
    }

    public static Builder builder() {
        return new Builder();
    }

    public Map<String, Schema> getSchemas() {
        return schemas;
    }

    public Map<String, ResponseObject> getResponses() {
        return responses;
    }

    public Map<String, ParameterObject> getParameters() {
        return parameters;
    }

    public Map<String, RequestBodyObject> getRequestBodies() {
        return requestBodies;
    }

    public Map<String, ParameterObject> getHeaders() {
        return headers;
    }

    public Map<String, SecurityScheme> getSecuritySchemes() {
        return securitySchemes;
    }

    public Map<String, LinkObject> getLinks() {
        return links;
    }

    public Map<String, CallbackObject> getCallbacks() {
        return callbacks;
    }

    @Override
    protected ObjectNode.Builder createNodeBuilder() {
        ObjectNode.Builder builder = Node.objectNodeBuilder();

        if (!schemas.isEmpty()) {
            builder.withMember("schemas",
                    schemas.entrySet()
                            .stream()
                            .collect(ObjectNode.collectStringKeys(Map.Entry::getKey, Map.Entry::getValue)));
        }

        if (!responses.isEmpty()) {
            builder.withMember("responses",
                    responses.entrySet()
                            .stream()
                            .collect(ObjectNode.collectStringKeys(Map.Entry::getKey, Map.Entry::getValue)));
        }

        if (!parameters.isEmpty()) {
            builder.withMember("parameters",
                    parameters.entrySet()
                            .stream()
                            .collect(ObjectNode.collectStringKeys(Map.Entry::getKey, Map.Entry::getValue)));
        }

        if (!requestBodies.isEmpty()) {
            builder.withMember("requestBodies",
                    requestBodies.entrySet()
                            .stream()
                            .collect(ObjectNode.collectStringKeys(Map.Entry::getKey, Map.Entry::getValue)));
        }

        if (!headers.isEmpty()) {
            builder.withMember("headers",
                    headers.entrySet()
                            .stream()
                            .collect(ObjectNode.collectStringKeys(Map.Entry::getKey, Map.Entry::getValue)));
        }

        if (!securitySchemes.isEmpty()) {
            builder.withMember("securitySchemes",
                    securitySchemes.entrySet()
                            .stream()
                            .collect(ObjectNode.collectStringKeys(Map.Entry::getKey, Map.Entry::getValue)));
        }

        if (!links.isEmpty()) {
            builder.withMember("links",
                    links.entrySet()
                            .stream()
                            .collect(ObjectNode.collectStringKeys(Map.Entry::getKey, Map.Entry::getValue)));
        }

        if (!callbacks.isEmpty()) {
            builder.withMember("callbacks",
                    callbacks.entrySet()
                            .stream()
                            .collect(ObjectNode.collectStringKeys(Map.Entry::getKey, Map.Entry::getValue)));
        }

        return builder;
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .schemas(schemas)
                .responses(responses)
                .parameters(parameters)
                .requestBodies(requestBodies)
                .headers(headers)
                .securitySchemes(securitySchemes)
                .links(links)
                .callbacks(callbacks)
                .extensions(getExtensions());
    }

    public static final class Builder extends Component.Builder<Builder, ComponentsObject> {
        private final BuilderRef<Map<String, Schema>> schemas = BuilderRef.forSortedMap();
        private final BuilderRef<Map<String, ResponseObject>> responses = BuilderRef.forSortedMap();
        private final BuilderRef<Map<String, ParameterObject>> parameters = BuilderRef.forSortedMap();
        private final BuilderRef<Map<String, RequestBodyObject>> requestBodies = BuilderRef.forSortedMap();
        private final BuilderRef<Map<String, ParameterObject>> headers = BuilderRef.forSortedMap();
        private final BuilderRef<Map<String, SecurityScheme>> securitySchemes = BuilderRef.forSortedMap();
        private final BuilderRef<Map<String, LinkObject>> links = BuilderRef.forSortedMap();
        private final BuilderRef<Map<String, CallbackObject>> callbacks = BuilderRef.forSortedMap();

        private Builder() {}

        @Override
        public ComponentsObject build() {
            return new ComponentsObject(this);
        }

        public Builder schemas(Map<String, Schema> schemas) {
            this.schemas.clear();
            schemas.forEach(this::putSchema);
            return this;
        }

        public Builder putSchema(String name, Schema schema) {
            schemas.get().put(name, schema);
            return this;
        }

        public Builder removeSchema(String name) {
            schemas.get().remove(name);
            return this;
        }

        public Builder responses(Map<String, ResponseObject> responses) {
            this.responses.clear();
            responses.forEach(this::putResponse);
            return this;
        }

        public Builder putResponse(String name, ResponseObject response) {
            responses.get().put(name, response);
            return this;
        }

        public Builder parameters(Map<String, ParameterObject> parameters) {
            this.parameters.clear();
            parameters.forEach(this::putParameter);
            return this;
        }

        public Builder putParameter(String name, ParameterObject parameter) {
            parameters.get().put(name, parameter);
            return this;
        }

        public Builder requestBodies(Map<String, RequestBodyObject> requestBodies) {
            this.requestBodies.clear();
            requestBodies.forEach(this::putRequestBodies);
            return this;
        }

        public Builder putRequestBodies(String name, RequestBodyObject requestBody) {
            requestBodies.get().put(name, requestBody);
            return this;
        }

        public Builder headers(Map<String, ParameterObject> headers) {
            this.headers.clear();
            headers.forEach(this::putHeader);
            return this;
        }

        public Builder putHeader(String name, ParameterObject header) {
            headers.get().put(name, header);
            return this;
        }

        public Builder securitySchemes(Map<String, SecurityScheme> securitySchemes) {
            this.securitySchemes.clear();
            securitySchemes.forEach(this::putSecurityScheme);
            return this;
        }

        public Builder putSecurityScheme(String name, SecurityScheme securityScheme) {
            securitySchemes.get().put(name, securityScheme);
            return this;
        }

        public Builder removeSecurityScheme(String name) {
            securitySchemes.get().remove(name);
            return this;
        }

        public Builder links(Map<String, LinkObject> links) {
            this.links.clear();
            links.forEach(this::putLink);
            return this;
        }

        public Builder putLink(String name, LinkObject link) {
            links.get().put(name, link);
            return this;
        }

        public Builder callbacks(Map<String, CallbackObject> callbacks) {
            this.callbacks.clear();
            callbacks.forEach(this::putCallbacks);
            return this;
        }

        public Builder putCallbacks(String name, CallbackObject callback) {
            callbacks.get().put(name, callback);
            return this;
        }
    }
}
