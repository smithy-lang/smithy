package software.amazon.smithy.openapi.fromsmithy.security;

import java.util.Set;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.traits.AuthenticationTrait;
import software.amazon.smithy.openapi.OpenApiException;
import software.amazon.smithy.openapi.fromsmithy.Context;
import software.amazon.smithy.openapi.fromsmithy.SecuritySchemeConverter;
import software.amazon.smithy.openapi.model.SecurityScheme;

/**
 * An authentication scheme converter that adds Cognito User Pool based
 * authentication ({@code cognito_user_pools} to an OpenAPI model when the
 * {@code apigateway.cognito-user-pools} authentication scheme is found on
 * a service shape.
 *
 * @see <a href="https://docs.aws.amazon.com/apigateway/latest/developerguide/apigateway-enable-cognito-user-pool.html">Integrate a REST API with a User Pool </a>
 */
public class AmazonCognitoUserPools implements SecuritySchemeConverter {
    private static final String SCHEME = "apigateway.cognito-user-pools";
    private static final String AUTH_HEADER = "Authorization";
    private static final Set<String> REQUEST_HEADERS = Set.of(AUTH_HEADER);
    private static final String AUTH_TYPE = "cognito_user_pools";
    private static final String PROVIDER_ARNS_PROPERTY = "providerARNs";

    @Override
    public String getAuthenticationSchemeName() {
        return SCHEME;
    }

    @Override
    public SecurityScheme createSecurityScheme(
            Context context,
            AuthenticationTrait authTrait,
            AuthenticationTrait.AuthScheme authScheme
    ) {
        // Providers are provided in the providerARNs property as a CSV list of ARNs.
        var providers = authScheme.getSetting(PROVIDER_ARNS_PROPERTY)
                .orElseThrow(() -> new OpenApiException(String.format(
                        "Missing required `%s` property in `%s` authentication scheme of `%s`.",
                        PROVIDER_ARNS_PROPERTY, SCHEME, context.getService().getId())))
                .split(",");

        // Trim any whitespace from the string.
        for (var i = 0; i < providers.length; i++) {
            providers[i] = providers[i].trim();
        }

        return SecurityScheme.builder()
                .type("apiKey")
                .description("Amazon Cognito User Pools authentication")
                .name(AUTH_HEADER)
                .in("header")
                .putExtension("x-amazon-apigateway-authtype", Node.from(AUTH_TYPE))
                .putExtension("x-amazon-apigateway-authorizer", Node.objectNode()
                        .withMember("type", Node.from(AUTH_TYPE))
                        .withMember(PROVIDER_ARNS_PROPERTY, Node.fromStrings(providers)))
                .build();
    }

    @Override
    public Set<String> getAuthRequestHeaders() {
        return REQUEST_HEADERS;
    }
}
