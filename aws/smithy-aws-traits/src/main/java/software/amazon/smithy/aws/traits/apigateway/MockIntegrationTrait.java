package software.amazon.smithy.aws.traits.apigateway;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import software.amazon.smithy.model.ToSmithyBuilder;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.traits.AbstractTrait;
import software.amazon.smithy.model.traits.AbstractTraitBuilder;
import software.amazon.smithy.model.traits.TraitService;

/**
 * API Gateway mock integration.
 */
public final class MockIntegrationTrait extends AbstractTrait implements ToSmithyBuilder<MockIntegrationTrait> {
    public static final String TRAIT = "aws.apigateway#mockIntegration";
    private static final String PASS_THROUGH_BEHAVIOR_KEY = "passThroughBehavior";
    private static final String REQUEST_PARAMETERS_KEY = "requestParameters";
    private static final String REQUEST_TEMPLATES_KEY = "requestTemplates";
    private static final String RESPONSES_KEY = "responses";
    private static final Set<String> KEYS = Set.of(
            PASS_THROUGH_BEHAVIOR_KEY, REQUEST_PARAMETERS_KEY, REQUEST_TEMPLATES_KEY, RESPONSES_KEY);

    private final String passThroughBehavior;
    private final Map<String, String> requestParameters;
    private final Map<String, String> requestTemplates;
    private final Map<String, IntegrationResponse> responses;

    private MockIntegrationTrait(Builder builder) {
        super(TRAIT, builder.getSourceLocation());
        passThroughBehavior = builder.passThroughBehavior;
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
        return Node.objectNode()
                .withOptionalMember(PASS_THROUGH_BEHAVIOR_KEY, getPassThroughBehavior().map(Node::from))
                .withOptionalMember(REQUEST_PARAMETERS_KEY, requestParameters.size() > 0
                        ? Optional.of(ObjectNode.fromStringMap(requestParameters))
                        : Optional.empty())
                .withOptionalMember(REQUEST_TEMPLATES_KEY, requestTemplates.size() > 0
                        ? Optional.of(ObjectNode.fromStringMap(requestTemplates))
                        : Optional.empty())
                .withOptionalMember(RESPONSES_KEY, responses.size() > 0
                        ? Optional.of(responses.entrySet().stream().collect(
                                ObjectNode.collectStringKeys(Map.Entry::getKey, entry -> entry.getValue().toNode())))
                        : Optional.empty());
    }

    @Override
    public Builder toBuilder() {
        var builder = builder().passThroughBehavior(passThroughBehavior);
        requestParameters.forEach(builder::putRequestParameter);
        requestTemplates.forEach(builder::putRequestTemplate);
        responses.forEach(builder::putResponse);
        return builder;
    }

    public static final class Builder extends AbstractTraitBuilder<MockIntegrationTrait, Builder> {
        private String passThroughBehavior;
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
