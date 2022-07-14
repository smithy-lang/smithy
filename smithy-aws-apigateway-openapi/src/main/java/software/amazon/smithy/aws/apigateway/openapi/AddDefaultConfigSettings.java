/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.aws.apigateway.openapi;

import java.util.List;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.openapi.OpenApiConfig;

/**
 * Disables OpenAPI and JSON Schema features not supported by API Gateway.
 *
 * <p>API Gateway does not allow characters like "_". API Gateway
 * doesn't support the "default" trait.
 */
final class AddDefaultConfigSettings implements ApiGatewayMapper {
    @Override
    public List<ApiGatewayConfig.ApiType> getApiTypes() {
        return null;
    }

    @Override
    public void updateDefaultSettings(Model model, OpenApiConfig config) {
        config.setAlphanumericOnlyRefs(true);
        config.getDisableFeatures().add("default");
    }
}
