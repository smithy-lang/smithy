package software.amazon.smithy.aws.traits.apigateway;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import software.amazon.smithy.model.SmithyBuilder;
import software.amazon.smithy.model.ToSmithyBuilder;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NumberNode;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.traits.AbstractTrait;
import software.amazon.smithy.model.traits.AbstractTraitBuilder;
import software.amazon.smithy.model.traits.TraitService;

/**
 * API Gateway integration.
 */
public final class IntegrationTrait extends AbstractTrait implements ToSmithyBuilder<IntegrationTrait> {
    public static final String TRAIT = "aws.apigateway#integration";

    private static final String PASS_THROUGH_BEHAVIOR_KEY = "passThroughBehavior";
    private static final String REQUEST_PARAMETERS_KEY = "requestParameters";
    private static final String REQUEST_TEMPLATES_KEY = "requestTemplates";
    private static final String RESPONSES_KEY = "responses";
    private static final String CREDENTIALS_KEY = "credentials";
    private static final String HTTP_METHOD_KEY = "httpMethod";
    private static final String CACHE_KEY_PARAMETERS_KEY = "cacheKeyParameters";
    private static final String CACHE_NAMESPACE_KEY = "cacheNamespace";
    private static final String CONTENT_HANDLING_KEY = "contentHandling";
    private static final String TIMEOUT_KEY = "timeoutInMillis";
    private static final String URI_KEY = "uri";
    private static final String CONNECTION_ID_KEY = "connectionId";
    private static final String CONNECTION_TYPE = "connectionType";
    private static final Set<String> KEYS = Set.of(
            PASS_THROUGH_BEHAVIOR_KEY, REQUEST_PARAMETERS_KEY, REQUEST_TEMPLATES_KEY,
            RESPONSES_KEY, CREDENTIALS_KEY, HTTP_METHOD_KEY, CACHE_KEY_PARAMETERS_KEY,
            CACHE_NAMESPACE_KEY, CONTENT_HANDLING_KEY, TIMEOUT_KEY, URI_KEY,
            CONNECTION_ID_KEY, CONNECTION_TYPE);

    private final String uri;
    private final String credentials;
    private final String httpMethod;
    private final String passThroughBehavior;
    private final String contentHandling;
    private final Integer timeoutInMillis;
    private final String connectionId;
    private final String connectionType;
    private final String cacheNamespace;
    private final List<String> cacheKeyParameters;
    private final Map<String, String> requestParameters;
    private final Map<String, String> requestTemplates;
    private final Map<String, IntegrationResponse> responses;

    private IntegrationTrait(Builder builder) {
        super(TRAIT, builder.getSourceLocation());
        uri = SmithyBuilder.requiredState(URI_KEY, builder.uri);
        httpMethod = SmithyBuilder.requiredState(HTTP_METHOD_KEY, builder.httpMethod);
        credentials = builder.credentials;
        passThroughBehavior = builder.passThroughBehavior;
        contentHandling = builder.contentHandling;
        timeoutInMillis = builder.timeoutInMillis;
        connectionId = builder.connectionId;
        connectionType = builder.connectionType;
        cacheNamespace = builder.cacheNamespace;
        cacheKeyParameters = List.copyOf(builder.cacheKeyParameters);
        requestParameters = Map.copyOf(builder.requestParameters);
        requestTemplates = Map.copyOf(builder.requestTemplates);
        responses = Map.copyOf(builder.responses);
    }

    public static TraitService provider() {
        return TraitService.createProvider(TRAIT, (target, value) -> {
            Builder builder = builder();
            builder.sourceLocation(value);
            ObjectNode node = value.expectObjectNode();
            node.warnIfAdditionalProperties(KEYS);
            builder.uri(node.expectMember(URI_KEY).expectStringNode().getValue());
            builder.httpMethod(node.expectMember(HTTP_METHOD_KEY).expectStringNode().getValue());
            node.getArrayMember(CACHE_KEY_PARAMETERS_KEY)
                    .ifPresent(arrayNode -> arrayNode.getElements().stream()
                            .map(Node::expectStringNode)
                            .map(StringNode::getValue)
                            .forEach(builder::addCacheKeyParameter));
            node.getStringMember(CACHE_NAMESPACE_KEY)
                    .map(StringNode::getValue)
                    .ifPresent(builder::cacheNamespace);
            node.getStringMember(CONTENT_HANDLING_KEY)
                    .map(StringNode::getValue)
                    .ifPresent(builder::contentHandling);
            node.getNumberMember(TIMEOUT_KEY)
                    .map(NumberNode::getValue)
                    .map(Number::intValue)
                    .ifPresent(builder::timeoutInMillis);
            node.getStringMember(CONNECTION_ID_KEY).map(StringNode::getValue).ifPresent(builder::connectionId);
            node.getStringMember(CONNECTION_TYPE).map(StringNode::getValue).ifPresent(builder::connectionType);
            node.getStringMember(CREDENTIALS_KEY).map(StringNode::getValue).ifPresent(builder::credentials);
            node.getStringMember(PASS_THROUGH_BEHAVIOR_KEY)
                    .map(StringNode::getValue)
                    .ifPresent(builder::passThroughBehavior);
            node.getObjectMember(REQUEST_PARAMETERS_KEY)
                    .map(ObjectNode::getMembers)
                    .ifPresent(members -> members.forEach(
                            (k, v) -> builder.putRequestParameter(k.getValue(), v.expectStringNode().getValue())));
            node.getObjectMember(REQUEST_TEMPLATES_KEY)
                    .map(ObjectNode::getMembers)
                    .ifPresent(members -> members.forEach(
                            (k, v) -> builder.putRequestTemplate(k.getValue(), v.expectStringNode().getValue())));
            node.getObjectMember(RESPONSES_KEY)
                    .map(ObjectNode::getMembers)
                    .ifPresent(members -> members.forEach((k, v) -> builder.putResponse(
                            k.getValue(), IntegrationResponse.fromNode(v))));
            return builder.build();
        });
    }

