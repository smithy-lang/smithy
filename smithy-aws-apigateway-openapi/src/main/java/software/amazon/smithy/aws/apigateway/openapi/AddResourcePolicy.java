/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.apigateway.openapi;

import java.util.List;
import java.util.logging.Logger;
import software.amazon.smithy.aws.apigateway.traits.ResourcePolicyTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.openapi.fromsmithy.Context;
import software.amazon.smithy.openapi.model.OpenApi;
import software.amazon.smithy.utils.ListUtils;

/**
 * Adds the API Gateway x-amazon-apigateway-policy extension to the OpenAPI
 * model when the {@link ResourcePolicyTrait} is applied to a service.
 *
 * @see <a href="https://docs.aws.amazon.com/apigateway/latest/developerguide/openapi-extensions-policy.html">x-amazon-apigateway-policy</a>
 */
final class AddResourcePolicy implements ApiGatewayMapper {

    private static final String EXTENSION_NAME = "x-amazon-apigateway-policy";
    private static final Logger LOGGER = Logger.getLogger(AddResourcePolicy.class.getName());

    @Override
    public List<ApiGatewayConfig.ApiType> getApiTypes() {
        return ListUtils.of(ApiGatewayConfig.ApiType.REST);
    }

    @Override
    public OpenApi after(Context<? extends Trait> context, OpenApi openApi) {
        return context.getService()
                .getTrait(ResourcePolicyTrait.class)
                .map(trait -> {
                    LOGGER.fine(() -> String.format(
                            "Adding %s to %s",
                            EXTENSION_NAME,
                            context.getService().getId()));
                    return openApi.toBuilder().putExtension(EXTENSION_NAME, trait.getValue()).build();
                })
                .orElse(openApi);
    }
}
