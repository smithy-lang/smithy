/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.apigateway.openapi;

import java.util.List;
import java.util.logging.Logger;
import software.amazon.smithy.aws.apigateway.traits.ApiTlsPolicyTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.openapi.fromsmithy.Context;
import software.amazon.smithy.openapi.model.OpenApi;
import software.amazon.smithy.utils.ListUtils;

/**
 * Adds the {@code x-amazon-apigateway-security-policy} and optionally
 * {@code x-amazon-apigateway-endpoint-access-mode} extensions to the OpenAPI
 * model when the {@link ApiTlsPolicyTrait} is applied to a service.
 *
 * @see <a href="https://docs.aws.amazon.com/apigateway/latest/developerguide/apigateway-security-policies.html">Security policies for REST APIs</a>
 */
final class AddApiTlsPolicy implements ApiGatewayMapper {

    private static final String SECURITY_POLICY_EXTENSION = "x-amazon-apigateway-security-policy";
    private static final String ENDPOINT_ACCESS_MODE_EXTENSION = "x-amazon-apigateway-endpoint-access-mode";
    private static final Logger LOGGER = Logger.getLogger(AddApiTlsPolicy.class.getName());

    @Override
    public List<ApiGatewayConfig.ApiType> getApiTypes() {
        return ListUtils.of(ApiGatewayConfig.ApiType.REST);
    }

    @Override
    public OpenApi after(Context<? extends Trait> context, OpenApi openApi) {
        return context.getService()
                .getTrait(ApiTlsPolicyTrait.class)
                .map(trait -> {
                    LOGGER.fine(() -> String.format(
                            "Adding TLS policy extensions to %s",
                            context.getService().getId()));

                    OpenApi.Builder builder = openApi.toBuilder()
                            .putExtension(SECURITY_POLICY_EXTENSION, trait.getSecurityPolicy());

                    trait.getEndpointAccessMode()
                            .ifPresent(
                                    mode -> builder.putExtension(ENDPOINT_ACCESS_MODE_EXTENSION, mode));

                    return builder.build();
                })
                .orElse(openApi);
    }
}
