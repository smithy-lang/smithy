/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.apigateway.openapi;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;
import software.amazon.smithy.aws.apigateway.traits.AuthorizerDefinition;
import software.amazon.smithy.aws.apigateway.traits.AuthorizerIndex;
import software.amazon.smithy.aws.apigateway.traits.AuthorizerTrait;
import software.amazon.smithy.aws.apigateway.traits.AuthorizersTrait;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.HttpApiKeyAuthTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.openapi.fromsmithy.Context;
import software.amazon.smithy.openapi.fromsmithy.SecuritySchemeConverter;
import software.amazon.smithy.openapi.fromsmithy.mappers.RemoveUnusedComponents;
import software.amazon.smithy.openapi.model.ComponentsObject;
import software.amazon.smithy.openapi.model.OpenApi;
import software.amazon.smithy.openapi.model.OperationObject;
import software.amazon.smithy.openapi.model.SecurityScheme;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.MapUtils;

/**
 * Dynamically creates security schemes for API Gateway authorizers based on
 * the {@link AuthorizerTrait}.
 *
 * <p>The effective authorizer of a service and each operation is calculated,
 * and if resolves to a value, the components are updated to remove the
 * scheme that the authorizer augments. API Gateway authorizers create a copy
 * of the scheme that they augment and apply API Gateway extensions. This
 * allows for a single Smithy authentication scheme to be referenced by
 * multiple authorizers.
 *
 * <p>This mapper quite possibly could cause security scheme definitions on a
 * service to become unreferenced by any operation. These security schemes
 * should be picked up and removed by the {@link RemoveUnusedComponents}
 * mapper.
 */
final class AddAuthorizers implements ApiGatewayMapper {

    private static final String EXTENSION_NAME = "x-amazon-apigateway-authorizer";
    private static final String CLIENT_EXTENSION_NAME = "x-amazon-apigateway-authtype";
    private static final String DEFAULT_AUTH_TYPE = "custom";
    private static final Logger LOGGER = Logger.getLogger(AddApiKeySource.class.getName());

    @Override
    public List<ApiGatewayConfig.ApiType> getApiTypes() {
        return ListUtils.of(ApiGatewayConfig.ApiType.REST, ApiGatewayConfig.ApiType.HTTP);
    }

    @Override
    public Map<String, List<String>> updateSecurity(
            Context<? extends Trait> context,
            Shape shape,
            SecuritySchemeConverter<? extends Trait> converter,
            Map<String, List<String>> requirement
    ) {
        // Only modify requirements that exactly match the updated scheme.
        if (requirement.size() != 1
                || !requirement.keySet().iterator().next().equals(converter.getOpenApiAuthSchemeName())) {
            return requirement;
        }

        ServiceShape service = context.getService();
        AuthorizerIndex authorizerIndex = AuthorizerIndex.of(context.getModel());

        return authorizerIndex.getAuthorizer(service, shape)
                // Remove the original scheme authentication scheme from the operation if found.
                // Add a new scheme for this operation using the authorizer name.
                .map(authorizer -> MapUtils.of(authorizer, requirement.values().iterator().next()))
                .orElse(requirement);
    }

    @Override
    public OperationObject updateOperation(
            Context<? extends Trait> context,
            OperationShape shape,
            OperationObject operation,
            String httpMethodName,
            String path
    ) {
        ServiceShape service = context.getService();
        AuthorizerIndex authorizerIndex = AuthorizerIndex.of(context.getModel());

        // Get the resolved security schemes of the service and operation, and
        // only add security if it's different than the service or...
        String serviceAuth = authorizerIndex.getAuthorizer(service).orElse(null);
        String operationAuth = authorizerIndex.getAuthorizer(service, shape).orElse(null);

        // Short circuit if we have no authorizer for the operation.
        if (operationAuth == null) {
            return operation;
        }

        // ...API Gateway's built-in API keys are being used. It requires the
        // security to be specified on every operation.
        // See https://docs.aws.amazon.com/apigateway/latest/developerguide/api-gateway-setup-api-key-with-console.html#api-gateway-usage-plan-configure-apikey-on-method
        if (Objects.equals(operationAuth, serviceAuth) && !usesApiGatewayApiKeys(service, operationAuth)) {
            return operation;
        }

        return operation.toBuilder()
                .addSecurity(MapUtils.of(operationAuth, ListUtils.of()))
                .build();
    }

