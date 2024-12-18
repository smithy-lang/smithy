/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.openapi.fromsmithy.security;

import software.amazon.smithy.model.traits.HttpBasicAuthTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.openapi.fromsmithy.Context;
import software.amazon.smithy.openapi.fromsmithy.SecuritySchemeConverter;
import software.amazon.smithy.openapi.model.SecurityScheme;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Applies Basic HTTP auth.
 */
@SmithyInternalApi
public final class HttpBasicConverter implements SecuritySchemeConverter<HttpBasicAuthTrait> {
    @Override
    public Class<HttpBasicAuthTrait> getAuthSchemeType() {
        return HttpBasicAuthTrait.class;
    }

    @Override
    public SecurityScheme createSecurityScheme(Context<? extends Trait> context, HttpBasicAuthTrait trait) {
        return SecurityScheme.builder()
                .type("http")
                .scheme("Basic")
                .description("HTTP Basic authentication")
                .build();
    }
}
