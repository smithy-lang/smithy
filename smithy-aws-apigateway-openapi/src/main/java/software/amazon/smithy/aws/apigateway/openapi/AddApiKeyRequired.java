/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.apigateway.openapi;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import software.amazon.smithy.aws.apigateway.traits.ApiKeyRequiredTrait;
import software.amazon.smithy.aws.apigateway.traits.AuthorizerIndex;
import software.amazon.smithy.model.knowledge.ServiceIndex;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.openapi.fromsmithy.Context;
import software.amazon.smithy.openapi.fromsmithy.SecuritySchemeConverter;
import software.amazon.smithy.openapi.model.OpenApi;
import software.amazon.smithy.openapi.model.OperationObject;
import software.amazon.smithy.openapi.model.SecurityScheme;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.MapUtils;

/**
 * Adds the {@code api_key} security scheme and per-operation security
 * requirements for operations annotated with the {@link ApiKeyRequiredTrait}.
 *
 * <p>API Gateway requires the {@code api_key} scheme to appear in the security requirements of
 * every operation that needs an api key. Since an operation-level {@code security} field replaces the
 * document-level one rather than merging with it, {@code api_key} must be added <em>alongside</em>
 * whatever the operation was already secured by. To retain inherited document-level security requirements,
 * {@link #updateOperation} copies those requirements and adds {@code api_key} to avoid silently leaving
 * the operation unauthenticated.
 *
 * <p>{@code api_key} is merged into each requirement rather than appended as a separate one, since
 * separate entries in {@code security} are alternatives (a caller may satisfy any one of them)
 * while the keys within a single entry are all required.
 *
 * @see <a href="https://docs.aws.amazon.com/apigateway/latest/developerguide/api-gateway-api-usage-plans.html">API Gateway Usage Plans</a>
 * for usage of API Keys.
 */
final class AddApiKeyRequired implements ApiGatewayMapper {

    private static final String SCHEME_NAME = "api_key";
    private static final Logger LOGGER = Logger.getLogger(AddApiKeyRequired.class.getName());

    @Override
    public List<ApiGatewayConfig.ApiType> getApiTypes() {
        return ListUtils.of(ApiGatewayConfig.ApiType.REST);
    }

    @Override
    public byte getOrder() {
        // Run after every mapper that writes an operation's security requirements (AddAuthorizers
        // and AddCognitoUserPoolsScopes, both at the default order 0), so updateOperation sees the
        // operation's final requirements and can merge api_key into each of them. Running earlier
        // would leave requirements added by later mappers without the api_key.
        return 1;
    }

    @Override
    public OperationObject updateOperation(
            Context<? extends Trait> context,
            OperationShape shape,
            OperationObject operation,
            String httpMethodName,
            String path
    ) {
        if (!shape.hasTrait(ApiKeyRequiredTrait.ID)) {
            return operation;
        }

        LOGGER.fine(() -> String.format(
                "Adding api_key security requirement to %s",
                shape.getId()));

        Optional<List<Map<String, List<String>>>> existing = operation.getSecurity();

        // Earlier mappers gave the operation requirements of its own: the core converter emits them
        // when the operation's effective auth schemes differ from the service's, AddAuthorizers adds
        // the resolved authorizer when it differs from the service's, and AddCognitoUserPoolsScopes
        // adds a scoped requirement. The entries are alternatives, so the api_key is merged into each of
        // them rather than appended as one more (api_key-only) alternative.
        if (existing.isPresent() && !existing.get().isEmpty()) {
            OperationObject.Builder builder = operation.toBuilder().clearSecurity();
            for (Map<String, List<String>> requirement : existing.get()) {
                builder.addSecurity(withApiKey(requirement));
            }
            return builder.build();
        }

        // An explicitly empty security field means @auth([]): the operation removes authentication,
        // so the api_key is the only requirement.
        if (existing.isPresent()) {
            return operation.toBuilder()
                    .addSecurity(MapUtils.of(SCHEME_NAME, ListUtils.of()))
                    .build();
        }

        // Otherwise the operation has no requirements of its own and inherits the service's. Those
        // must be restated here because adding api_key creates an operation-level security field
        // that replaces the service one.
        List<Map<String, List<String>>> inherited = inheritedRequirements(context, shape);
        if (inherited.isEmpty()) {
            // The service has no auth to preserve, so the api_key is the only requirement.
            return operation.toBuilder()
                    .addSecurity(MapUtils.of(SCHEME_NAME, ListUtils.of()))
                    .build();
        }

        OperationObject.Builder builder = operation.toBuilder();
        for (Map<String, List<String>> requirement : inherited) {
            builder.addSecurity(withApiKey(requirement));
        }
        return builder.build();
    }

