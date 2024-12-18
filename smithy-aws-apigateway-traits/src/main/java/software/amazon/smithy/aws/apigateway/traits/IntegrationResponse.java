/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.apigateway.traits;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodeMapper;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * An API Gateway integration response object.
 *
 * @see <a href="https://docs.aws.amazon.com/apigateway/latest/developerguide/api-gateway-swagger-extensions-integration-response.html">Integration response</a>
 */
public final class IntegrationResponse implements ToNode, ToSmithyBuilder<IntegrationResponse> {

    private final String statusCode;
    private final String contentHandling;
    private final Map<String, String> responseTemplates;
    private final Map<String, String> responseParameters;
    private final FromSourceLocation sourceLocation;

    private IntegrationResponse(Builder builder) {
        statusCode = SmithyBuilder.requiredState("statusCode", builder.statusCode);
        contentHandling = builder.contentHandling;
        responseTemplates = MapUtils.copyOf(builder.responseTemplates);
        responseParameters = MapUtils.copyOf(builder.responseParameters);
        sourceLocation = builder.sourceLocation;
    }

    /**
     * Creates a builder used to build an IntegrationResponse.
     *
     * @return Returns the created builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Gets the status code of the response.
     *
     * @return Returns the status code.
     */
    public String getStatusCode() {
        return statusCode;
    }

    /**
     * Gets the response payload content handling type.
     *
     * <p>Valid values are 1) CONVERT_TO_TEXT, for converting a binary payload
     * into a Base64-encoded string or converting a text payload into a
     * utf-8-encoded string or passing through the text payload natively
     * without modification, and 2) CONVERT_TO_BINARY, for converting a text
     * payload into Base64-decoded blob or passing through a binary payload
     * natively without modification.
     *
     * @return Returns the content handling type.
     */
    public Optional<String> getContentHandling() {
        return Optional.ofNullable(contentHandling);
    }

    /**
     * Gets a map of MIME types to mapping templates for the response payload.
     *
     * @return Returns the immutable map.
     */
    public Map<String, String> getResponseTemplates() {
        return responseTemplates;
    }

    /**
     * Gets a specific response template by MIME type.
     *
     * @param mimeType Response template MIME type.
     * @return Returns the optionally found response template.
     */
    public Optional<String> getResponseTemplate(String mimeType) {
        return Optional.ofNullable(responseTemplates.get(mimeType));
    }

    /**
     * Gets response parameters.
     *
     * @return Returns the map of parameter expressions to how they modify the response.
     * @see <a href="https://docs.aws.amazon.com/apigateway/latest/developerguide/api-gateway-swagger-extensions-integration-responseParameters.html">Response parameters</a>
     */
    public Map<String, String> getResponseParameters() {
        return responseParameters;
    }

    /**
     * Gets a specific response parameter by it's input mapping expression.
     *
     * @param name Header name to retrieve.
     * @return Returns the optionally found response parameter.
     */
    public Optional<String> getResponseParameter(String name) {
        return Optional.ofNullable(responseParameters.get(name));
    }

    @Override
    public Builder toBuilder() {
        Builder builder = builder();
        responseTemplates.forEach(builder::putResponseTemplate);
        responseParameters.forEach(builder::putResponseParameter);
        return builder
                .sourceLocation(sourceLocation)
                .contentHandling(contentHandling)
                .statusCode(statusCode);
    }

    @Override
    public Node toNode() {
        NodeMapper mapper = new NodeMapper();
        mapper.disableToNodeForClass(IntegrationResponse.class);
        mapper.setOmitEmptyValues(true);
        return mapper.serialize(this).expectObjectNode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof IntegrationResponse)) {
            return false;
        }

        IntegrationResponse other = (IntegrationResponse) o;
        return toNode().equals(other.toNode());
    }

    @Override
    public int hashCode() {
        return toNode().hashCode();
    }

    /**
     * Builds a {@link IntegrationResponse}.
     */
    public static final class Builder implements SmithyBuilder<IntegrationResponse> {
        private String statusCode;
        private String contentHandling;
        private Map<String, String> responseTemplates = new HashMap<>();
        private Map<String, String> responseParameters = new HashMap<>();
        private FromSourceLocation sourceLocation;

        @Override
        public IntegrationResponse build() {
            return new IntegrationResponse(this);
        }

        /**
         * Sets the status code of the response.
         *
         * @param statusCode HTTP response status code.
         * @return Returns the builder.
         * @see IntegrationResponse#getStatusCode()
         */
        public Builder statusCode(String statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        /**
         * Sets the content handling of the response template.
         *
         * @param contentHandling Content handling strategy to set.
         * @return Returns the builder.
         * @see IntegrationResponse#getContentHandling()
         */
        public Builder contentHandling(String contentHandling) {
            this.contentHandling = contentHandling;
            return this;
        }

        /**
         * Sets a mapping of response MIME types to templates.
         *
         * @param responseTemplates Map of MIME types to response templates.
         * @return Returns the builder.
         * @see IntegrationResponse#getResponseTemplates()
         */
        public Builder responseTemplates(Map<String, String> responseTemplates) {
            this.responseTemplates.clear();
            this.responseTemplates.putAll(responseTemplates);
            return this;
        }

        /**
         * Adds a response template for a MIME type.
         *
         * @param mimeType MIME type of the response template.
         * @param template Response template for the payload.
         * @return Returns the builder.
         * @see IntegrationResponse#getResponseTemplates()
         */
        public Builder putResponseTemplate(String mimeType, String template) {
            responseTemplates.put(mimeType, template);
            return this;
        }

        /**
         * Remove a response template for a given MIME type.
         *
         * @param mimeType MIME type to remove.
         * @return Returns the builder.
         */
        public Builder removeResponseTemplate(String mimeType) {
            responseTemplates.remove(mimeType);
            return this;
        }

        /**
         * Sets response parameter mappings.
         *
         * @param responseParameters Map of response expressions to expressions to apply to response.
         * @return Returns the builder.
         * @see IntegrationResponse#getResponseParameters()
         */
        public Builder responseParameters(Map<String, String> responseParameters) {
            this.responseParameters.clear();
            this.responseParameters.putAll(responseParameters);
            return this;
        }

        /**
         * Sets a response parameter mapping.
         *
         * @param name Name of the expression to extract.
         * @param value Expression used to apply in the response.
         * @return Returns the builder.
         * @see IntegrationResponse#getResponseParameters()
         */
        public Builder putResponseParameter(String name, String value) {
            responseParameters.put(name, value);
            return this;
        }

        /**
         * Removes a response parameter mapping.
         *
         * @param name Expression to remove.
         * @return Returns the builder.
         */
        public Builder removeResponseParameter(String name) {
            responseParameters.remove(name);
            return this;
        }

        Builder sourceLocation(FromSourceLocation sourceLocation) {
            this.sourceLocation = sourceLocation;
            return this;
        }
    }
}
