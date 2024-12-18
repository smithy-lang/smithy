/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.apigateway.openapi;

import java.util.Set;
import software.amazon.smithy.aws.traits.auth.CognitoUserPoolsTrait;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.openapi.fromsmithy.Context;
import software.amazon.smithy.openapi.fromsmithy.SecuritySchemeConverter;
import software.amazon.smithy.openapi.model.SecurityScheme;
import software.amazon.smithy.utils.SetUtils;

/**
 * An authentication scheme converter that adds Cognito User Pool based
 * authentication ({@code cognito_user_pools} to an OpenAPI model when the
 * {@code aws.auth#cognitoUserPools} authentication scheme is found on
 * a service shape.
 *
 * @see <a href="https://docs.aws.amazon.com/apigateway/latest/developerguide/apigateway-enable-cognito-user-pool.html">Integrate a REST API with a User Pool </a>
 */
final class CognitoUserPoolsConverter implements SecuritySchemeConverter<CognitoUserPoolsTrait> {

    private static final String AUTH_HEADER = "Authorization";
    private static final Set<String> REQUEST_HEADERS = SetUtils.of(AUTH_HEADER);
    private static final String AUTH_TYPE = "cognito_user_pools";
    private static final String PROVIDER_ARNS_PROPERTY = "providerARNs";

    @Override
    public Class<CognitoUserPoolsTrait> getAuthSchemeType() {
        return CognitoUserPoolsTrait.class;
    }

    @Override
    public SecurityScheme createSecurityScheme(Context<? extends Trait> context, CognitoUserPoolsTrait trait) {
        return SecurityScheme.builder()
                .type("apiKey")
                .description("Amazon Cognito User Pools authentication")
                .name(AUTH_HEADER)
                .in("header")
                .putExtension("x-amazon-apigateway-authtype", Node.from(AUTH_TYPE))
                .putExtension("x-amazon-apigateway-authorizer",
                        Node.objectNode()
                                .withMember("type", Node.from(AUTH_TYPE))
                                .withMember(PROVIDER_ARNS_PROPERTY,
                                        trait.getProviderArns().stream().map(Node::from).collect(ArrayNode.collect())))
                .build();
    }

    @Override
    public Set<String> getAuthRequestHeaders(Context<? extends Trait> context, CognitoUserPoolsTrait trait) {
        return REQUEST_HEADERS;
    }
}
