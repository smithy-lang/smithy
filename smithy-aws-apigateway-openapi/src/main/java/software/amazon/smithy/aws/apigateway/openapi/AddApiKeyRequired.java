/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.apigateway.openapi;

import java.util.List;
import java.util.logging.Logger;
import software.amazon.smithy.aws.apigateway.traits.ApiKeyRequiredTrait;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.openapi.fromsmithy.Context;
import software.amazon.smithy.openapi.model.OpenApi;
import software.amazon.smithy.openapi.model.OperationObject;
import software.amazon.smithy.openapi.model.SecurityScheme;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.MapUtils;

/**
 * Adds the {@code api_key} security scheme and per-operation security
 * requirements for operations annotated with the
 * {@link ApiKeyRequiredTrait}.
 *
 * @see <a href="https://docs.aws.amazon.com/apigateway/latest/developerguide/api-gateway-api-usage-plans.html">API Gateway usage plans</a>
 */
final class AddApiKeyRequired implements ApiGatewayMapper {

    private static final String SCHEME_NAME = "api_key";
    private static final Logger LOGGER = Logger.getLogger(AddApiKeyRequired.class.getName());

    @Override
    public List<ApiGatewayConfig.ApiType> getApiTypes() {
        return ListUtils.of(ApiGatewayConfig.ApiType.REST);
    }

    @Override
    public OperationObject updateOperation(
            Context<? extends Trait> context,
            OperationShape shape,
            OperationObject operation,
            String httpMethodName,
            String path
    ) {
        if (!shape.hasTrait(ApiKeyRequiredTrait.class)) {
            return operation;
        }

        LOGGER.fine(() -> String.format(
                "Adding api_key security requirement to %s",
                shape.getId()));

        return operation.toBuilder()
                .addSecurity(MapUtils.of(SCHEME_NAME, ListUtils.of()))
                .build();
    }

    @Override
    public OpenApi after(Context<? extends Trait> context, OpenApi openApi) {
        // Only add the api_key security scheme if any operation uses the trait.
        boolean hasApiKeyRequired = context.getModel()
                .getOperationShapes()
                .stream()
                .anyMatch(op -> op.hasTrait(ApiKeyRequiredTrait.class));

        if (!hasApiKeyRequired) {
            return openApi;
        }

        SecurityScheme apiKeyScheme = SecurityScheme.builder()
                .type("apiKey")
                .name("x-api-key")
                .in("header")
                .build();

        return openApi.toBuilder()
                .components(openApi.getComponents()
                        .toBuilder()
                        .putSecurityScheme(SCHEME_NAME, apiKeyScheme)
                        .build())
                .build();
    }
}
