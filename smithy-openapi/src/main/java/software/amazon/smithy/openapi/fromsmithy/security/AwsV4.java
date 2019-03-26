package software.amazon.smithy.openapi.fromsmithy.security;

import java.util.Set;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.traits.AuthenticationTrait;
import software.amazon.smithy.openapi.OpenApiConstants;
import software.amazon.smithy.openapi.fromsmithy.Context;
import software.amazon.smithy.openapi.fromsmithy.SecuritySchemeConverter;
import software.amazon.smithy.openapi.model.SecurityScheme;

/**
 * Adds AWS signature version in a way that"s compatible with AWS API Gateway.
 */
public final class AwsV4 implements SecuritySchemeConverter {
    private static final String AUTH_HEADER = "Authorization";
    private static final Set<String> REQUEST_HEADERS = Set.of(
            AUTH_HEADER, "Date", "X-Amz-Date", "X-Amz-Target", "X-Amz-Security-Token");

    @Override
    public String getAuthenticationSchemeName() {
        return "aws.v4";
    }

    @Override
    public String getSecurityName(Context context) {
        return context.getConfig().getStringMemberOrDefault(
                OpenApiConstants.SECURITY_NAME_PREFIX + getAuthenticationSchemeName(), "sigv4");
    }

    @Override
    public SecurityScheme createSecurityScheme(
            Context context,
            AuthenticationTrait authTrait,
            AuthenticationTrait.AuthScheme authScheme
    ) {
        return SecurityScheme.builder()
                .type("apiKey")
                .description("AWS Signature Version 4 authentication")
                .name(AUTH_HEADER)
                .in("header")
                .putExtension("x-amazon-apigateway-authtype", Node.from("awsSigv4"))
                .build();
    }

    @Override
    public Set<String> getAuthRequestHeaders() {
        return REQUEST_HEADERS;
    }
}