    /**
     * The requirements an operation inherits from the service, keyed the same way the rest of the
     * conversion keys them: by API Gateway authorizer name when the operation resolves to one
     * (matching {@link AddAuthorizers}), and otherwise by the OpenAPI names of the service's
     * effective auth schemes (matching the core converter).
     *
     * <p>Effective auth schemes are alternatives, so each becomes its own requirement rather than
     * being combined into one. Collapsing them would turn a choice of schemes into a demand for all
     * of them. An operation resolves to at most one authorizer, so that case is a single requirement.
     */
    private List<Map<String, List<String>>> inheritedRequirements(
            Context<? extends Trait> context,
            OperationShape shape
    ) {
        ServiceShape service = context.getService();

        Optional<String> authorizer = AuthorizerIndex.of(context.getModel())
                .getAuthorizer(service, shape);
        if (authorizer.isPresent()) {
            return ListUtils.of(MapUtils.of(authorizer.get(), ListUtils.of()));
        }

        Map<ShapeId, Trait> schemes = ServiceIndex.of(context.getModel())
                .getEffectiveAuthSchemes(service, shape);
        List<Map<String, List<String>>> requirements = new ArrayList<>();
        // Iterating the converters rather than the schemes keeps these requirements in the same order
        // the core converter writes the service's own, which it derives from the converter order.
        for (SecuritySchemeConverter<? extends Trait> converter : context.getSecuritySchemeConverters()) {
            if (schemes.containsKey(converter.getAuthSchemeId())) {
                requirements.add(MapUtils.of(
                        converter.getOpenApiAuthSchemeName(),
                        createSecurityRequirements(context, converter, service)));
            }
        }
        return requirements;
    }

    /**
     * Builds a requirement's scope list the same way the core converter builds the document-level
     * and operation-level ones, so converters that override
     * {@link SecuritySchemeConverter#createSecurityRequirements} keep their scopes when the
     * inherited requirements are restated. This method exists primarily to appease the type-checker.
     */
    private static <A extends Trait> List<String> createSecurityRequirements(
            Context<? extends Trait> context,
            SecuritySchemeConverter<A> converter,
            ServiceShape service
    ) {
        return converter.createSecurityRequirements(
                context,
                service.expectTrait(converter.getAuthSchemeType()),
                service);
    }

    /** Returns a copy of {@code requirement} with {@code api_key} added. */
    private static Map<String, List<String>> withApiKey(Map<String, List<String>> requirement) {
        Map<String, List<String>> merged = new LinkedHashMap<>(requirement);
        merged.put(SCHEME_NAME, ListUtils.of());
        return merged;
    }

    @Override
    public OpenApi after(Context<? extends Trait> context, OpenApi openApi) {
        // Only add the api_key security scheme if any operation uses the trait.
        boolean hasApiKeyRequired = context.getModel().isTraitApplied(ApiKeyRequiredTrait.class);

        if (!hasApiKeyRequired) {
            return openApi;
        }

        SecurityScheme apiKeyScheme = SecurityScheme.builder()
                .type("apiKey")
                .name("x-api-key")
                .in("header")
                .build();

        return openApi.toBuilder()
                .components(openApi.getComponents()
                        .toBuilder()
                        .putSecurityScheme(SCHEME_NAME, apiKeyScheme)
                        .build())
                .build();
    }
}
