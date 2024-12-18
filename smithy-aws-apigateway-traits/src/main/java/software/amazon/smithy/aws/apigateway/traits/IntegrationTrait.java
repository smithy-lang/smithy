/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.apigateway.traits;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodeMapper;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ToShapeId;
import software.amazon.smithy.model.traits.AbstractTrait;
import software.amazon.smithy.model.traits.AbstractTraitBuilder;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * API Gateway integration.
 */
public final class IntegrationTrait extends AbstractTrait implements ToSmithyBuilder<IntegrationTrait> {
    public static final ShapeId ID = ShapeId.from("aws.apigateway#integration");

    private static final String SERVICE_NAME_LABEL = "{serviceName}";
    private static final String OPERATION_NAME_LABEL = "{operationName}";
    private static final String TYPE_KEY = "type";
    private static final String CREDENTIALS_KEY = "credentials";
    private static final String HTTP_METHOD_KEY = "httpMethod";
    private static final String URI_KEY = "uri";

    private final String type;
    private final String uri;
    private final String credentials;
    private final String httpMethod;
    private final String passThroughBehavior;
    private final String contentHandling;
    private final Integer timeoutInMillis;
    private final String connectionId;
    private final String connectionType;
    private final String cacheNamespace;
    private final String payloadFormatVersion;
    private final List<String> cacheKeyParameters;
    private final Map<String, String> requestParameters;
    private final Map<String, String> requestTemplates;
    private final Map<String, IntegrationResponse> responses;

