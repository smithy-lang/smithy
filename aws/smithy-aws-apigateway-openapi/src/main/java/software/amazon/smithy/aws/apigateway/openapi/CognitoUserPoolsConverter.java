package software.amazon.smithy.aws.apigateway.openapi;

import java.util.Set;
import software.amazon.smithy.aws.traits.CognitoUserPoolsSettingsTrait;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.openapi.OpenApiException;
import software.amazon.smithy.openapi.fromsmithy.Context;
import software.amazon.smithy.openapi.fromsmithy.SecuritySchemeConverter;
import software.amazon.smithy.openapi.model.SecurityScheme;
import software.amazon.smithy.utils.SetUtils;

/**
 * An authentication scheme converter that adds Cognito User Pool based
 * authentication ({@code cognito_user_pools} to an OpenAPI model when the
 * {@code aws.cognito-user-pools} authentication scheme is found on
 * a service shape.
 *
 * @see <a href="https://docs.aws.amazon.com/apigateway/latest/developerguide/apigateway-enable-cognito-user-pool.html">Integrate a REST API with a User Pool </a>
 */
public class CognitoUserPoolsConverter implements SecuritySchemeConverter {
    private static final String SCHEME = "aws.cognito-user-pools";
    private static final String AUTH_HEADER = "Authorization";
    private static final Set<String> REQUEST_HEADERS = SetUtils.of(AUTH_HEADER);
    private static final String AUTH_TYPE = "cognito_user_pools";
    private static final String PROVIDER_ARNS_PROPERTY = "providerARNs";

    @Override
    public String getAuthSchemeName() {
        return SCHEME;
    }

    @Override
    public SecurityScheme createSecurityScheme(Context context) {
        // Providers are provided in a required trait.
        CognitoUserPoolsSettingsTrait providers = context.getService()
                .getTrait(CognitoUserPoolsSettingsTrait.class)
                .orElseThrow(() -> new OpenApiException(String.format(
                        "Missing required `%s` trait for the `%s` authentication scheme of `%s`.",
                        CognitoUserPoolsSettingsTrait.TRAIT, SCHEME, context.getService().getId())));

        return SecurityScheme.builder()
                .type("apiKey")
                .description("Amazon Cognito User Pools authentication")
                .name(AUTH_HEADER)
                .in("header")
                .putExtension("x-amazon-apigateway-authtype", Node.from(AUTH_TYPE))
                .putExtension("x-amazon-apigateway-authorizer", Node.objectNode()
                        .withMember("type", Node.from(AUTH_TYPE))
                        .withMember(PROVIDER_ARNS_PROPERTY,
                                    providers.getProviderArns().stream().map(Node::from).collect(ArrayNode.collect())))
                .build();
    }

    @Override
    public Set<String> getAuthRequestHeaders() {
        return REQUEST_HEADERS;
    }
}
