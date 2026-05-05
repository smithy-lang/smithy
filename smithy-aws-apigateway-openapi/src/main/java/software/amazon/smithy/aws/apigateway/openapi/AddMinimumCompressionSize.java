/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.apigateway.openapi;

import java.util.List;
import java.util.logging.Logger;
import software.amazon.smithy.aws.apigateway.traits.MinimumCompressionSizeTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.openapi.fromsmithy.Context;
import software.amazon.smithy.openapi.model.OpenApi;
import software.amazon.smithy.utils.ListUtils;

/**
 * Adds the API Gateway x-amazon-apigateway-minimum-compression-size extension
 * to the OpenAPI model when the {@link MinimumCompressionSizeTrait} is applied
 * to a service.
 *
 * @see <a href="https://docs.aws.amazon.com/apigateway/latest/developerguide/api-gateway-openapi-minimum-compression-size.html">Minimum compression size</a>
 */
final class AddMinimumCompressionSize implements ApiGatewayMapper {

    private static final String EXTENSION_NAME = "x-amazon-apigateway-minimum-compression-size";
    private static final Logger LOGGER = Logger.getLogger(AddMinimumCompressionSize.class.getName());

    @Override
    public List<ApiGatewayConfig.ApiType> getApiTypes() {
        return ListUtils.of(ApiGatewayConfig.ApiType.REST);
    }

    @Override
    public OpenApi after(Context<? extends Trait> context, OpenApi openApi) {
        return context.getService()
                .getTrait(MinimumCompressionSizeTrait.class)
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
