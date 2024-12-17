/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
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
                ApiGatewayMapper.wrap(new CorsHttpIntegration()));
    }

    @Override
    public List<SecuritySchemeConverter<? extends Trait>> getSecuritySchemeConverters() {
        return ListUtils.of(
                new CognitoUserPoolsConverter());
    }
}
