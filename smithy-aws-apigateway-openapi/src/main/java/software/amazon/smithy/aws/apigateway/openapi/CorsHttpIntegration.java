/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.apigateway.openapi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.traits.CorsTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.openapi.fromsmithy.Context;
import software.amazon.smithy.openapi.model.OpenApi;
import software.amazon.smithy.openapi.model.OperationObject;
import software.amazon.smithy.openapi.model.ParameterObject;
import software.amazon.smithy.openapi.model.PathItem;
import software.amazon.smithy.openapi.model.Ref;
import software.amazon.smithy.openapi.model.ResponseObject;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.SetUtils;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Adds support for the API Gateway {@code x-amazon-apigateway-cors}
 * extension for API Gateway HTTP APIs using values from the
 * Smithy {@code cors} trait.
 *
 * <ul>
 *     <li>{@code allowOrigins} is populated based on the {@code origin}
 *     property of the {@code cors} trait.</li>
 *     <li>{@code maxAge} is populated based on the {@code maxAge}
 *     property of the {@code cors} trait.</li>
 *     <li>{@code allowMethods} is populated by scanning the generated
 *     OpenAPI definition for every defined method.</li>
 *     <li>{@code exposedHeaders} is set to "*" to expose all headers IFF
 *     the service does not use HTTP credentials, and no value is provided
 *     to the {@code additionalExposedHeaders} property of the Smithy
 *     {@code cors} trait. Otherwise, this value is populated by finding
 *     all of the response headers used by the protocol, modeled in the
 *     service, and used by auth schemes.</li>
 *     <li>{@code allowedHeaders} is set to "*" to allow all headers IFF
 *     the service does not use HTTP credentials, and no value is provided
 *     to the {@code additionalAllowedHeaders} property of the Smithy
 *     {@code cors} trait. Otherwise, this value is populated by finding
 *     all of the request headers used by the protocol, modeled in the
 *     service, and used by auth schemes.</li>
 *     <li>{@code allowCredentials} is set to true if any of the
 *     auth schemes used in the API use HTTP credentials according
 *     to {@link Context#usesHttpCredentials()}.</li>
 * </ul>
 *
 * @see <a href="https://docs.aws.amazon.com/apigateway/latest/developerguide/api-gateway-swagger-extensions-cors-configuration.html">API Gateway documentation</a>
 */
@SmithyInternalApi
public final class CorsHttpIntegration implements ApiGatewayMapper {

    private static final Logger LOGGER = Logger.getLogger(CorsHttpIntegration.class.getName());
    private static final String CORS_HTTP_EXTENSION = "x-amazon-apigateway-cors";

    @Override
    public List<ApiGatewayConfig.ApiType> getApiTypes() {
        return ListUtils.of(ApiGatewayConfig.ApiType.HTTP);
    }

    @Override
    public OpenApi after(Context<? extends Trait> context, OpenApi openapi) {
        return context.getService()
                .getTrait(CorsTrait.class)
                .map(corsTrait -> addCors(context, openapi, corsTrait))
                .orElse(openapi);
    }

    private OpenApi addCors(Context<? extends Trait> context, OpenApi openapi, CorsTrait trait) {
        // Use any existing x-amazon-apigateway-cors value, if present.
        Node alreadySetCorsValue = openapi.getExtension(CORS_HTTP_EXTENSION)
                .flatMap(Node::asObjectNode)
                .orElse(null);

        if (alreadySetCorsValue != null) {
            return openapi;
        }

        Set<String> allowedMethodsInService = getMethodsUsedInApi(context, openapi);
        Set<String> allowedRequestHeaders = getAllowedHeaders(context, trait, openapi);
        Set<String> exposedHeaders = getExposedHeaders(context, trait, openapi);

        ObjectNode.Builder corsObjectBuilder = Node.objectNodeBuilder()
                .withMember("allowOrigins", Node.fromStrings(trait.getOrigin()))
                .withMember("maxAge", trait.getMaxAge())
                .withMember("allowMethods", Node.fromStrings(allowedMethodsInService))
                .withMember("exposeHeaders", Node.fromStrings(exposedHeaders))
                .withMember("allowHeaders", Node.fromStrings(allowedRequestHeaders));

        if (context.usesHttpCredentials()) {
            corsObjectBuilder.withMember("allowCredentials", true);
        }

        return openapi.toBuilder()
                .putExtension(CORS_HTTP_EXTENSION, corsObjectBuilder.build())
                .build();
    }

    private <T extends Trait> Set<String> getMethodsUsedInApi(Context<T> context, OpenApi openApi) {
        Set<String> methods = new TreeSet<>();

        if (!context.usesHttpCredentials()) {
            LOGGER.info("Using * for Access-Control-Allow-Methods because the service does not use HTTP credentials");
            return SetUtils.of("*");
        }

        LOGGER.info("Generating a value for Access-Control-Allow-Methods because the service uses HTTP credentials");
        for (PathItem pathItem : openApi.getPaths().values()) {
            for (String method : pathItem.getOperations().keySet()) {
                // No need to call out OPTIONS as supported.
                if (!method.equalsIgnoreCase("OPTIONS")) {
                    methods.add(method.toUpperCase(Locale.ENGLISH));
                }
            }
        }

        return methods;
    }

    private <T extends Trait> Set<String> getAllowedHeaders(Context<T> context, CorsTrait corsTrait, OpenApi openApi) {
        Set<String> headers = new TreeSet<>(corsTrait.getAdditionalAllowedHeaders());

        // If no additionalAllowedHeaders are set on the trait and the
        // service doesn't use HTTP credentials, then the simplest way
        // to ensure that every header is allowed is using "*", which
        // allows all headers. This can't be used when HTTP credentials are
        // used since "*" then becomes a literal "*".
        if (headers.isEmpty() && !context.usesHttpCredentials()) {
            LOGGER.info("Using * for Access-Control-Allow-Headers because the service does not use HTTP credentials");
            return SetUtils.of("*");
        }

        LOGGER.info("Generating a value for Access-Control-Allow-Headers because the service uses HTTP credentials");

        // Note: not all headers used in a service are defined in the OpenAPI model.
        // That's generally true for any service, but that assumption is made here
        // too because security scheme and protocol headers are not defined on operations.

        // Allow request headers needed by security schemes.
        headers.addAll(context.getAllSecuritySchemeRequestHeaders());

        // Allow any protocol-specific request headers for each operation.
        TopDownIndex topDownIndex = TopDownIndex.of(context.getModel());
        for (OperationShape operation : topDownIndex.getContainedOperations(context.getService())) {
            headers.addAll(context.getOpenApiProtocol().getProtocolRequestHeaders(context, operation));
        }

        // Allow all of the headers that were added to the generated OpenAPI definition.
        for (PathItem item : openApi.getPaths().values()) {
            headers.addAll(getHeadersFromParameterRefs(openApi, item.getParameters()));
            for (OperationObject operationObject : item.getOperations().values()) {
                headers.addAll(getHeadersFromParameters(operationObject.getParameters()));
            }
        }

        return headers;
    }

    private <T extends Trait> Set<String> getExposedHeaders(Context<T> context, CorsTrait corsTrait, OpenApi openApi) {
        Set<String> headers = new TreeSet<>(corsTrait.getAdditionalExposedHeaders());

        // If not additionalExposedHeaders are set on the trait and the
        // service doesn't use HTTP credentials, then the simplest way
        // to ensure that every header is exposed is using "*", which
        // exposes all headers. This can't be used when HTTP credentials are
        // used since "*" then becomes a literal "*".
        if (headers.isEmpty() && !context.usesHttpCredentials()) {
            LOGGER.info("Using * for Access-Control-Expose-Headers because the service does not use HTTP credentials");
            return SetUtils.of("*");
        }

        LOGGER.info("Generating a value for Access-Control-Expose-Headers because the service uses HTTP credentials");

        // Note: not all headers used in a service are defined in the OpenAPI model.
        // That's generally true for any service, but that assumption is made here
        // too because security scheme and protocol headers are not defined on operations.

        // Expose response headers populated by security schemes.
        headers.addAll(context.getAllSecuritySchemeResponseHeaders());

        // Expose any protocol-specific response headers for each operation.
        TopDownIndex topDownIndex = TopDownIndex.of(context.getModel());
        for (OperationShape operation : topDownIndex.getContainedOperations(context.getService())) {
            headers.addAll(context.getOpenApiProtocol().getProtocolResponseHeaders(context, operation));
        }

        // Expose all of the headers that were added to the generated OpenAPI definition.
        for (PathItem item : openApi.getPaths().values()) {
            for (OperationObject operationObject : item.getOperations().values()) {
                for (ResponseObject responseObject : operationObject.getResponses().values()) {
                    headers.addAll(responseObject.getHeaders().keySet());
                }
            }
        }

        return headers;
    }

    private Set<String> getHeadersFromParameterRefs(OpenApi openApi, Collection<Ref<ParameterObject>> params) {
        Collection<ParameterObject> resolved = new ArrayList<>();
        for (Ref<ParameterObject> ref : params) {
            resolved.add(ref.deref(openApi.getComponents()));
        }
        return getHeadersFromParameters(resolved);
    }

    private Set<String> getHeadersFromParameters(Collection<ParameterObject> params) {
        Set<String> result = new TreeSet<>();
        for (ParameterObject param : params) {
            if (param.getIn().filter(in -> in.equals("header")).isPresent()) {
                param.getName().ifPresent(result::add);
            }
        }
        return result;
    }
}
