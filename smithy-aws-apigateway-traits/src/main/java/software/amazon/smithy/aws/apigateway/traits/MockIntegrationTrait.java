/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.apigateway.traits;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodeMapper;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AbstractTrait;
import software.amazon.smithy.model.traits.AbstractTraitBuilder;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * API Gateway mock integration.
 */
public final class MockIntegrationTrait extends AbstractTrait implements ToSmithyBuilder<MockIntegrationTrait> {
    public static final ShapeId ID = ShapeId.from("aws.apigateway#mockIntegration");

    private final String passThroughBehavior;
    private final String contentHandling;
    private final Map<String, String> requestParameters;
    private final Map<String, String> requestTemplates;
    private final Map<String, IntegrationResponse> responses;

    private MockIntegrationTrait(Builder builder) {
        super(ID, builder.getSourceLocation());
        passThroughBehavior = builder.passThroughBehavior;
        contentHandling = builder.contentHandling;
        requestParameters = MapUtils.copyOf(builder.requestParameters);
        requestTemplates = MapUtils.copyOf(builder.requestTemplates);
        responses = MapUtils.copyOf(builder.responses);
    }

    public static final class Provider extends AbstractTrait.Provider {
        public Provider() {
            super(ID);
        }

        @Override
        public Trait createTrait(ShapeId target, Node value) {
            MockIntegrationTrait result = new NodeMapper().deserialize(value, MockIntegrationTrait.class);
            result.setNodeCache(value);
            return result;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Gets the pass through behavior of the integration.
     *
     * <p>Specifies how a request payload of unmapped content type is
     * passed through the integration request without modification.
     * Supported values are when_no_templates, when_no_match, and never.
     *
     * @return Returns the pass through setting.
     * @see <a href="https://docs.aws.amazon.com/apigateway/api-reference/resource/integration/#passthroughBehavior">Pass through behavior</a>
     */
    public Optional<String> getPassThroughBehavior() {
        return Optional.ofNullable(passThroughBehavior);
    }

    /**
     * Gets the request parameter mappings of the integration.
     *
     * <p>Each key is an expression used to extract a value from the request,
     * and each value is an expression of where to place the value in the
     * downstream request. Supported request parameters are querystring, path,
     * header, and body.
     *
     * @return Returns the request parameters.
     * @see <a href="https://docs.aws.amazon.com/apigateway/latest/developerguide/api-gateway-swagger-extensions-integration-requestParameters.html">Request parameters</a>
     */
    public Map<String, String> getRequestParameters() {
        return requestParameters;
    }

    /**
     * Get a specific request parameter by input expression.
     *
     * @param expression Expression to get.
     * @return Returns the optionally found request parameter.
     */
    public Optional<String> getRequestParameter(String expression) {
        return Optional.ofNullable(requestParameters.get(expression));
    }

    /**
     * Gets all request templates of the integration.
     *
     * @return Returns a map of MIME types to request templates.
     * @see <a href="https://docs.aws.amazon.com/apigateway/latest/developerguide/api-gateway-swagger-extensions-integration-requestTemplates.html">Request templates</a>
     */
    public Map<String, String> getRequestTemplates() {
        return requestTemplates;
    }

    /**
     * Gets a specific request template by MIME type.
     *
     * @param mimeType MIME type to get.
     * @return Returns the optionally found template.
     */
    public Optional<String> getRequestTemplate(String mimeType) {
        return Optional.ofNullable(requestTemplates.get(mimeType));
    }

    /**
     * Gets all integration responses.
     *
     * @return Returns a map of status code regular expressions to responses.
     * @see <a href="https://docs.aws.amazon.com/apigateway/latest/developerguide/api-gateway-swagger-extensions-integration-responses.html">Integration responses</a>
     */
    public Map<String, IntegrationResponse> getResponses() {
        return responses;
    }

    /**
     * Get a specific integration response by status code expression.
     *
     * @param statusCode Status code regular expression to search for.
     * @return Returns the optionally found response object.
     */
    public Optional<IntegrationResponse> getResponse(String statusCode) {
        return Optional.ofNullable(responses.get(statusCode));
    }

    /**
     * Gets the contentHandling property of the integration.
     *
     * @return Returns the contentHandling property.
     */
    public Optional<String> getContentHandling() {
        return Optional.ofNullable(contentHandling);
    }

    @Override
    protected ObjectNode createNode() {
        NodeMapper mapper = new NodeMapper();
        mapper.disableToNodeForClass(MockIntegrationTrait.class);
        mapper.setOmitEmptyValues(true);
        return mapper.serialize(this).expectObjectNode();
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .sourceLocation(getSourceLocation())
                .passThroughBehavior(passThroughBehavior)
                .contentHandling(contentHandling)
                .requestParameters(requestParameters)
                .requestTemplates(requestTemplates)
                .responses(responses);
    }

    public static final class Builder extends AbstractTraitBuilder<MockIntegrationTrait, Builder> {
        private String passThroughBehavior;
        private String contentHandling;
        private final Map<String, String> requestParameters = new HashMap<>();
        private final Map<String, String> requestTemplates = new HashMap<>();
        private final Map<String, IntegrationResponse> responses = new HashMap<>();

        @Override
        public MockIntegrationTrait build() {
            return new MockIntegrationTrait(this);
        }

        /**
         * Configures the pass through behavior of the integration.
         *
         * @param passThroughBehavior Pass through behavior setting.
         * @return Returns the builder.
         * @see IntegrationTrait#getPassThroughBehavior()
         */
        public Builder passThroughBehavior(String passThroughBehavior) {
            this.passThroughBehavior = passThroughBehavior;
            return this;
        }

        /**
         * Set the Request payload encoding conversion types.
         *
         * <p>Valid values are:
         *
         * <ul>
         *     <li>CONVERT_TO_TEXT, for converting a binary payload into a
         *     Base64-encoded string or converting a text payload into a
         *     utf-8-encoded string or passing through the text payload natively
         *     without modification</li>
         *     <li>CONVERT_TO_BINARY, for converting a text payload into
         *     Base64-decoded blob or passing through a binary payload natively
         *     without modification.</li>
         * </ul>
         *
         * @param contentHandling Content handling property.
         * @return Returns the builder.
         */
        public Builder contentHandling(String contentHandling) {
            this.contentHandling = contentHandling;
            return this;
        }

        /**
         * Adds a request parameters.
         *
         * @param input Input request expression.
         * @param output Output request expression.
         * @return Returns the builder.
         * @see IntegrationTrait#getRequestParameters()
         */
        public Builder putRequestParameter(String input, String output) {
            requestParameters.put(input, output);
            return this;
        }

        /**
         * Sets request parameters.
         *
         * @param requestParameters Map of parameters to add.
         * @return Returns the builder.
         * @see IntegrationTrait#getRequestParameters()
         */
        public Builder requestParameters(Map<String, String> requestParameters) {
            this.requestParameters.clear();
            this.requestParameters.putAll(requestParameters);
            return this;
        }

        /**
         * Remove a request parameter by expression.
         *
         * @param expression Expression to remove.
         * @return Returns the builder.
         */
        public Builder removeRequestParameter(String expression) {
            requestParameters.remove(expression);
            return this;
        }

        /**
         * Adds a request template.
         *
         * @param mimeType MIME type of the request template to set.
         * @param template Request template to set.
         * @return Returns the builder.
         * @see IntegrationTrait#getRequestTemplates()
         */
        public Builder putRequestTemplate(String mimeType, String template) {
            requestTemplates.put(mimeType, template);
            return this;
        }

        /**
         * Sets request templates.
         *
         * @param requestTemplates Map of MIME types to the corresponding template.
         * @return Returns the builder.
         * @see IntegrationTrait#getRequestTemplates()
         */
        public Builder requestTemplates(Map<String, String> requestTemplates) {
            this.requestTemplates.clear();
            this.requestTemplates.putAll(requestTemplates);
            return this;
        }

        /**
         * Removes a request template by MIME type.
         *
         * @param mimeType MIME type to remove.
         * @return Returns the builder.
         */
        public Builder removeRequestTemplate(String mimeType) {
            requestTemplates.remove(mimeType);
            return this;
        }

        /**
         * Adds a response for the given response regex.
         *
         * @param statusCodeRegex Status code regular expression.
         * @param integrationResponse Integration response to set.
         * @return Returns the builder.
         * @see IntegrationTrait#getResponses()
         */
        public Builder putResponse(String statusCodeRegex, IntegrationResponse integrationResponse) {
            responses.put(statusCodeRegex, integrationResponse);
            return this;
        }

        /**
         * Sets responses for the given response regular expressions.
         *
         * @param responses Map of regular expressions to responses.
         * @return Returns the builder.
         * @see IntegrationTrait#getResponses()
         */
        public Builder responses(Map<String, IntegrationResponse> responses) {
            this.responses.clear();
            this.responses.putAll(responses);
            return this;
        }

        /**
         * Removes a response by status code regex.
         *
         * @param statusCodeRegex Status code regular expression to remove.
         * @return Returns the builder.
         */
        public Builder removeResponse(String statusCodeRegex) {
            responses.remove(statusCodeRegex);
            return this;
        }
    }
}
