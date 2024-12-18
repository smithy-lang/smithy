/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.apigateway.openapi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import software.amazon.smithy.aws.apigateway.traits.IntegrationResponse;
import software.amazon.smithy.aws.apigateway.traits.MockIntegrationTrait;
import software.amazon.smithy.jsonschema.Schema;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.traits.CorsTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.openapi.OpenApiException;
import software.amazon.smithy.openapi.fromsmithy.Context;
import software.amazon.smithy.openapi.fromsmithy.SecuritySchemeConverter;
import software.amazon.smithy.openapi.model.OperationObject;
import software.amazon.smithy.openapi.model.ParameterObject;
import software.amazon.smithy.openapi.model.PathItem;
import software.amazon.smithy.openapi.model.Ref;
import software.amazon.smithy.openapi.model.ResponseObject;
import software.amazon.smithy.utils.CaseUtils;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.SetUtils;

/**
 * Adds CORS-preflight OPTIONS requests using mock API Gateway integrations.
 *
 * <p>This allows clients to first make an OPTIONS request to a URI to
 * determine what possible actions can be made on the URI. Rather than require
 * models to explicitly define and implement these protocol-specific
 * integrations, we can generate a static response using API Gateway mock
 * integrations.
 *
 * <p>TODO: Only add security scheme headers on a per/operation basis.
 * We currently add security headers to the CORS headers that contains all
 * security schemes defined in the model. An improvement here would be to only
 * include headers that are added by security schemes on a per/operation basis.
 *
 * <p>This extension only takes effect if the service being converted to
 * OpenAPI has the CORS trait.
 *
 * @see <a href="https://fetch.spec.whatwg.org/#cors-preflight-fetch-0">CORS-preflight fetch</a>
 */
final class AddCorsPreflightIntegration implements ApiGatewayMapper {

    private static final Logger LOGGER = Logger.getLogger(AddCorsPreflightIntegration.class.getName());
    private static final String API_GATEWAY_DEFAULT_ACCEPT_VALUE = "application/json";
    private static final String INTEGRATION_EXTENSION = "x-amazon-apigateway-integration";
    private static final String REQUEST_TEMPLATES_KEY = "requestTemplates";
    private static final String PREFLIGHT_SUCCESS = "{\"statusCode\":200}";

    @Override
    public List<ApiGatewayConfig.ApiType> getApiTypes() {
        return ListUtils.of(ApiGatewayConfig.ApiType.REST);
    }

    @Override
    public PathItem updatePathItem(Context<? extends Trait> context, String path, PathItem pathItem) {
        return context.getService()
                .getTrait(CorsTrait.class)
                .map(corsTrait -> addPreflightIntegration(context, path, pathItem, corsTrait))
                .orElse(pathItem);
    }

    private static PathItem addPreflightIntegration(
            Context<? extends Trait> context,
            String path,
            PathItem pathItem,
            CorsTrait corsTrait
    ) {
        // Filter out any path for which an OPTIONS handler has already been defined
        if (pathItem.getOptions().isPresent()) {
            LOGGER.fine(() -> path + " already defines an OPTIONS request, so no need to generate CORS-preflight");
            return pathItem;
        }

        LOGGER.fine(() -> "Adding CORS-preflight OPTIONS request and API Gateway integration for " + path);
        Map<CorsHeader, String> headers = deduceCorsHeaders(context, path, pathItem, corsTrait);
        return pathItem.toBuilder()
                .options(createPreflightOperation(context, path, pathItem, headers))
                .build();
    }