    private boolean usesApiGatewayApiKeys(ServiceShape service, String operationAuth) {
        // Get the authorizer for this operation if it has no "type" or
        // "customAuthType" set, as is required for API Gateway's API keys.
        Optional<AuthorizerDefinition> definitionOptional = service.getTrait(AuthorizersTrait.class)
                .flatMap(authorizers -> authorizers.getAuthorizer(operationAuth)
                        .filter(authorizer -> !authorizer.getType().isPresent()
                                && !authorizer.getCustomAuthType().isPresent()));

        if (!definitionOptional.isPresent()) {
            return false;
        }
        AuthorizerDefinition definition = definitionOptional.get();

        // We then need to validate that the @httpApiKeyAuth trait has been set
        // to authenticate the operation, declaring it's a built-in scheme.
        return definition.getScheme().equals(HttpApiKeyAuthTrait.ID);
    }

    @Override
    public OpenApi after(Context<? extends Trait> context, OpenApi openapi) {
        return context.getService()
                .getTrait(AuthorizersTrait.class)
                .map(authorizers -> addComputedAuthorizers(context, openapi, authorizers))
                .orElse(openapi);
    }

    private OpenApi addComputedAuthorizers(
            Context<? extends Trait> context,
            OpenApi openApi,
            AuthorizersTrait trait
    ) {
        OpenApi.Builder builder = openApi.toBuilder();
        ComponentsObject.Builder components = openApi.getComponents().toBuilder();

        for (Map.Entry<String, AuthorizerDefinition> entry : trait.getAuthorizers().entrySet()) {
            String authorizerName = entry.getKey();
            AuthorizerDefinition authorizer = entry.getValue();
            ShapeId scheme = entry.getValue().getScheme();

            for (SecuritySchemeConverter<? extends Trait> converter : context.getSecuritySchemeConverters()) {
                if (isAuthConverterMatched(context, converter, scheme)) {
                    SecurityScheme updatedScheme = convertAuthScheme(context, converter, authorizer, authorizerName);
                    components.putSecurityScheme(authorizerName, updatedScheme);
                    break;
                }
            }
        }

        builder.components(components.build());
        return builder.build();
    }

    private boolean isAuthConverterMatched(
            Context<? extends Trait> context,
            SecuritySchemeConverter<? extends Trait> converter,
            ShapeId scheme
    ) {
        return converter.getAuthSchemeId().equals(scheme)
                && context.getService().hasTrait(converter.getAuthSchemeType());
    }

    private <T extends Trait> SecurityScheme convertAuthScheme(
            Context<? extends Trait> context,
            SecuritySchemeConverter<T> converter,
            AuthorizerDefinition authorizer,
            String authorizerName
    ) {
        T authTrait = context.getService().expectTrait(converter.getAuthSchemeType());
        SecurityScheme createdScheme = converter.createSecurityScheme(context, authTrait);
        SecurityScheme.Builder schemeBuilder = createdScheme.toBuilder();

        // Do not default the client extension if there is no "type" property
        // set on the authorizer definition. This allows setting the
        // "customAuthType" property without setting the "type".
        //
        // This is necessary to enable various API Gateway authentication
        // schemes and usage plans.
        Optional<String> authTypeOptional = authorizer.getCustomAuthType();
        if (authorizer.getType().isPresent() || authTypeOptional.isPresent()) {
            String authType = authTypeOptional.orElse(DEFAULT_AUTH_TYPE);
            schemeBuilder.putExtension(CLIENT_EXTENSION_NAME, authType);
        }

        ObjectNode authorizerNode = Node.objectNodeBuilder()
                .withOptionalMember("type", authorizer.getType().map(Node::from))
                .withOptionalMember("authorizerUri", authorizer.getUri().map(Node::from))
                .withOptionalMember("authorizerCredentials", authorizer.getCredentials().map(Node::from))
                .withOptionalMember("identityValidationExpression",
                        authorizer.getIdentityValidationExpression().map(Node::from))
                .withOptionalMember("identitySource", authorizer.getIdentitySource().map(Node::from))
                .withOptionalMember("authorizerResultTtlInSeconds",
                        authorizer.getResultTtlInSeconds().map(Node::from))
                .withOptionalMember("authorizerPayloadFormatVersion",
                        authorizer.getAuthorizerPayloadFormatVersion().map(Node::from))
                .withOptionalMember("enableSimpleResponses",
                        authorizer.getEnableSimpleResponses().map(Node::from))
                .build();
        if (authorizerNode.size() != 0) {
            schemeBuilder.putExtension(EXTENSION_NAME, authorizerNode);
        }

        LOGGER.fine(() -> String.format("Adding the `%s` OpenAPI security scheme", authorizerName));
        return schemeBuilder.build();
    }
}
