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
import software.amazon.smithy.utils.ListUtils;

/**
 * REST APIs require that name parameters for greedy labels are not suffixed with "+".
 *  @see <a href="https://docs.aws.amazon.com/apigateway/latest/developerguide/setup-http-integrations.html">REST APIs</a>
 *  which is counter to the default, like for
 *  <a href="https://docs.aws.amazon.com/apigateway/latest/developerguide/http-api-develop-integrations-http.html">HTTP APIs</a>.
 */
final class AddDefaultRestConfigSettings implements ApiGatewayMapper {
    @Override
    public List<ApiGatewayConfig.ApiType> getApiTypes() {
        return ListUtils.of(ApiGatewayConfig.ApiType.REST);
    }

    @Override
    public void updateDefaultSettings(Model model, OpenApiConfig config) {
        config.setRemoveGreedyParameterSuffix(true);
    }
}
