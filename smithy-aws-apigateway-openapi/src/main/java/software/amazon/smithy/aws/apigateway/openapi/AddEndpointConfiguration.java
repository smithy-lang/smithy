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
 * object</a> rather than at the top level of the document; top-level
 * placement is only correct for Swagger 2.0.
 *
 * <p>If the OpenAPI model does not define any servers, a default server with
 * a URL of {@code /} is added to hold the extension ({@code url} is required
 * by the Server object). API Gateway ignores the host of the server URL
 * because it assigns an execute-api domain name to the API based on the
 * restApiId and uses the path portion of the URL as a potential base path.
 * A URL of {@code /} contributes no base path.
 *
 * <p>The {@code types} and {@code ipAddressType} members are not written to
 * the {@code x-amazon-apigateway-endpoint-configuration} extension because
 * they are not configurable through the OpenAPI document. Endpoint types are
 * set through the {@code endpointConfigurationTypes} parameter of
 * ImportRestApi or through UpdateRestApi patch operations; the IP address
 * type can only be set through UpdateRestApi patch operations.
 *
 * @see <a href="https://docs.aws.amazon.com/apigateway/latest/developerguide/api-gateway-swagger-extensions-endpoint-configuration.html">x-amazon-apigateway-endpoint-configuration</a>
 */
final class AddEndpointConfiguration implements ApiGatewayMapper {

    private static final String EXTENSION_NAME = "x-amazon-apigateway-endpoint-configuration";
    private static final String VPC_ENDPOINT_IDS = "vpcEndpointIds";
    private static final String DISABLE_EXECUTE_API_ENDPOINT = "disableExecuteApiEndpoint";
    private static final String DEFAULT_SERVER_URL_NO_BASE_PATH = "/";
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
        if (openApi.getServers().isEmpty()) {
            // URL is required, so use OpenAPI 3.0's default of "/".
            builder.addServer(ServerObject.builder()
                    .url(DEFAULT_SERVER_URL_NO_BASE_PATH)
                    .putExtension(EXTENSION_NAME, extension)
                    .build());
        } else {
            builder.clearServer();
            for (ServerObject server : openApi.getServers()) {
                builder.addServer(server.toBuilder()
                        .putExtension(EXTENSION_NAME, extension)
                        .build());
            }
        }

        return builder.build();
    }
}
