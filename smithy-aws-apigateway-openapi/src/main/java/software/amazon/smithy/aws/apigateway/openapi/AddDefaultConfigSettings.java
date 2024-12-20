/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.apigateway.openapi;

import java.util.List;
import java.util.logging.Logger;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.openapi.OpenApiConfig;

/**
 * Sets default config settings for API Gateway.
 *
 * <p>By default, this disables OpenAPI and JSON Schema features not
 * supported by API Gateway.</p>
 *
 * <p>API Gateway does not allow characters like "_". API Gateway
 * doesn't support the "default" trait or `int32` or `int64` "format"
 * values.
 */
final class AddDefaultConfigSettings implements ApiGatewayMapper {
    private static final Logger LOGGER = Logger.getLogger(AddDefaultConfigSettings.class.getName());
    private static final ApiGatewayDefaults DEFAULT_VERSION = ApiGatewayDefaults.VERSION_2023_08_11;

    @Override
    public List<ApiGatewayConfig.ApiType> getApiTypes() {
        return null;
    }

    @Override
    public void updateDefaultSettings(Model model, OpenApiConfig openApiConfig) {
        ApiGatewayConfig config = openApiConfig.getExtensions(ApiGatewayConfig.class);
        if (config.getApiGatewayDefaults() == null) {
            LOGGER.warning(String.format("`apiGatewayDefaults` configuration not set for openapi plugin. Assuming %s.",
                    DEFAULT_VERSION));
            config.setApiGatewayDefaults(DEFAULT_VERSION);
        }
        config.getApiGatewayDefaults().setDefaults(openApiConfig);
    }
}
