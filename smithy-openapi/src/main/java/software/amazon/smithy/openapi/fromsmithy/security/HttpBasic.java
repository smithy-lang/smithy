package software.amazon.smithy.openapi.fromsmithy.security;

import software.amazon.smithy.openapi.fromsmithy.Context;
import software.amazon.smithy.openapi.fromsmithy.SecuritySchemeConverter;
import software.amazon.smithy.openapi.model.SecurityScheme;

/**
 * Applies Basic HTTP auth.
 */
public final class HttpBasic implements SecuritySchemeConverter {
    @Override
    public String getAuthSchemeName() {
        return "http-basic";
    }

    @Override
    public SecurityScheme createSecurityScheme(Context context) {
        return SecurityScheme.builder()
                .type("http")
                .scheme("Basic")
                .description("HTTP Basic authentication")
                .build();
    }
}
