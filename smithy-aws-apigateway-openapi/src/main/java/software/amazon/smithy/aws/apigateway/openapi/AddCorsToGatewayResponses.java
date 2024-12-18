/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.apigateway.openapi;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.traits.CorsTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.openapi.fromsmithy.Context;
import software.amazon.smithy.openapi.model.OpenApi;
import software.amazon.smithy.utils.ListUtils;

/**
 * Adds static CORS response headers to API Gateway "gateway" responses.
 *
 * If no gateway responses are defined in the OpenAPI model, a default set of
 * responses (containing a <code>DEFAULT_4XX</code> and <code>DEFAULT_5XX</code>
 * response for authentication and integration errors) is added with the
 * appropriate CORS headers.
 *
 * <p>This extension only takes effect if the service being converted to
 * OpenAPI has the CORS trait.
 *
 * @see <a href="https://docs.aws.amazon.com/apigateway/latest/developerguide/api-gateway-swagger-extensions-gateway-responses.html">x-amazon-apigateway-gateway-responses Object</a>
 */
final class AddCorsToGatewayResponses implements ApiGatewayMapper {

    private static final Logger LOGGER = Logger.getLogger(AddCorsToGatewayResponses.class.getName());

    /**
     * The default gateway response for 4xx and 5xx.
     *
     * TODO: Does this need to be made protocol-specific?
     */
    private static final ObjectNode DEFAULT_GATEWAY_RESPONSE = Node.objectNode()
            .withMember("responseTemplates",
                    Node.objectNode()
                            .withMember("application/json", "{\"message\":$context.error.messageString}"));

    private static final ObjectNode DEFAULT_GATEWAY_RESPONSES = Node.objectNodeBuilder()
            .withMember("DEFAULT_4XX", DEFAULT_GATEWAY_RESPONSE)
            .withMember("DEFAULT_5XX", DEFAULT_GATEWAY_RESPONSE)
            .build();

    private static final String GATEWAY_RESPONSES_EXTENSION = "x-amazon-apigateway-gateway-responses";
    private static final String HEADER_PREFIX = "gatewayresponse.header.";
    private static final String RESPONSE_PARAMETERS_KEY = "responseParameters";

    @Override
    public List<ApiGatewayConfig.ApiType> getApiTypes() {
        return ListUtils.of(ApiGatewayConfig.ApiType.REST);
    }

    @Override
    public OpenApi after(Context<? extends Trait> context, OpenApi openapi) {
        return context.getService()
                .getTrait(CorsTrait.class)
                .map(corsTrait -> updateModel(context, openapi, corsTrait))
                .orElse(openapi);
    }

    private OpenApi updateModel(Context<? extends Trait> context, OpenApi openapi, CorsTrait corsTrait) {
        // Update the existing gateway responses if present, or inject a default one if not.
        Node extension = openapi.getExtension(GATEWAY_RESPONSES_EXTENSION)
                .map(node -> node.expectObjectNode(GATEWAY_RESPONSES_EXTENSION + " must be an object"))
                .map(node -> updateGatewayResponses(context, corsTrait, node))
                .orElse(updateGatewayResponses(context, corsTrait));
        // Add the gateway responses the `x-amazon-apigateway-gateway-responses` OpenAPI extension.
        return openapi.toBuilder()
                .putExtension(GATEWAY_RESPONSES_EXTENSION, extension)
                .build();
    }

    private Node updateGatewayResponses(Context<? extends Trait> context, CorsTrait trait) {
        LOGGER.fine(() -> "Injecting default API Gateway responses for " + context.getService().getId());
        return updateGatewayResponses(context, trait, DEFAULT_GATEWAY_RESPONSES);
    }

    private Node updateGatewayResponses(
            Context<? extends Trait> context,
            CorsTrait trait,
            ObjectNode gatewayResponses
    ) {
        Map<CorsHeader, String> corsHeaders = new HashMap<>();
        corsHeaders.put(CorsHeader.ALLOW_ORIGIN, trait.getOrigin());

        // Include an 'Access-Control-Allow-Credentials' header if any security scheme requires
        // browser-managed credentials.
        if (context.usesHttpCredentials()) {
            corsHeaders.put(CorsHeader.ALLOW_CREDENTIALS, "true");
        }

        return gatewayResponses.getMembers()
                .entrySet()
                .stream()
                .collect(ObjectNode.collect(Map.Entry::getKey, entry -> {
                    return updateGatewayResponse(context, trait, corsHeaders, entry.getValue().expectObjectNode());
                }));
    }

    private ObjectNode updateGatewayResponse(
            Context<? extends Trait> context,
            CorsTrait trait,
            Map<CorsHeader, String> sharedHeaders,
            ObjectNode gatewayResponse
    ) {
        ObjectNode responseParameters = gatewayResponse
                .getObjectMember(RESPONSE_PARAMETERS_KEY)
                .orElse(Node.objectNode());

        // Track all CORS headers of the gateway response.
        Map<CorsHeader, String> headers = new TreeMap<>(sharedHeaders);

        // Add the modeled additional headers. These could potentially be added by an
        // apigateway feature, so they need to be present.
        Set<String> exposedHeaders = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        exposedHeaders.addAll(trait.getAdditionalExposedHeaders());

        // Find all headers exposed already in the response. These need to be added to the
        // Access-Control-Expose-Headers header if any are found.
        for (String key : responseParameters.getStringMap().keySet()) {
            if (key.startsWith(HEADER_PREFIX)) {
                exposedHeaders.add(key.substring(HEADER_PREFIX.length()));
            }
        }

        if (!exposedHeaders.isEmpty()) {
            headers.put(CorsHeader.EXPOSE_HEADERS, String.join(",", exposedHeaders));
        }

        // Merge the defined response parameters with the derived CORS headers.
        ObjectNode.Builder responseBuilder = responseParameters.toBuilder();
        for (Map.Entry<CorsHeader, String> entry : headers.entrySet()) {
            String key = HEADER_PREFIX + entry.getKey().toString();
            if (!responseParameters.getMember(key).isPresent()) {
                responseBuilder.withMember(key, "'" + entry.getValue() + "'");
            }
        }

        return gatewayResponse.withMember(RESPONSE_PARAMETERS_KEY, responseBuilder.build());
    }
}
