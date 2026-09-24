/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.apigateway.openapi;

import java.util.List;
import java.util.logging.Logger;
import software.amazon.smithy.aws.apigateway.traits.EndpointConfigurationTrait;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.openapi.fromsmithy.Context;
import software.amazon.smithy.openapi.model.OpenApi;
import software.amazon.smithy.openapi.model.ServerObject;
import software.amazon.smithy.utils.ListUtils;

/**
 * Adds the API Gateway {@code x-amazon-apigateway-endpoint-configuration}
 * extension to the OpenAPI model when the {@link EndpointConfigurationTrait}
 * is applied to a service.
 *
 * <p>For OpenAPI 3.0, this extension must be present under the vendor
 * extensions of the <a href="https://github.com/OAI/OpenAPI-Specification/blob/main/versions/3.0.0.md#server-object">Server
 * object</a> rather than at the top level of the document (top-level placement
 * is only used for Swagger 2.0). If the OpenAPI model does not define any
 * servers, a default server with a URL of {@code /} is added to hold the
 * extension.
 *
 * <p>The {@code types} member is not written to the extension because it is
 * not a supported property of {@code x-amazon-apigateway-endpoint-configuration};
 * endpoint types are configured outside of the OpenAPI extension at API import
 * time.
 *
 * <p>The {@code ipAddressType} member is also omitted from the extension
 * because it is not a supported property; it is configured outside of the
 * OpenAPI extension at API import time.
 *
 * @see <a href="https://docs.aws.amazon.com/apigateway/latest/developerguide/api-gateway-swagger-extensions-endpoint-configuration.html">x-amazon-apigateway-endpoint-configuration</a>
 */
final class AddEndpointConfiguration implements ApiGatewayMapper {

    private static final String EXTENSION_NAME = "x-amazon-apigateway-endpoint-configuration";
    private static final String VPC_ENDPOINT_IDS = "vpcEndpointIds";
    private static final String DISABLE_EXECUTE_API_ENDPOINT = "disableExecuteApiEndpoint";
    private static final String DEFAULT_SERVER_URL = "/";
    private static final Logger LOGGER = Logger.getLogger(AddEndpointConfiguration.class.getName());

    @Override
    public List<ApiGatewayConfig.ApiType> getApiTypes() {
        return ListUtils.of(ApiGatewayConfig.ApiType.REST);
    }

    @Override
    public OpenApi after(Context<? extends Trait> context, OpenApi openApi) {
        return context.getService()
                .getTrait(EndpointConfigurationTrait.class)
                .map(trait -> addExtension(context, openApi, trait))
                .orElse(openApi);
    }

    private OpenApi addExtension(
            Context<? extends Trait> context,
            OpenApi openApi,
            EndpointConfigurationTrait trait
    ) {
        ObjectNode.Builder node = Node.objectNodeBuilder();

        trait.getVpcEndpointIds()
                .ifPresent(ids -> node.withMember(
                        VPC_ENDPOINT_IDS,
                        ids.stream().map(Node::from).collect(ArrayNode.collect())));

        trait.getDisableExecuteApiEndpoint()
                .ifPresent(disabled -> node.withMember(
                        DISABLE_EXECUTE_API_ENDPOINT,
                        Node.from(disabled)));

        ObjectNode extension = node.build();
        if (extension.isEmpty()) {
            return openApi;
        }

        LOGGER.fine(() -> String.format(
                "Adding %s to the server objects of %s",
                EXTENSION_NAME,
                context.getService().getId()));

        OpenApi.Builder builder = openApi.toBuilder();
        builder.clearServer();
        if (openApi.getServers().isEmpty()) {
            // URL is required, so use OpenAPI 3.0's default of "/".
            builder.addServer(ServerObject.builder()
                    .url(DEFAULT_SERVER_URL)
                    .putExtension(EXTENSION_NAME, extension)
                    .build());
        } else {
            for (ServerObject server : openApi.getServers()) {
                builder.addServer(server.toBuilder()
                        .putExtension(EXTENSION_NAME, extension)
                        .build());
            }
        }

        return builder.build();
    }
}
