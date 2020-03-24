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
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;
import software.amazon.smithy.aws.apigateway.traits.AuthorizerDefinition;
import software.amazon.smithy.aws.apigateway.traits.AuthorizerIndex;
import software.amazon.smithy.aws.apigateway.traits.AuthorizerTrait;
import software.amazon.smithy.aws.apigateway.traits.AuthorizersTrait;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.openapi.fromsmithy.Context;
import software.amazon.smithy.openapi.fromsmithy.OpenApiMapper;
import software.amazon.smithy.openapi.fromsmithy.SecuritySchemeConverter;
import software.amazon.smithy.openapi.fromsmithy.mappers.RemoveUnusedComponents;
import software.amazon.smithy.openapi.model.ComponentsObject;
import software.amazon.smithy.openapi.model.OpenApi;
import software.amazon.smithy.openapi.model.OperationObject;
import software.amazon.smithy.openapi.model.SecurityScheme;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.MapUtils;

/**
 * Dynamically creates security schemes for API Gateway authorizers based on
 * the {@link AuthorizerTrait}.
 *
 * <p>The effective authorizer of a service and each operation is calculated,
 * and if resolves to a value, the components are updated to remove the
 * scheme that the authorizer augments. API Gateway authorizers create a copy
 * of the scheme that they augment and apply API Gateway extensions. This
 * allows for a single Smithy authentication scheme to be referenced by
 * multiple authorizers.
 *
 * <p>This mapper quite possibly could cause security scheme definitions on a
 * service to become unreferenced by any operation. These security schemes
 * should be picked up and removed by the {@link RemoveUnusedComponents}
 * mapper.
 */
final class AddAuthorizers implements OpenApiMapper {
    private static final String EXTENSION_NAME = "x-amazon-apigateway-authorizer";
    private static final String CLIENT_EXTENSION_NAME = "x-amazon-apigateway-authtype";
    private static final Logger LOGGER = Logger.getLogger(AddApiKeySource.class.getName());

    @Override
    public Map<String, List<String>> updateSecurity(
            Context<? extends Trait> context,
            Shape shape,
            SecuritySchemeConverter<? extends Trait> converter,
            Map<String, List<String>> requirement
    ) {
        // Only modify requirements that exactly match the updated scheme.
        if (requirement.size() != 1
                || !requirement.keySet().iterator().next().equals(converter.getAuthSchemeId().toString())) {
            return requirement;
        }

        ServiceShape service = context.getService();
        AuthorizerIndex authorizerIndex = context.getModel().getKnowledge(AuthorizerIndex.class);

        return authorizerIndex.getAuthorizer(service, shape)
                // Remove the original scheme authentication scheme from the operation if found.
                // Add a new scheme for this operation using the authorizer name.
                .map(authorizer -> MapUtils.of(authorizer, requirement.values().iterator().next()))
                .orElse(requirement);
    }

    @Override
    public OperationObject updateOperation(Context context, OperationShape shape, OperationObject operation) {
        ServiceShape service = context.getService();
        AuthorizerIndex authorizerIndex = context.getModel().getKnowledge(AuthorizerIndex.class);

        // Get the resolved security schemes of the service and operation, and
        // only add security if it's different than the service.
        String serviceAuth = authorizerIndex.getAuthorizer(service).orElse(null);
        String operationAuth = authorizerIndex.getAuthorizer(service, shape).orElse(null);

        if (operationAuth == null || Objects.equals(operationAuth, serviceAuth)) {
            return operation;
        }

        return operation.toBuilder()
                .addSecurity(MapUtils.of(operationAuth, ListUtils.of()))
                .build();
    }

    @Override
    public OpenApi after(Context<? extends Trait> context, OpenApi openapi) {
        return context.getService().getTrait(AuthorizersTrait.class)
                .map(authorizers -> addComputedAuthorizers(context, openapi, authorizers))
                .orElse(openapi);
    }

    private OpenApi addComputedAuthorizers(
            Context<? extends Trait> context,
            OpenApi openApi,
            AuthorizersTrait trait
    ) {
        OpenApi.Builder builder = openApi.toBuilder();
        ComponentsObject.Builder components = openApi.getComponents().toBuilder();

        for (Map.Entry<String, AuthorizerDefinition> entry : trait.getAllAuthorizers().entrySet()) {
            String authorizerName = entry.getKey();
            AuthorizerDefinition authorizer = entry.getValue();
            ShapeId scheme = entry.getValue().getScheme();

            for (SecuritySchemeConverter<? extends Trait> converter : context.getSecuritySchemeConverters()) {
                if (isAuthConverterMatched(context, converter, scheme)) {
                    SecurityScheme updatedScheme = convertAuthScheme(context, converter, authorizer, authorizerName);
                    components.putSecurityScheme(authorizerName, updatedScheme);
                    break;
                }
            }
        }

        builder.components(components.build());
        return builder.build();
    }

    private boolean isAuthConverterMatched(
            Context<? extends Trait> context,
            SecuritySchemeConverter<? extends Trait> converter,
            ShapeId scheme
    ) {
        return converter.getAuthSchemeId().equals(scheme)
               && context.getService().hasTrait(converter.getAuthSchemeType());
    }

    private <T extends Trait> SecurityScheme convertAuthScheme(
            Context<? extends Trait> context,
            SecuritySchemeConverter<T> converter,
            AuthorizerDefinition authorizer,
            String authorizerName
    ) {
        T authTrait = context.getService().expectTrait(converter.getAuthSchemeType());
        SecurityScheme createdScheme = converter.createSecurityScheme(context, authTrait);
        SecurityScheme.Builder schemeBuilder = createdScheme.toBuilder();
        schemeBuilder.putExtension(CLIENT_EXTENSION_NAME, authorizer.getAuthType());

        ObjectNode authorizerNode = Node.objectNodeBuilder()
                .withOptionalMember("type", authorizer.getType().map(Node::from))
                .withOptionalMember("authorizerUri", authorizer.getUri().map(Node::from))
                .withOptionalMember("authorizerCredentials", authorizer.getCredentials().map(Node::from))
                .withOptionalMember("identityValidationExpression",
                                    authorizer.getIdentityValidationExpression().map(Node::from))
                .withOptionalMember("identitySource", authorizer.getIdentitySource().map(Node::from))
                .withOptionalMember("authorizerResultTtlInSeconds",
                                    authorizer.getResultTtlInSeconds().map(Node::from))
                .build();
        if (authorizerNode.size() != 0) {
            schemeBuilder.putExtension(EXTENSION_NAME, authorizerNode);
        }

        LOGGER.fine(() -> String.format("Adding the `%s` OpenAPI security scheme", authorizerName));
        return schemeBuilder.build();
    }
}
