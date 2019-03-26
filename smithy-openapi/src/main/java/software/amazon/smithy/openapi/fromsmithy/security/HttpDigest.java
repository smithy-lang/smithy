package software.amazon.smithy.openapi.fromsmithy.security;

import software.amazon.smithy.model.traits.AuthenticationTrait;
import software.amazon.smithy.openapi.fromsmithy.Context;
import software.amazon.smithy.openapi.fromsmithy.SecuritySchemeConverter;
import software.amazon.smithy.openapi.model.SecurityScheme;

/**
 * Applies Digest HTTP auth.
 */
public final class HttpDigest implements SecuritySchemeConverter {
    @Override
    public String getAuthenticationSchemeName() {
        return "http-digest";
    }

    @Override
    public SecurityScheme createSecurityScheme(
            Context context,
            AuthenticationTrait authTrait,
            AuthenticationTrait.AuthScheme authScheme
    ) {
        return SecurityScheme.builder()
                .type("http")
                .scheme("Digest")
                .description("HTTP Digest authentication")
                .build();
    }
}
