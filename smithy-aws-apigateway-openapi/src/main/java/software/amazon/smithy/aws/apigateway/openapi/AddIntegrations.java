/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.apigateway.openapi;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import software.amazon.smithy.aws.apigateway.traits.IntegrationTrait;
import software.amazon.smithy.aws.apigateway.traits.IntegrationTraitIndex;
import software.amazon.smithy.aws.apigateway.traits.MockIntegrationTrait;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.openapi.OpenApiException;
import software.amazon.smithy.openapi.fromsmithy.Context;
import software.amazon.smithy.openapi.model.OperationObject;
import software.amazon.smithy.utils.ListUtils;

/**
 * Adds API Gateway integrations to operations.
 */
final class AddIntegrations implements ApiGatewayMapper {

    static final String INTEGRATION_EXTENSION_NAME = "x-amazon-apigateway-integration";

    private static final Logger LOGGER = Logger.getLogger(AddIntegrations.class.getName());
    private static final String PASSTHROUGH_BEHAVIOR = "passthroughBehavior";
    private static final String INCORRECT_PASSTHROUGH_BEHAVIOR = "passThroughBehavior";

    @Override
    public List<ApiGatewayConfig.ApiType> getApiTypes() {
        return ListUtils.of(ApiGatewayConfig.ApiType.REST, ApiGatewayConfig.ApiType.HTTP);
    }

    @Override
    public OperationObject updateOperation(
            Context<? extends Trait> context,
            OperationShape shape,
            OperationObject operation,
            String httpMethod,
            String path
    ) {
        IntegrationTraitIndex index = IntegrationTraitIndex.of(context.getModel());
        return index.getIntegrationTrait(context.getService(), shape)
                .map(trait -> operation.toBuilder()
                        .putExtension(INTEGRATION_EXTENSION_NAME, createIntegration(context, shape, trait))
                        .build())
                .orElseGet(() -> {
                    LOGGER.warning("No API Gateway integration trait found for " + shape.getId());
                    return operation;
                });
    }

    static ObjectNode createIntegration(MockIntegrationTrait integration) {
        // The MockIntegrationTrait path doesn't use the context or shape,
        // so it's safe to pass null here for those.
        return createIntegration(null, null, integration);
    }

    private static ObjectNode createIntegration(
            Context<? extends Trait> context,
            OperationShape shape,
            Trait integration
    ) {
        ObjectNode integrationNode;
        if (integration instanceof MockIntegrationTrait) {
            integrationNode = integration.toNode().expectObjectNode().withMember("type", Node.from("mock"));
        } else if (integration instanceof IntegrationTrait) {
            validateTraitConfiguration((IntegrationTrait) integration, context, shape);
            integrationNode = ((IntegrationTrait) integration).toExpandedNode(context.getService(), shape);
        } else {
            throw new OpenApiException("Unexpected integration trait: " + integration);
        }

        // Fix a naming issue where the Smithy trait uses a different casing for "passthroughBehavior"
        Optional<Node> passthroughBehavior = integrationNode.getMember(INCORRECT_PASSTHROUGH_BEHAVIOR);
        if (passthroughBehavior.isPresent()) {
            integrationNode = integrationNode
                    .withoutMember(INCORRECT_PASSTHROUGH_BEHAVIOR)
                    .withMember(PASSTHROUGH_BEHAVIOR, passthroughBehavior.get());
        }
        return integrationNode;
    }

    private static void validateTraitConfiguration(
            IntegrationTrait trait,
            Context<? extends Trait> context,
            OperationShape operation
    ) {
        // For HTTP APIs, API Gateway requires that the payloadFormatVersion is set on integrations.
        // https://docs.aws.amazon.com/apigateway/latest/developerguide/api-gateway-swagger-extensions-integration.html
        // If the payloadFormatVersion has not been set on an integration and the apiGatewayType has been set to "HTTP",
        // the conversion fails.
        ApiGatewayConfig.ApiType apiType = context.getConfig()
                .getExtensions(ApiGatewayConfig.class)
                .getApiGatewayType();
        if (!trait.getPayloadFormatVersion().isPresent() && apiType.equals(ApiGatewayConfig.ApiType.HTTP)) {
            throw new OpenApiException("When the 'apiGatewayType' OpenAPI conversion setting is 'HTTP', a "
                    + "'payloadFormatVersion' must be set on the aws.apigateway#integration trait.");
        }
    }
}
