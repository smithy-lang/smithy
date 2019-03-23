package software.amazon.smithy.openapi.fromsmithy.security;

import software.amazon.smithy.openapi.fromsmithy.Context;
import software.amazon.smithy.openapi.fromsmithy.SecuritySchemeConverter;
import software.amazon.smithy.openapi.model.SecurityScheme;

/**
 * Uses the Bearer scheme of the Authentication header.
 */
public final class HttpBearer implements SecuritySchemeConverter {
    @Override
    public String getAuthenticationSchemeName() {
        return "http-bearer";
    }

    @Override
    public SecurityScheme createSecurityScheme(Context context) {
        return SecurityScheme.builder()
                .type("http")
                .scheme("Bearer")
                .description("HTTP Bearer authentication")
                .build();
    }
}
