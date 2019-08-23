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
import java.util.logging.Logger;
import software.amazon.smithy.aws.traits.apigateway.AuthorizerDefinition;
import software.amazon.smithy.aws.traits.apigateway.AuthorizerIndex;
import software.amazon.smithy.aws.traits.apigateway.AuthorizerTrait;
import software.amazon.smithy.aws.traits.apigateway.AuthorizersTrait;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.openapi.fromsmithy.Context;
import software.amazon.smithy.openapi.fromsmithy.OpenApiMapper;
import software.amazon.smithy.openapi.fromsmithy.SecuritySchemeConverter;
import software.amazon.smithy.openapi.fromsmithy.mappers.RemoveUnusedComponents;
import software.amazon.smithy.openapi.model.ComponentsObject;
import software.amazon.smithy.openapi.model.OpenApi;
import software.amazon.smithy.openapi.model.SecurityScheme;
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
            Context context,
            Shape shape,
            SecuritySchemeConverter converter,
            Map<String, List<String>> requirement
    ) {
        // Only modify requirements that exactly match the updated scheme.
        if (requirement.size() != 1
                || !requirement.keySet().iterator().next().equals(converter.getAuthSchemeName())) {
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
    public OpenApi after(Context context, OpenApi openapi) {
        return context.getService().getTrait(AuthorizersTrait.class)
                .map(authorizers -> addComputedAuthorizers(context, openapi, authorizers))
                .orElse(openapi);
    }

    private OpenApi addComputedAuthorizers(Context context, OpenApi openApi, AuthorizersTrait trait) {
        OpenApi.Builder builder = openApi.toBuilder();
        ComponentsObject.Builder components = openApi.getComponents().toBuilder();

        for (Map.Entry<String, AuthorizerDefinition> entry : trait.getAllAuthorizers().entrySet()) {
            String scheme = entry.getValue().getScheme();
            for (SecuritySchemeConverter converter : context.getSecuritySchemeConverters()) {
                AuthorizerDefinition authorizer = entry.getValue();
                if (converter.getAuthSchemeName().equals(scheme)) {
                    SecurityScheme createdScheme = converter.createSecurityScheme(context);
                    SecurityScheme.Builder schemeBuilder = createdScheme.toBuilder();
                    schemeBuilder.putExtension(
                            CLIENT_EXTENSION_NAME, determineApiGatewayClientName(authorizer.getScheme()));

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

                    LOGGER.fine(() -> String.format("Adding the `%s` OpenAPI security scheme", entry.getKey()));
                    components.putSecurityScheme(entry.getKey(), schemeBuilder.build());
                    break;
                }
            }
        }

        builder.components(components.build());
        return builder.build();
    }

    // TODO: should this also allow overrides via configuration properties?
    private static String determineApiGatewayClientName(String value) {
        if (value.equals("aws.v4")) {
            return "awsSigv4";
        } else {
            return "custom";
        }
    }
}
