/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.apigateway.openapi;

import java.util.List;
import java.util.logging.Logger;
import software.amazon.smithy.aws.apigateway.traits.GatewayResponsesTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.openapi.fromsmithy.Context;
import software.amazon.smithy.openapi.model.OpenApi;
import software.amazon.smithy.utils.ListUtils;

/**
 * Adds the {@code x-amazon-apigateway-gateway-responses} extension to the
 * OpenAPI model when the {@link GatewayResponsesTrait} is applied to a
 * service.
 *
 * <p>This mapper runs before {@link AddCorsToGatewayResponses} so that
 * customer-defined gateway responses take precedence over CORS-generated
 * headers. The CORS mapper merges its headers without overwriting existing
 * response parameters.
 *
 * @see <a href="https://docs.aws.amazon.com/apigateway/latest/developerguide/api-gateway-swagger-extensions-gateway-responses.html">x-amazon-apigateway-gateway-responses</a>
 */
final class AddGatewayResponses implements ApiGatewayMapper {

    private static final String EXTENSION_NAME = "x-amazon-apigateway-gateway-responses";
    private static final Logger LOGGER = Logger.getLogger(AddGatewayResponses.class.getName());

    @Override
    public List<ApiGatewayConfig.ApiType> getApiTypes() {
        return ListUtils.of(ApiGatewayConfig.ApiType.REST);
    }

    @Override
    public OpenApi after(Context<? extends Trait> context, OpenApi openApi) {
        return context.getService()
                .getTrait(GatewayResponsesTrait.class)
                .map(trait -> {
                    LOGGER.fine(() -> String.format(
                            "Adding %s to %s",
                            EXTENSION_NAME,
                            context.getService().getId()));
                    return openApi.toBuilder()
                            .putExtension(EXTENSION_NAME, trait.getValue())
                            .build();
                })
                .orElse(openApi);
    }
}
