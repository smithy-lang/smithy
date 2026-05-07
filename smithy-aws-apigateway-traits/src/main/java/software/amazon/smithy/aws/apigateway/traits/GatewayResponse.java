/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.apigateway.traits;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.utils.BuilderRef;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * A gateway response configuration for a specific response type.
 *
 * @see GatewayResponsesTrait
 */
public final class GatewayResponse implements ToNode, ToSmithyBuilder<GatewayResponse> {
    private static final String STATUS_CODE = "statusCode";
    private static final String RESPONSE_PARAMETERS = "responseParameters";
    private static final String RESPONSE_TEMPLATES = "responseTemplates";

    private final String statusCode;
    private final Map<String, String> responseParameters;
    private final Map<String, String> responseTemplates;

    private GatewayResponse(Builder builder) {
        statusCode = builder.statusCode;
        responseParameters = builder.responseParameters.copy();
        responseTemplates = builder.responseTemplates.copy();
    }

    /**
     * Creates a builder for a {@link GatewayResponse}.
     *
     * @return Returns the created builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a {@link GatewayResponse} from a Node.
     *
     * @param node Node to deserialize.
     * @return Returns the created GatewayResponse.
     */
    public static GatewayResponse fromNode(Node node) {
        ObjectNode objectNode = node.expectObjectNode();
        Builder builder = builder();
        objectNode.getStringMember(STATUS_CODE)
                .map(StringNode::getValue)
                .ifPresent(builder::statusCode);
        objectNode.getObjectMember(RESPONSE_PARAMETERS).ifPresent(params -> {
            params.getMembers()
                    .forEach((k, v) -> builder.putResponseParameter(k.getValue(), v.expectStringNode().getValue()));
        });
        objectNode.getObjectMember(RESPONSE_TEMPLATES).ifPresent(templates -> {
            templates.getMembers()
                    .forEach((k, v) -> builder.putResponseTemplate(k.getValue(), v.expectStringNode().getValue()));
        });
        return builder.build();
    }

    /**
     * Gets the HTTP status code for the gateway response.
     *
     * @return Returns the optional status code.
     */
    public Optional<String> getStatusCode() {
        return Optional.ofNullable(statusCode);
    }

    /**
     * Gets the response parameters for the gateway response.
     *
     * @return Returns the immutable map of response parameters.
     */
    public Map<String, String> getResponseParameters() {
        return responseParameters;
    }

    /**
     * Gets the response templates for the gateway response.
     *
     * @return Returns the immutable map of response templates.
     */
    public Map<String, String> getResponseTemplates() {
        return responseTemplates;
    }

    @Override
    public Builder toBuilder() {
        Builder builder = builder().statusCode(statusCode);
        builder.responseParameters(responseParameters);
        builder.responseTemplates(responseTemplates);
        return builder;
    }

    @Override
    public Node toNode() {
        ObjectNode.Builder builder = ObjectNode.builder();
        if (statusCode != null) {
            builder.withMember(STATUS_CODE, Node.from(statusCode));
        }
        if (!responseParameters.isEmpty()) {
            ObjectNode.Builder paramsBuilder = ObjectNode.builder();
            for (Map.Entry<String, String> entry : responseParameters.entrySet()) {
                paramsBuilder.withMember(entry.getKey(), Node.from(entry.getValue()));
            }
            builder.withMember(RESPONSE_PARAMETERS, paramsBuilder.build());
        }
        if (!responseTemplates.isEmpty()) {
            ObjectNode.Builder templatesBuilder = ObjectNode.builder();
            for (Map.Entry<String, String> entry : responseTemplates.entrySet()) {
                templatesBuilder.withMember(entry.getKey(), Node.from(entry.getValue()));
            }
            builder.withMember(RESPONSE_TEMPLATES, templatesBuilder.build());
        }
        return builder.build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof GatewayResponse)) {
            return false;
        }
        GatewayResponse that = (GatewayResponse) o;
        return Objects.equals(statusCode, that.statusCode)
                && responseParameters.equals(that.responseParameters)
                && responseTemplates.equals(that.responseTemplates);
    }

    @Override
    public int hashCode() {
        return Objects.hash(statusCode, responseParameters, responseTemplates);
    }

    /**
     * Builder used to create a {@link GatewayResponse}.
     */
    public static final class Builder implements SmithyBuilder<GatewayResponse> {
        private String statusCode;
        private final BuilderRef<Map<String, String>> responseParameters = BuilderRef.forOrderedMap();
        private final BuilderRef<Map<String, String>> responseTemplates = BuilderRef.forOrderedMap();

        @Override
        public GatewayResponse build() {
            return new GatewayResponse(this);
        }

        /**
         * Sets the HTTP status code for the gateway response.
         *
         * @param statusCode Status code to set.
         * @return Returns the builder.
         */
        public Builder statusCode(String statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        /**
         * Adds a response parameter.
         *
         * @param key Parameter key (e.g., {@code gatewayresponse.header.Access-Control-Allow-Origin}).
         * @param value Parameter value.
         * @return Returns the builder.
         */
        public Builder putResponseParameter(String key, String value) {
            responseParameters.get().put(key, value);
            return this;
        }

        /**
         * Adds a response template.
         *
         * @param mediaType Media type key (e.g., {@code application/json}).
         * @param template Template value.
         * @return Returns the builder.
         */
        public Builder putResponseTemplate(String mediaType, String template) {
            responseTemplates.get().put(mediaType, template);
            return this;
        }

        /**
         * Replaces all response parameters with the given map.
         *
         * @param responseParameters Map of response parameters.
         * @return Returns the builder.
         */
        public Builder responseParameters(Map<String, String> responseParameters) {
            this.responseParameters.clear();
            this.responseParameters.get().putAll(responseParameters);
            return this;
        }

        /**
         * Replaces all response templates with the given map.
         *
         * @param responseTemplates Map of response templates.
         * @return Returns the builder.
         */
        public Builder responseTemplates(Map<String, String> responseTemplates) {
            this.responseTemplates.clear();
            this.responseTemplates.get().putAll(responseTemplates);
            return this;
        }
    }
}
