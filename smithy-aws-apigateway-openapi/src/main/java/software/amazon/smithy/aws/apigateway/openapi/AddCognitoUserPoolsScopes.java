/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.apigateway.openapi;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import software.amazon.smithy.aws.traits.auth.CognitoUserPoolsScopesTrait;
import software.amazon.smithy.aws.traits.auth.CognitoUserPoolsTrait;
import software.amazon.smithy.model.knowledge.ServiceIndex;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.openapi.fromsmithy.Context;
import software.amazon.smithy.openapi.model.OperationObject;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.MapUtils;

/**
 * Adds per-operation OAuth scopes to the Cognito User Pools security
 * requirement when an operation is annotated with
 * {@link CognitoUserPoolsScopesTrait}.
 *
 * <p>The mapper only applies when Cognito User Pools is an effective
 * authentication scheme for the operation. Operations that opt out of
 * authentication with {@code @auth([])} or use a different scheme are
 * left unchanged. When scopes are present the mapper appends a security
 * requirement to the operation using the Cognito scheme name, which
 * takes precedence over the service-level security requirement.
 *
 * @see <a href="https://docs.aws.amazon.com/apigateway/latest/developerguide/apigateway-enable-cognito-user-pool.html">Integrate a REST API with a User Pool</a>
 */
final class AddCognitoUserPoolsScopes implements ApiGatewayMapper {

    private static final Logger LOGGER = Logger.getLogger(AddCognitoUserPoolsScopes.class.getName());

    @Override
    public List<ApiGatewayConfig.ApiType> getApiTypes() {
        return ListUtils.of(ApiGatewayConfig.ApiType.REST);
    }

    @Override
    public OperationObject updateOperation(
            Context<? extends Trait> context,
            OperationShape shape,
            OperationObject operation,
            String httpMethodName,
            String path
    ) {
        if (!context.getService().hasTrait(CognitoUserPoolsTrait.class)) {
            return operation;
        }

        if (!shape.hasTrait(CognitoUserPoolsScopesTrait.ID)) {
            return operation;
        }

        List<String> scopes = shape.expectTrait(CognitoUserPoolsScopesTrait.class).getValues();

        // Only emit a scoped security requirement when Cognito is actually an
        // effective auth scheme for this operation. This respects operations
        // that opt out of auth via @auth([]) or that use a different scheme
        // via @auth.
        ServiceIndex serviceIndex = ServiceIndex.of(context.getModel());
        if (!serviceIndex.getEffectiveAuthSchemes(context.getService(), shape)
                .containsKey(CognitoUserPoolsTrait.ID)) {
            return operation;
        }

        String schemeName = CognitoUserPoolsTrait.ID.toString().replace("#", ".");
        Map<String, List<String>> requirement = MapUtils.of(schemeName, scopes);

        LOGGER.fine(() -> String.format(
                "Adding Cognito User Pools scopes %s to operation %s",
                scopes,
                shape.getId()));

        return operation.toBuilder()
                .addSecurity(requirement)
                .build();
    }
}
