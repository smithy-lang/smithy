/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.apigateway.openapi;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.aws.apigateway.traits.ApiKeyRequiredTrait;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.openapi.OpenApiConfig;
import software.amazon.smithy.openapi.fromsmithy.OpenApiConverter;
import software.amazon.smithy.openapi.fromsmithy.mappers.RemoveUnusedComponents;
import software.amazon.smithy.utils.IoUtils;

/**
 * Covers how {@link ApiKeyRequiredTrait} combines with the auth an operation is otherwise subject to,
 * including API Gateway authorizers. Each test converts one {@code api-key-required-*.smithy} model
 * and compares the entire result against a static {@code .openapi.json} document to ensure the
 * converted OpenApi result stays consistent.
 *
 * <p>The following are scenarios, referenced by ID in the test comments below. "Emitted" indicates
 * where {@link OpenApiConverter#addOperationSecurity} emitted the operation's requirements because its
 * effective auth schemes differ from the service's; sigv4, bearer, tokenAuth etc. abbreviate the
 * requirement keys in the resulting {@code security} list:
 *
 * <pre>
 * ID  | Service auth              | Operation                   | ApiKey | Emitted | operation level security
 * A1  | none                      | inherits                    | yes    | no      | [{api_key}]
 * A2  | sigv4                     | inherits                    | no     | no      | absent (inherits)
 * A3  | sigv4                     | inherits                    | yes    | no      | [{sigv4, api_key}]
 * A4  | sigv4 + basic + bearer    | auth([sigv4])               | yes    | yes     | [{sigv4, api_key}]
 * A5  | sigv4                     | auth([])                    | yes    | no      | [{api_key}]
 * A6  | sigv4 + basic + bearer    | auth([basic, bearer])       | yes    | yes     | [{basic, api_key},
 *     |                           |                             |        |         |  {bearer, api_key}]
 * A7  | sigv4 + basic + bearer    | inherits all                | yes    | no      | [{basic, api_key},
 *     |                           |                             |        |         |  {bearer, api_key},
 *     |                           |                             |        |         |  {sigv4, api_key}]
 * B1  | authorizers, none applied | authorizer(tokenAuth)       | no     | no      | [{tokenAuth}]
 * B2  | authorizers, none applied | authorizer(tokenAuth)       | yes    | no      | [{tokenAuth, api_key}]
 * B3  | sigv4 + basic + bearer,   | auth([bearer]) + authorizer | yes    | yes     | [{tokenAuth, api_key}]
 *     | authorizers               |                             |        |         |
 * B4  | sigv4 + basic + bearer,   | auth([basic, bearer])       | yes    | yes     | [{tokenAuth, api_key}]
 *     | authorizers               | + authorizer(tokenAuth)     |        |         |
 * C1  | authorizer(tokenAuth)     | inherits                    | no     | no      | absent
 * C2  | authorizer(tokenAuth)     | inherits                    | yes    | no      | [{tokenAuth, api_key}]
 * C3  | authorizer(tokenAuth)     | authorizer(tokenAuth), same | yes    | no      | [{tokenAuth, api_key}]
 * C4  | authorizer(tokenAuth)     | authorizer(otherAuth)       | yes    | no      | [{otherAuth, api_key}]
 * D1  | built-in key authorizer   | inherits                    | no     | no      | [{apiKeyAuth}]
 * D2  | built-in key authorizer   | inherits                    | yes    | no      | [{apiKeyAuth, api_key}]
 * E1  | cognito                   | auth([]) + scopes           | no     | no      | [] (explicitly empty)
 * E2  | cognito                   | scopes(email, profile)      | no     | no      | [{cognito: scopes}]
 * E3  | cognito                   | scopes(email, profile)      | yes    | no      | [{cognito: scopes, api_key}]
 * E4  | cognito                   | inherits                    | yes    | no      | [{cognito, api_key}]
 * E5  | cognito                   | auth([])                    | yes    | no      | [{api_key}]
 * </pre>
 *
 * <p>A1 is covered by {@link AddApiKeyRequiredTest}; everything else is covered here.
 *
 * <p>Summary of the code paths referenced in the comments:
 *
 * <ul>
 *     <li>{@link OpenApiConverter#addOperationSecurity} emits operation-level requirements only when the operation's
 *     effective auth schemes differ from the service's. An {@code @authorizer} never affects the auth scheme
 *     comparison, because an authorizer references an auth scheme but is not itself one. Each emitted requirement
 *     passes through the {@code updateSecurity} hook below. {@code @auth([])} is the one exception to "differ means
 *     emit": it is handled before the auth scheme comparison, writing an explicitly empty {@code security} list and
 *     returning, so nothing is emitted (A5's Emitted is "no" even though an empty scheme set differs from the
 *     service's).</li>
 *     <li>{@link AddAuthorizers#updateSecurity} renames an emitted requirement's key from the auth scheme name to
 *     the resolved authorizer name, for single-entry requirements only. It is also invoked for the service shape,
 *     which is how the document-level requirement gets renamed.</li>
 *     <li>{@link AddAuthorizers#updateOperation} adds a requirement for the resolved authorizer when it differs from
 *     the service's (or is API Gateway's built-in API keys, which are restated on every operation).</li>
 *     <li>{@link AddCognitoUserPoolsScopes#updateOperation} adds a scoped requirement for operations carrying
 *     {@code @cognitoUserPoolsScopes}.</li>
 *     <li>{@link AddApiKeyRequired#updateOperation} runs after all of the above ({@code getOrder} places it after
 *     the other mappers' {@code updateOperation}, and the {@code updateOperation} phase itself runs after all
 *     {@code updateSecurity} calls, including the {@link AddAuthorizers} rename). It merges {@code api_key} into
 *     each requirement the operation ended up with. When the operation has none of its own, it restates what the
 *     operation inherits (resolved authorizer, else one requirement per effective auth scheme) with
 *     {@code api_key} merged into each, or adds {@code api_key} if there are no requirements.</li>
 * </ul>
 */
