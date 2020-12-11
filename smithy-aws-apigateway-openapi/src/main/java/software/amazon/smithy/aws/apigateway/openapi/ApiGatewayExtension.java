/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.openapi.fromsmithy.OpenApiMapper;
import software.amazon.smithy.openapi.fromsmithy.SecuritySchemeConverter;
import software.amazon.smithy.openapi.fromsmithy.Smithy2OpenApiExtension;
import software.amazon.smithy.utils.ListUtils;

public final class ApiGatewayExtension implements Smithy2OpenApiExtension {
    @Override
    public List<OpenApiMapper> getOpenApiMappers() {
        return ListUtils.of(
                ApiGatewayMapper.wrap(new AddDefaultConfigSettings()),
                ApiGatewayMapper.wrap(new AddDefaultRestConfigSettings()),
                ApiGatewayMapper.wrap(new AddApiKeySource()),
                ApiGatewayMapper.wrap(new AddAuthorizers()),
                ApiGatewayMapper.wrap(new AddBinaryTypes()),
                ApiGatewayMapper.wrap(new AddIntegrations()),

                // CORS For REST APIs
                ApiGatewayMapper.wrap(new AddCorsToRestIntegrations()),
                ApiGatewayMapper.wrap(new AddCorsResponseHeaders()),
                ApiGatewayMapper.wrap(new AddCorsPreflightIntegration()),
                ApiGatewayMapper.wrap(new AddCorsToGatewayResponses()),

                ApiGatewayMapper.wrap(new AddRequestValidators()),
                ApiGatewayMapper.wrap(new CloudFormationSubstitution()),

                // HTTP API mappers.
                ApiGatewayMapper.wrap(new CorsHttpIntegration())
        );
    }

    @Override
    public List<SecuritySchemeConverter<? extends Trait>> getSecuritySchemeConverters() {
        return ListUtils.of(
                new CognitoUserPoolsConverter()
        );
    }
}
