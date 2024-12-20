/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.apigateway.openapi;

import java.util.List;
import java.util.logging.Logger;
import software.amazon.smithy.aws.apigateway.traits.ApiKeySourceTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.openapi.fromsmithy.Context;
import software.amazon.smithy.openapi.model.OpenApi;
import software.amazon.smithy.utils.ListUtils;

final class AddApiKeySource implements ApiGatewayMapper {

    private static final String EXTENSION_NAME = "x-amazon-apigateway-api-key-source";
    private static final Logger LOGGER = Logger.getLogger(AddApiKeySource.class.getName());

    @Override
    public List<ApiGatewayConfig.ApiType> getApiTypes() {
        return ListUtils.of(ApiGatewayConfig.ApiType.REST, ApiGatewayConfig.ApiType.HTTP);
    }

    @Override
    public OpenApi after(Context<? extends Trait> context, OpenApi openApi) {
        return context.getService()
                .getTrait(ApiKeySourceTrait.class)
                .map(trait -> {
                    LOGGER.fine(() -> String.format(
                            "Adding %s trait to %s",
                            EXTENSION_NAME,
                            context.getService().getId()));
                    return openApi.toBuilder().putExtension(EXTENSION_NAME, trait.getValue()).build();
                })
                .orElse(openApi);
    }
}