public class ApiKeyRequiredAuthTest {

    private static void assertConvertsTo(String model, String service, String expected) {
        Model assembled = Model.assembler()
                .discoverModels(ApiKeyRequiredAuthTest.class.getClassLoader())
                .addImport(ApiKeyRequiredAuthTest.class.getClassLoader().getResource(model))
                .assemble()
                .unwrap();
        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from(service));
        Node actual = OpenApiConverter.create()
                .config(config)
                .classLoader(ApiKeyRequiredAuthTest.class.getClassLoader())
                .convertToNode(assembled);
        Node expectedNode = Node.parse(IoUtils.toUtf8String(
                ApiKeyRequiredAuthTest.class.getClassLoader().getResourceAsStream(expected)));

        Node.assertEquals(actual, expectedNode);
    }

    /**
     * Service declares {@code @sigv4} only; no authorizers anywhere.
     *
     * <ul>
     *     <li>Document level: the service cannot carry {@code @apiKeyRequired}, so its requirement is
     *     {@code [{aws.auth.sigv4}]}.</li>
     *     <li>{@code /a2-inherits-auth-no-key} (A2): no trait, both mappers do nothing. No operation-level {@code
     *     security} is written, so the operation inherits the document-level requirement.</li>
     *     <li>{@code /a3-inherits-auth-with-key} (A3): The operation's effective auth schemes equal the service's,
     *     so {@code addOperationSecurity} does nothing. {@link AddApiKeyRequired#updateOperation} finds {@code
     *     security} absent and restates the inherited sigv4 with the api_key merged in: {@code [{aws.auth.sigv4,
     *     api_key}]}. Merging into one entry makes both required (AND); appending a separate entry would have offered
     *     them as alternatives (OR).</li>
     *     <li>{@code /a5-disabled-auth-with-key} (A5): {@code @auth([])} makes {@code addOperationSecurity} write an
     *     explicitly empty {@code security} list and return. {@link AddApiKeyRequired#updateOperation} sees
     *     present-but-empty, meaning there is no auth to preserve, and adds {@code api_key} alone.</li>
     * </ul>
     */
    @Test
    public void inheritedAuth() {
        assertConvertsTo("software/amazon/smithy/aws/apigateway/openapi/api-key-required-inherited-auth.smithy",
                "smithy.example#InheritedAuthService",
                "software/amazon/smithy/aws/apigateway/openapi/api-key-required-inherited-auth.openapi.json");
    }

    /**
     * Service declares {@code @sigv4}, {@code @httpBasicAuth} and {@code @httpBearerAuth}, plus a
     * {@code tokenAuth} authorizer (referencing bearer) that no service-level {@code @authorizer}
     * applies.
     *
     * <ul>
     *     <li>{@code /a4-narrowed-auth-with-key} (A4): {@code @auth([sigv4])} makes the effective schemes differ, so
     *     {@code addOperationSecurity} writes {@code {aws.auth.sigv4}} to the operation's security. No authorizer
     *     resolves, so {@link AddAuthorizers} does nothing, and {@link AddApiKeyRequired#updateOperation}
     *     merges api_key in: {@code [{aws.auth.sigv4, api_key}]}. The excluded schemes must not reappear.</li>
     *     <li>{@code /a6-two-schemes-with-key} (A6): {@code @auth([httpBasicAuth, httpBearerAuth])} makes the
     *     effective schemes differ, so {@code addOperationSecurity} writes TWO single-entry requirements, one per
     *     scheme. No authorizer resolves, so {@link AddAuthorizers} does nothing and
     *     {@link AddApiKeyRequired#updateOperation} merges {@code api_key} into each:
     *     {@code [{httpBasicAuth, api_key}, {httpBearerAuth, api_key}]}.</li>
     *     <li>{@code /a7-inherits-all-schemes-with-key} (A7): {@code addOperationSecurity} writes nothing (schemes
     *     equal the service's), {@code security} is absent, and no authorizer resolves, so
     *     {@link AddApiKeyRequired#updateOperation} restates the effective schemes. They are alternatives, so each
     *     becomes its own requirement with api_key merged into each: {@code [{httpBasicAuth, api_key},
     *     {httpBearerAuth, api_key}, {aws.auth.sigv4, api_key}]}. Collapsing them into one entry would require all
     *     the schemes at once.</li>
     *     <li>{@code /b1-authorizer-no-key} (B1): again {@code addOperationSecurity} writes nothing. There is no
     *     {@code @apiKeyRequired}, so {@link AddAuthorizers#updateOperation} adds {@code [{tokenAuth}]} since the
     *     resolved authorizer differs from the service's (which has none).</li>
     *     <li>{@code /b2-authorizer-with-key} (B2): Same as B1, {@link AddAuthorizers#updateOperation} adds
     *     {@code {tokenAuth}}, then {@link AddApiKeyRequired#updateOperation} merges the api_key into it:
     *     {@code [{tokenAuth, api_key}]}.</li>
     *     <li>{@code /b3-narrowed-auth-and-authorizer-with-key} (B3): {@code @auth([httpBearerAuth])} makes the
     *     effective auth schemes differ, so {@code addOperationSecurity} writes {@code {httpBearerAuth}}. The
     *     authorizer happens to reference the same scheme, but it's inconsequential. The requirement contains only
     *     one auth scheme, so {@link AddAuthorizers#updateSecurity} REPLACES that scheme with the resolved
     *     authorizer by renaming the requirement to {@code {tokenAuth}}. {@link AddAuthorizers#updateOperation}
     *     appends the same {@code {tokenAuth}}, which collapses with the renamed requirement in the operation's
     *     security set, and {@link AddApiKeyRequired#updateOperation} then merges in api_key, resulting in the
     *     requirement {@code [{tokenAuth, api_key}]}.
     *     Despite the auth scheme being present on the operation, the authorizer effectively takes priority.
     *     Since renaming applies only to single-entry requirements, {@link AddApiKeyRequired#updateOperation}'s
     *     merging behavior which creates multiple-entry requirements must happen after the renaming. Otherwise without
     *     the renaming, the auth schemes would effectively take priority over the authorizer.
     *     Since api_key needs to apply to all requirements, {@link AddApiKeyRequired} must run after all mappers
     *     have updated the requirements. This is achieved through {@link AddApiKeyRequired#getOrder} placing it
     *     after the other auth related mappers.</li>
     *     <li>{@code /b4-two-schemes-and-authorizer-with-key} (B4): same as B3 but with two narrowed auth schemes
     *     instead of one.
     *     Both single-entry requirements are renamed to the same resolved authorizer and collapse into one in the
     *     operation's security set. api_key is merged in, resulting in the same as B3.</li>
     * </ul>
     */
    @Test
    public void operationAuthorizer() {
        assertConvertsTo("software/amazon/smithy/aws/apigateway/openapi/api-key-required-operation-authorizer.smithy",
                "smithy.example#OperationAuthorizerService",
                "software/amazon/smithy/aws/apigateway/openapi/api-key-required-operation-authorizer.openapi.json");
    }

    /**
     * Service declares {@code @httpBearerAuth} and applies {@code @authorizer("tokenAuth")} at the
     * service level, with a second {@code otherAuth} authorizer available. Both authorizers are
     * bearer-token Lambda authorizers, referencing the scheme the service declares.
     *
     * <ul>
     *     <li>Document level: {@code updateSecurity} also runs for the service shape, where {@code AddAuthorizers}
     *     renames the service requirement from {@code {httpBearerAuth}} to {@code [{tokenAuth}]}. With nothing
     *     referencing {@code httpBearerAuth} anywhere, {@link RemoveUnusedComponents} drops {@code httpBearerAuth}
     *     from {@code securitySchemes}.</li>
     *     <li>{@code /c1-inherits-authorizer-no-key} (C1): {@link AddAuthorizers#updateOperation} returns early
     *     because the resolved authorizer equals the service's, so no operation-level {@code security} is written
     *     and the document-level requirement applies.</li>
     *     <li>{@code /c2-inherits-authorizer-with-key} (C2): the operation's auth schemes are inherited from the
     *     service, so {@code addOperationSecurity} writes nothing and {@link AddApiKeyRequired#updateOperation}
     *     restates the inherited authorizer with api_key merged: {@code [{tokenAuth, api_key}]}.</li>
     *     <li>{@code /c3-same-authorizer-with-key} (C3): repeating the service's own {@code @authorizer("tokenAuth")}
     *     resolves to the same authorizer as inheriting it, so the result matches C2 exactly.</li>
     *     <li>{@code /c4-different-authorizer-with-key} (C4): {@link AddAuthorizers#updateOperation} adds
     *     {@code {otherAuth}} since it differs from the service's authorizer, then
     *     {@link AddApiKeyRequired#updateOperation} merges the api_key: {@code [{otherAuth, api_key}]}.</li>
     * </ul>
     */
    @Test
    public void serviceAuthorizer() {
        assertConvertsTo("software/amazon/smithy/aws/apigateway/openapi/api-key-required-service-authorizer.smithy",
                "smithy.example#ServiceAuthorizerService",
                "software/amazon/smithy/aws/apigateway/openapi/api-key-required-service-authorizer.openapi.json");
    }

    /**
     * Service declares {@code @httpApiKeyAuth} and applies a scheme-only {@code apiKeyAuth} authorizer (no
     * {@code type}, no {@code customAuthType}), which is how a model requests API Gateway's built-in API keys.
     *
     * <ul>
     *     <li>Document level: renamed to {@code [{apiKeyAuth}]} by the service-shape {@code updateSecurity} call, as
     *     in the service-authorizer test.</li>
     *     <li>{@code /d1-built-in-key-no-key} (D1, control): the resolved authorizer is the service's, which normally
     *     makes {@link AddAuthorizers#updateOperation} return early, but {@code usesApiGatewayApiKeys} matches the
     *     authorizer (scheme httpApiKeyAuth, no type, no customAuthType), so the {@code apiKeyAuth} authorizer is added
     *     as a requirement on the operation.</li>
     *     <li>{@code /d2-built-in-key-with-key} (D2): Similar to D1, {@link AddAuthorizers} adds {@code {apiKeyAuth}},
     *     then {@link AddApiKeyRequired#updateOperation} merges the api_key: {@code [{apiKeyAuth, api_key}]}.</li>
     * </ul>
     */
    @Test
    public void builtInKeyAuthorizer() {
        assertConvertsTo("software/amazon/smithy/aws/apigateway/openapi/api-key-required-builtin-key-authorizer.smithy",
                "smithy.example#BuiltInKeyAuthorizerService",
                "software/amazon/smithy/aws/apigateway/openapi/api-key-required-builtin-key-authorizer.openapi.json");
    }

    /**
     * Service declares {@code @cognitoUserPools}; no authorizers. Unlike the other scenarios, the
     * scoped requirement is written by {@link AddCognitoUserPoolsScopes#updateOperation} rather than
     * by the core converter, so this covers {@code api_key} being merged into a requirement that
     * another mapper's {@code updateOperation} added.
     *
     * <ul>
     *     <li>{@code /e1-disabled-auth-no-key} (E1, control): {@code @auth([])} with scopes but no key.
     *     {@code addOperationSecurity} writes an explicitly empty {@code security} list,
     *     {@link AddCognitoUserPoolsScopes#updateOperation} does nothing despite the scopes trait because Cognito
     *     is not an effective auth scheme for the operation.</li>
     *     <li>{@code /e2-scoped-no-key} (E2, control): {@link AddCognitoUserPoolsScopes#updateOperation} adds the
     *     scoped requirement: {@code [{cognito: [email, profile]}]}.</li>
     *     <li>{@code /e3-scoped-with-key} (E3): same, then {@link AddApiKeyRequired#updateOperation} (which runs
     *     after it by {@code getOrder}) merges the api_key into the scoped requirement:
     *     {@code [{cognito: [email, profile], api_key}]}. The api_key must not be appended as a separate (key-only)
     *     alternative, and the scoped requirement must not cause the api_key to be dropped.</li>
     *     <li>{@code /e4-unscoped-with-key} (E4): no scopes, so the operation has no requirements of its own and
     *     {@link AddApiKeyRequired#updateOperation} restates the inherited Cognito scheme with the api_key merged:
     *     {@code [{cognito, api_key}]}.</li>
     *     <li>{@code /e5-disabled-auth-with-key} (E5): {@code @auth([])} and no scopes, the A5 case on a Cognito
     *     service. {@code addOperationSecurity} writes an explicitly empty {@code security} list,
     *     {@link AddCognitoUserPoolsScopes#updateOperation} does nothing (no scopes trait, and Cognito is not an
     *     effective auth scheme for the operation), and {@link AddApiKeyRequired#updateOperation} adds
     *     {@code api_key} alone: {@code [{api_key}]}.</li>
     * </ul>
     */
    @Test
    public void cognitoScopes() {
        assertConvertsTo("software/amazon/smithy/aws/apigateway/openapi/api-key-required-cognito-scopes.smithy",
                "smithy.example#CognitoScopesService",
                "software/amazon/smithy/aws/apigateway/openapi/api-key-required-cognito-scopes.openapi.json");
    }

}
