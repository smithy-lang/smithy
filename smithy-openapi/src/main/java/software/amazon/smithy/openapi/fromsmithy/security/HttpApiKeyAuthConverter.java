/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.openapi.fromsmithy.security;

import java.util.Set;
import software.amazon.smithy.model.traits.HttpApiKeyAuthTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.openapi.fromsmithy.Context;
import software.amazon.smithy.openapi.fromsmithy.SecuritySchemeConverter;
import software.amazon.smithy.openapi.model.SecurityScheme;
import software.amazon.smithy.utils.SetUtils;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Uses an HTTP header named X-Api-Key that contains an API key.
 *
 * <p>This is compatible with Amazon API Gateway API key authorization.
 *
 * @see <a href="https://docs.aws.amazon.com/apigateway/latest/developerguide/api-gateway-api-key-source.html">API Gateway documentation</a>
 */
@SmithyInternalApi
public final class HttpApiKeyAuthConverter implements SecuritySchemeConverter<HttpApiKeyAuthTrait> {
    @Override
    public Class<HttpApiKeyAuthTrait> getAuthSchemeType() {
        return HttpApiKeyAuthTrait.class;
    }

    @Override
    public SecurityScheme createSecurityScheme(Context<? extends Trait> context, HttpApiKeyAuthTrait trait) {
        StringBuilder description = new StringBuilder()
                .append("API key authentication via the '")
                .append(trait.getName())
                .append("' ");

        if (trait.getIn().equals(HttpApiKeyAuthTrait.Location.QUERY)) {
            description.append(" query string parameter");
        } else {
            description.append(trait.getIn());
        }

        if (trait.getScheme().isPresent()) {
            return SecurityScheme.builder()
                    .type("http")
                    .scheme(trait.getScheme().get())
                    .name(trait.getName())
                    .in(trait.getIn().toString())
                    .description(description.toString())
                    .build();
        }

        return SecurityScheme.builder()
                .type("apiKey")
                .name(trait.getName())
                .in(trait.getIn().toString())
                .description(description.toString())
                .build();
    }

    @Override
    public Set<String> getAuthRequestHeaders(Context<? extends Trait> context, HttpApiKeyAuthTrait trait) {
        return SetUtils.of(trait.getName());
    }
}