    private static <T extends Trait> Map<CorsHeader, String> deduceCorsHeaders(
            Context<T> context,
            String path,
            PathItem pathItem,
            CorsTrait corsTrait
    ) {
        Map<CorsHeader, String> corsHeaders = new HashMap<>();
        corsHeaders.put(CorsHeader.MAX_AGE, String.valueOf(corsTrait.getMaxAge()));
        corsHeaders.put(CorsHeader.ALLOW_ORIGIN, corsTrait.getOrigin());
        corsHeaders.put(CorsHeader.ALLOW_METHODS, getAllowMethods(pathItem));

        if (context.usesHttpCredentials()) {
            corsHeaders.put(CorsHeader.ALLOW_CREDENTIALS, "true");
        }

        // Find each header defined in this PathItem. The collected headers are added to the
        // Access-Control-Allow-Headers header list. Note that any further modifications that
        // add headers during the Smithy to OpenAPI conversion process will need to update this
        // list of headers accordingly.
        Set<String> headerNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        headerNames.addAll(corsTrait.getAdditionalAllowedHeaders());

        // Sets additional allowed headers from the API Gateway config.
        Set<String> additionalAllowedHeaders = context.getConfig()
                .getExtensions(ApiGatewayConfig.class)
                .getAdditionalAllowedCorsHeadersSet();
        headerNames.addAll(additionalAllowedHeaders);
        headerNames.addAll(findAllHeaders(path, pathItem));

        // Add all headers generated by security schemes.
        for (SecuritySchemeConverter<? extends Trait> converter : context.getSecuritySchemeConverters()) {
            headerNames.addAll(getSecuritySchemeRequestHeaders(context, converter));
        }

        // Add protocol headers.
        Model model = context.getModel();
        TopDownIndex topDownIndex = TopDownIndex.of(model);
        Map<String, OperationShape> operations = topDownIndex.getContainedOperations(context.getService())
                .stream()
                .collect(Collectors.toMap(o -> o.getId().getName(), o -> o));
        for (OperationObject operationObject : pathItem.getOperations().values()) {
            if (operationObject.getOperationId().isPresent()) {
                OperationShape operationShape = operations.get(operationObject.getOperationId().get());
                headerNames.addAll(context.getOpenApiProtocol().getProtocolRequestHeaders(context, operationShape));
            }
        }

        LOGGER.fine(() -> String.format(
                "Adding the following %s headers to `%s`: %s",
                CorsHeader.ALLOW_HEADERS,
                path,
                headerNames));
        corsHeaders.put(CorsHeader.ALLOW_HEADERS, String.join(",", headerNames));

        return corsHeaders;
    }

    private static <T extends Trait> Set<String> getSecuritySchemeRequestHeaders(
            Context<? extends Trait> context,
            SecuritySchemeConverter<T> converter
    ) {
        T t = context.getService().expectTrait(converter.getAuthSchemeType());
        return converter.getAuthRequestHeaders(context, t);
    }

    private static Collection<String> findAllHeaders(String path, PathItem pathItem) {
        // Get all "in" = "header" parameters and gather up their "name" properties.
        return pathItem.getOperations()
                .values()
                .stream()
                .flatMap(operationObject -> operationObject.getParameters().stream())
                .filter(parameter -> parameter.getIn().filter(in -> in.equals("header")).isPresent())
                .map(parameter -> parameter.getName()
                        .orElseThrow(() -> new OpenApiException(
                                "OpenAPI header parameter is missing a name in " + path)))
                .collect(Collectors.toList());
    }

    private static String getAllowMethods(PathItem item) {
        return String.join(",", item.getOperations().keySet());
    }

    private static OperationObject createPreflightOperation(
            Context<? extends Trait> context,
            String path,
            PathItem pathItem,
            Map<CorsHeader, String> headers
    ) {
        return OperationObject.builder()
                .tags(ListUtils.of("CORS"))
                .security(Collections.emptyList())
                .description("Handles CORS-preflight requests")
                .operationId(createOperationId(path))
                .putResponse("200", createPreflightResponse(headers))
                .parameters(findPathParameters(pathItem))
                .putExtension(INTEGRATION_EXTENSION, createPreflightIntegration(context, headers, pathItem))
                .build();
    }

