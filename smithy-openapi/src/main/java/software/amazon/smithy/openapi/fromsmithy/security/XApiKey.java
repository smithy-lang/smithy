package software.amazon.smithy.openapi.fromsmithy.security;

import software.amazon.smithy.openapi.OpenApiConstants;
import software.amazon.smithy.openapi.fromsmithy.Context;
import software.amazon.smithy.openapi.fromsmithy.SecuritySchemeConverter;
import software.amazon.smithy.openapi.model.SecurityScheme;

/**
 * Uses an HTTP header named X-Api-Key that contains an API key.
 *
 * <p>This is compatible with Amazon API Gateway API key authorization.
 *
 * @see <a href="https://docs.aws.amazon.com/apigateway/latest/developerguide/api-gateway-api-key-source.html">API Gateway documentation</a>
 */
public final class XApiKey implements SecuritySchemeConverter {
    @Override
    public String getAuthSchemeName() {
        return "http-x-api-key";
    }

    @Override
    public String getSecurityName(Context context) {
        return context.getConfig().getStringMemberOrDefault(
                OpenApiConstants.SECURITY_NAME_PREFIX + getAuthSchemeName(), "api_key");
    }

    @Override
    public SecurityScheme createSecurityScheme(Context context) {
        return SecurityScheme.builder()
                .type("apiKey")
                .in("header")
                .name("X-Api-Key")
                .description("X-Api-Key authentication")
                .build();
    }
}