    public static Builder builder() {
        return new Builder();
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
    public Map<String, String> getAllRequestParameters() {
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
    public Map<String, String> getAllRequestTemplates() {
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
    public Map<String, IntegrationResponse> getAllResponses() {
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

    @Override
    protected ObjectNode createNode() {
        return Node.objectNodeBuilder()
                .withMember(URI_KEY, getUri())
                .withMember(HTTP_METHOD_KEY, getHttpMethod())
                .withOptionalMember(CREDENTIALS_KEY, getCredentials().map(Node::from))
                .withOptionalMember(PASS_THROUGH_BEHAVIOR_KEY, getPassThroughBehavior().map(Node::from))
                .withOptionalMember(CONTENT_HANDLING_KEY, getContentHandling().map(Node::from))
                .withOptionalMember(TIMEOUT_KEY, getTimeoutInMillis().map(Node::from))
                .withOptionalMember(CONNECTION_ID_KEY, getConnectionId().map(Node::from))
                .withOptionalMember(CONNECTION_TYPE, getConnectionType().map(Node::from))
                .withOptionalMember(CACHE_NAMESPACE_KEY, getCacheNamespace().map(Node::from))
                .withOptionalMember(CACHE_KEY_PARAMETERS_KEY, cacheKeyParameters.size() > 0
                        ? Optional.of(Node.fromStrings(cacheKeyParameters))
                        : Optional.empty())
                .withOptionalMember(REQUEST_PARAMETERS_KEY, requestParameters.size() > 0
                        ? Optional.of(ObjectNode.fromStringMap(requestParameters))
                        : Optional.empty())
                .withOptionalMember(REQUEST_TEMPLATES_KEY, requestTemplates.size() > 0
                        ? Optional.of(ObjectNode.fromStringMap(requestTemplates))
                        : Optional.empty())
                .withOptionalMember(RESPONSES_KEY, responses.size() > 0
                        ? Optional.of(responses.entrySet().stream().collect(
                                ObjectNode.collectStringKeys(Map.Entry::getKey, entry -> entry.getValue().toNode())))
                        : Optional.empty())
                .build();
    }

    @Override
    public Builder toBuilder() {
        var builder = builder()
                .uri(uri)
                .credentials(credentials)
                .httpMethod(httpMethod)
                .passThroughBehavior(passThroughBehavior)
                .contentHandling(contentHandling)
                .timeoutInMillis(timeoutInMillis)
                .connectionId(connectionId)
                .connectionType(connectionType)
                .cacheNamespace(cacheNamespace);
        cacheKeyParameters.forEach(builder::addCacheKeyParameter);
        requestParameters.forEach(builder::putRequestParameter);
        requestTemplates.forEach(builder::putRequestTemplate);
        responses.forEach(builder::putResponse);
        return builder;
    }

    public static final class Builder extends AbstractTraitBuilder<IntegrationTrait, Builder> {
        private String uri;
        private String credentials;
        private String httpMethod;
        private String passThroughBehavior;
        private String contentHandling;
        private Integer timeoutInMillis;
        private String connectionId;
        private String connectionType;
        private String cacheNamespace;
        private final List<String> cacheKeyParameters = new ArrayList<>();
        private final Map<String, String> requestParameters = new HashMap<>();
        private final Map<String, String> requestTemplates = new HashMap<>();
        private final Map<String, IntegrationResponse> responses = new HashMap<>();

        @Override
        public IntegrationTrait build() {
            return new IntegrationTrait(this);
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
         *     Base64-decoded blobor passing through a binary payload natively
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
         * Adds a request template.
         *
         * @param input Input request expression.
         * @param output Output request expression.
         * @return Returns the builder.
         * @see IntegrationTrait#getAllRequestParameters()
         */
        public Builder putRequestParameter(String input, String output) {
            requestParameters.put(input, output);
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
         * @see IntegrationTrait#getAllRequestTemplates()
         */
        public Builder putRequestTemplate(String mimeType, String template) {
            requestTemplates.put(mimeType, template);
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
         * @see IntegrationTrait#getAllResponses()
         */
        public Builder putResponse(String statusCodeRegex, IntegrationResponse integrationResponse) {
            responses.put(statusCodeRegex, integrationResponse);
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
