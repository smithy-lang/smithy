/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.openapi.fromsmithy.security;

import software.amazon.smithy.model.traits.HttpDigestAuthTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.openapi.fromsmithy.Context;
import software.amazon.smithy.openapi.fromsmithy.SecuritySchemeConverter;
import software.amazon.smithy.openapi.model.SecurityScheme;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Applies Digest HTTP auth.
 */
@SmithyInternalApi
public final class HttpDigestConverter implements SecuritySchemeConverter<HttpDigestAuthTrait> {
    @Override
    public Class<HttpDigestAuthTrait> getAuthSchemeType() {
        return HttpDigestAuthTrait.class;
    }

    @Override
    public SecurityScheme createSecurityScheme(Context<? extends Trait> context, HttpDigestAuthTrait trait) {
        return SecurityScheme.builder()
                .type("http")
                .scheme("Digest")
                .description("HTTP Digest authentication")
                .build();
    }
}
