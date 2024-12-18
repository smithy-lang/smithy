/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.apigateway.openapi;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.smithy.aws.apigateway.traits.RequestValidatorTrait;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.openapi.fromsmithy.Context;
import software.amazon.smithy.openapi.model.OpenApi;
import software.amazon.smithy.openapi.model.OperationObject;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.OptionalUtils;

/**
 * Adds the API Gateway x-amazon-apigateway-request-validators object
 * to the service and x-amazon-apigateway-request-validator to the
 * service/operations.
 *
 * <p>Any operation or service shape with the {@link RequestValidatorTrait}
 * applied to it will cause that operation to have the {@code x-amazon-apigateway-request-validator}
 * extension, and adds a {@code x-amazon-apigateway-request-validators} extension
 * to the top-level OpenAPI document.
 *
 * @see <a href="https://docs.aws.amazon.com/apigateway/latest/developerguide/api-gateway-swagger-extensions-request-validators.html">Request validators</a>
 */
final class AddRequestValidators implements ApiGatewayMapper {

    private static final String REQUEST_VALIDATOR = "x-amazon-apigateway-request-validator";
    private static final String REQUEST_VALIDATORS = "x-amazon-apigateway-request-validators";
    private static final Map<String, Node> KNOWN_VALIDATORS = MapUtils.of(
            "params-only",
            Node.objectNode().withMember("validateRequestParameters", Node.from(true)),
            "body-only",
            Node.objectNode().withMember("validateRequestBody", Node.from(true)),
            "full",
            Node.objectNode()
                    .withMember("validateRequestParameters", Node.from(true))
                    .withMember("validateRequestBody", Node.from(true)));

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
        return shape.getTrait(RequestValidatorTrait.class)
                .map(RequestValidatorTrait::getValue)
                .map(value -> operation.toBuilder().putExtension(REQUEST_VALIDATOR, value).build())
                .orElse(operation);
    }

    @Override
    public OpenApi after(Context<? extends Trait> context, OpenApi openapi) {
        // Find each known request validator on operation shapes.
        Set<String> validators = context.getModel()
                .shapes(OperationShape.class)
                .flatMap(shape -> OptionalUtils.stream(shape.getTrait(RequestValidatorTrait.class)))
                .map(RequestValidatorTrait::getValue)
                .filter(KNOWN_VALIDATORS::containsKey)
                .collect(Collectors.toSet());

        // Check if the service has a request validator.
        String serviceValidator = null;
        if (context.getService().getTrait(RequestValidatorTrait.class).isPresent()) {
            serviceValidator = context.getService().getTrait(RequestValidatorTrait.class).get().getValue();
            validators.add(serviceValidator);
        }

        if (validators.isEmpty()) {
            return openapi;
        }

        OpenApi.Builder builder = openapi.toBuilder();

        if (serviceValidator != null) {
            builder.putExtension(REQUEST_VALIDATOR, serviceValidator);
        }

        // Add the known request validators to the OpenAPI model.
        ObjectNode.Builder objectBuilder = Node.objectNodeBuilder();
        for (String validator : validators) {
            objectBuilder.withMember(validator, KNOWN_VALIDATORS.get(validator));
        }

        builder.putExtension(REQUEST_VALIDATORS, objectBuilder.build());
        return builder.build();
    }
}