    private IntegrationTrait(Builder builder) {
        super(ID, builder.getSourceLocation());
        type = SmithyBuilder.requiredState(TYPE_KEY, builder.type);
        uri = SmithyBuilder.requiredState(URI_KEY, builder.uri);
        httpMethod = SmithyBuilder.requiredState(HTTP_METHOD_KEY, builder.httpMethod);
        credentials = builder.credentials;
        passThroughBehavior = builder.passThroughBehavior;
        contentHandling = builder.contentHandling;
        timeoutInMillis = builder.timeoutInMillis;
        connectionId = builder.connectionId;
        connectionType = builder.connectionType;
        cacheNamespace = builder.cacheNamespace;
        payloadFormatVersion = builder.payloadFormatVersion;
        cacheKeyParameters = ListUtils.copyOf(builder.cacheKeyParameters);
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
            IntegrationTrait result = new NodeMapper().deserialize(value, IntegrationTrait.class);
            result.setNodeCache(value);
            return result;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Gets the integration type.
     *
     * @return Returns the integration type.
     */
    public String getType() {
        return type;
    }

    /**
     * Gets the Uniform Resource Identifier (URI) of the integration endpoint.
     *
     * @return Returns the set URI of the integration.
     */
    public String getUri() {
        return uri;
    }

    /**
     * Gets the credentials required for the integration, if any.
     *
     * <p>For AWS integrations, three options are available. To specify an
     * IAM Role for API Gateway to assume, use the role's
     * Amazon Resource Name (ARN). To require that the caller's identity
     * be passed through from the request, specify the string
     * {@code arn:aws:iam::*:user/*}. Resource-based permissions are used if
     * no credentials are provided.
     *
     * @return Returns the optionally present credentials.
     */
    public Optional<String> getCredentials() {
        return Optional.ofNullable(credentials);
    }

    /**
     * Gets the integration's HTTP method type.
     *
     * @return Get the set HTTP method.
     */
    public String getHttpMethod() {
        return httpMethod;
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
     * Get the Request payload encoding conversion types.
     *
     * <p>Valid values are:
     *
     * <ul>
     *     <li>CONVERT_TO_TEXT, for converting a binary payload into a
     *     Base64-encoded string or converting a text payload into a
     *     utf-8-encoded string or passing through the text payload natively
     *     without modification</li>
     *     <li>CONVERT_TO_BINARY, for converting a text payload into
     *     Base64-decoded blobor passing through a binary payload natively
     *     without modification.</li>
     * </ul>
     *
     * @return Returns the content-handling.
     */
    public Optional<String> getContentHandling() {
        return Optional.ofNullable(contentHandling);
    }

    /**
     * Get the timeout in milliseconds.
     *
     * <p>Integration timeouts between 50 ms and 29,000 ms. The default
     * setting used by API Gateway is 29,000 (or, 29 seconds).
     *
     * @return Returns the optionally set timeout setting.
     */
    public Optional<Integer> getTimeoutInMillis() {
        return Optional.ofNullable(timeoutInMillis);
    }

    /**
     * Gets the ID of a VpcLink when using a private integration.
     *
     * @return Returns the optionally present connection ID.
     * @see <a href="https://docs.aws.amazon.com/apigateway/api-reference/resource/vpc-link/">VPC Link</a>
     */
    public Optional<String> getConnectionId() {
        return Optional.ofNullable(connectionId);
    }

    /**
     * Gets the connection type used by this integration.
     *
     * @return Returns the connection type.
     */
    public Optional<String> getConnectionType() {
        return Optional.ofNullable(connectionType);
    }

    /**
     * Gets an API-specific tag group of related cached parameters.
     *
     * @return Returns the optionally present cache namespace.
     */
    public Optional<String> getCacheNamespace() {
        return Optional.ofNullable(cacheNamespace);
    }

    /**
     * Gets the payload format version. Only used in HTTP APIs.
     *
     * @return Returns the optional payload format version.
     */
    public Optional<String> getPayloadFormatVersion() {
        return Optional.ofNullable(payloadFormatVersion);
    }

    /**
     * A list of request parameters whose values are to be cached.
     *
     * @return Returns the cache key parameters.
     */
    public List<String> getCacheKeyParameters() {
        return cacheKeyParameters;
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
     * Converts the trait an ObjectNode that finds and replaces the templated
     * serviceName and operationName labels in the "uri" and "credentials"
     * key-value pairs.
     *
     * @param service Service shape ID to use when replacing {@code {serviceName}}.
     * @param operation Operation shape ID to use when replacing {@code {operationName}}.
     * @return Returns the expanded Node.
     */
    public ObjectNode toExpandedNode(ToShapeId service, ToShapeId operation) {
        ObjectNode result = toNode().expectObjectNode();
        result = result.withMember(URI_KEY,
                formatComponent(
                        service,
                        operation,
                        result.expectStringMember(URI_KEY).getValue()));
        if (result.containsMember(CREDENTIALS_KEY)) {
            result = result.withMember(CREDENTIALS_KEY,
                    formatComponent(
                            service,
                            operation,
                            result.expectStringMember(CREDENTIALS_KEY).getValue()));
        }
        return result;
    }

    /**
     * Replaces templated placeholders in an Integration trait.
     *
     * @param service Service shape ID to use when replacing {@code {serviceName}}.
     * @param operation Operation shape ID to use when replacing {@code {operationName}}.
     * @param component Templatized component to expand.
     * @return Returns the expanded string.
     */
    public static String formatComponent(ToShapeId service, ToShapeId operation, String component) {
        return component
                .replace(SERVICE_NAME_LABEL, service.toShapeId().getName())
                .replace(OPERATION_NAME_LABEL, operation.toShapeId().getName());
    }

    @Override
    protected ObjectNode createNode() {
        NodeMapper mapper = new NodeMapper();
        mapper.disableToNodeForClass(IntegrationTrait.class);
        mapper.setOmitEmptyValues(true);
        return mapper.serialize(this).expectObjectNode();
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .sourceLocation(getSourceLocation())
                .type(type)
                .uri(uri)
                .credentials(credentials)
                .httpMethod(httpMethod)
                .passThroughBehavior(passThroughBehavior)
                .contentHandling(contentHandling)
                .timeoutInMillis(timeoutInMillis)
                .connectionId(connectionId)
                .connectionType(connectionType)
                .cacheNamespace(cacheNamespace)
                .payloadFormatVersion(payloadFormatVersion)
                .requestParameters(requestParameters)
                .requestTemplates(requestTemplates)
                .responses(responses)
                .cacheKeyParameters(cacheKeyParameters);
    }

    public static final class Builder extends AbstractTraitBuilder<IntegrationTrait, Builder> {
        private String type;
        private String uri;
        private String credentials;
        private String httpMethod;
        private String passThroughBehavior;
        private String contentHandling;
        private Integer timeoutInMillis;
        private String connectionId;
        private String connectionType;
        private String cacheNamespace;
        private String payloadFormatVersion;
        private final List<String> cacheKeyParameters = new ArrayList<>();
        private final Map<String, String> requestParameters = new HashMap<>();
        private final Map<String, String> requestTemplates = new HashMap<>();
        private final Map<String, IntegrationResponse> responses = new HashMap<>();

        @Override
        public IntegrationTrait build() {
            return new IntegrationTrait(this);
        }

        /**
         * Sets the integration type.
         *
         * @param type Type to set (aws, aws_proxy, http, http_proxy).
         * @return Returns the builder.
         */
        public Builder type(String type) {
            this.type = type;
            return this;
        }

        /**
         * Sets the URI of the integration endpoint.
         *
         * @param uri The URI of the integration.
         * @return Returns the builder.
         */
        public Builder uri(String uri) {
            this.uri = uri;
            return this;
        }

        /**
         * Specifies the credentials used by the integration, if any.
         *
         * @param credentials Credentials to use.
         * @return Returns the builder.
         */
        public Builder credentials(String credentials) {
            this.credentials = credentials;
            return this;
        }

        /**
         * Specifies the integration's HTTP method type.
         *
         * @param httpMethod HTTP method to use.
         * @return Returns the builder.
         */
        public Builder httpMethod(String httpMethod) {
            this.httpMethod = httpMethod;
            return this;
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
         * Set the timeout in milliseconds.
         *
         * <p>Integration timeouts between 50 ms and 29,000 ms. The default
         * setting used by API Gateway is 29,000 (or, 29 seconds).
         *
         * @param timeoutInMillis Timeout in milliseconds.
         * @return Returns the builder.
         */
        public Builder timeoutInMillis(Integer timeoutInMillis) {
            this.timeoutInMillis = timeoutInMillis;
            return this;
        }

        /**
         * Sets the ID of a VpcLink when using a private integration.
         *
         * @param connectionId VPC link ID.
         * @return Returns the builder.
         */
        public Builder connectionId(String connectionId) {
            this.connectionId = connectionId;
            return this;
        }

        /**
         * Sets the connection type.
         *
         * @param connectionType Connection type to set.
         * @return Returns the builder.
         */
        public Builder connectionType(String connectionType) {
            this.connectionType = connectionType;
            return this;
        }

        /**
         * Set an API-specific tag group of related cached parameters.
         *
         * @param cacheNamespace Cache namespace to set.
         * @return Returns the builder.
         */
        public Builder cacheNamespace(String cacheNamespace) {
            this.cacheNamespace = cacheNamespace;
            return this;
        }

        /**
         * Sets the payload format version. Required for HTTP APIs.
         *
         * @param payloadFormatVersion Payload format version to set.
         * @return Returns the builder.
         */
        public Builder payloadFormatVersion(String payloadFormatVersion) {
            this.payloadFormatVersion = payloadFormatVersion;
            return this;
        }

        /**
         * Adds a cache key parameter.
         *
         * @param cacheKeyParameter Parameter to add.
         * @return Returns the builder.
         */
        public Builder addCacheKeyParameter(String cacheKeyParameter) {
            this.cacheKeyParameters.add(cacheKeyParameter);
            return this;
        }

        /**
         * Sets a list of request parameters whose values are to be cached.
         *
         * @param cacheKeyParameters Parameters to use in the cache.
         * @return Returns the builder.
         */
        public Builder cacheKeyParameters(List<String> cacheKeyParameters) {
            this.cacheKeyParameters.clear();
            this.cacheKeyParameters.addAll(cacheKeyParameters);
            return this;
        }

        /**
         * Removes a specific cache key parameter.
         *
         * @param cacheKeyParameter Parameter to remove.
         * @return Returns the builder.
         */
        public Builder removeCacheKeyParameter(String cacheKeyParameter) {
            this.cacheKeyParameters.remove(cacheKeyParameter);
            return this;
        }

        /**
         * Clears all cache key parameters.
         *
         * @return Returns the builder.
         */
        public Builder clearCacheKeyParameters() {
            this.cacheKeyParameters.clear();
            return this;
        }

        /**
         * Adds a request parameter.
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