    private static List<ParameterObject> findPathParameters(PathItem pathItem) {
        // The first found operation in the path is used for path parameters since they should all be the same.
        List<ParameterObject> parameterObjects = new ArrayList<>();
        Iterator<OperationObject> iter = pathItem.getOperations().values().iterator();
        if (iter.hasNext()) {
            for (ParameterObject parameter : iter.next().getParameters()) {
                if (parameter.getIn().filter(in -> in.equals("path")).isPresent()) {
                    parameterObjects.add(parameter);
                }
            }
        }

        return parameterObjects;
    }

    private static String createOperationId(String path) {
        // Make the operationId all alphanumeric camel case characters.
        return CaseUtils.toCamelCase("Cors" + path, true, '{', '}', '/', '?', '&', '=')
                .replaceAll("[^A-Z0-9a-z_]", "_");
    }

    private static ResponseObject createPreflightResponse(Map<CorsHeader, String> headers) {
        // The preflight response just returns all of the computed CORS headers.
        ResponseObject.Builder builder = ResponseObject.builder()
                .description("Canned response for CORS-preflight requests");
        ParameterObject headerParameter = ParameterObject.builder()
                .schema(Schema.builder().type("string").build())
                .build();
        headers.forEach((name, value) -> builder.putHeader(name.toString(), Ref.local(headerParameter)));
        return builder.build();
    }

    private static ObjectNode createPreflightIntegration(
            Context<? extends Trait> context,
            Map<CorsHeader, String> headers,
            PathItem pathItem
    ) {
        IntegrationResponse.Builder responseBuilder = IntegrationResponse.builder().statusCode("200");

        // Add each CORS header to the mock integration response.
        for (Map.Entry<CorsHeader, String> e : headers.entrySet()) {
            responseBuilder.putResponseParameter("method.response.header." + e.getKey(), "'" + e.getValue() + "'");
        }

        boolean isPreflightSynced = Boolean.TRUE.equals(context.getConfig().getSyncCorsPreflightIntegration());
        MockIntegrationTrait.Builder integration = MockIntegrationTrait.builder()
                // See https://forums.aws.amazon.com/thread.jspa?threadID=256140
                .contentHandling("CONVERT_TO_TEXT")
                // Passthrough behavior "never" will fail the request with unsupported content type more appropriately.
                // https://docs.aws.amazon.com/apigateway/latest/developerguide/integration-passthrough-behaviors.html
                .passThroughBehavior(isPreflightSynced ? "never" : "when_no_match")
                .putResponse("default", responseBuilder.build())
                .putRequestTemplate(API_GATEWAY_DEFAULT_ACCEPT_VALUE, PREFLIGHT_SUCCESS);

        if (isPreflightSynced) {
            // Adds request template for every unique Content-Type supported by all path operations.
            // This ensures that for Content-Type(s) other than 'application/json', the entire request payload
            // is not sent to APIGW mock integration as stipulated by 'when_no_match' passthroughBehavior.
            // APIGW throws an error if the mock integration request does not follow a set contract,
            // example {"statusCode":200}.
            for (OperationObject operation : pathItem.getOperations().values()) {
                ObjectNode extensionNode = operation.getExtension(INTEGRATION_EXTENSION)
                        .flatMap(Node::asObjectNode)
                        .orElse(Node.objectNode());
                Set<String> mimeTypes = extensionNode.getObjectMember(REQUEST_TEMPLATES_KEY)
                        .map(ObjectNode::getStringMap)
                        .map(Map::keySet)
                        .orElse(SetUtils.of());
                mimeTypes.forEach(mimeType -> integration.putRequestTemplate(mimeType, PREFLIGHT_SUCCESS));
            }
        }

        // Add a request template for every mime-type of every response.
        for (OperationObject operation : pathItem.getOperations().values()) {
            for (ResponseObject response : operation.getResponses().values()) {
                for (String mimeType : response.getContent().keySet()) {
                    integration.putRequestTemplate(mimeType, PREFLIGHT_SUCCESS);
                }
            }
        }

        // Use createIntegration to fix the casing of the passThroughBehavior property.
        return AddIntegrations.createIntegration(integration.build());
    }
}
