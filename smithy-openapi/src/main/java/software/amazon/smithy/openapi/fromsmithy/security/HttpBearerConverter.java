/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.openapi.fromsmithy.security;

import software.amazon.smithy.model.traits.HttpBearerAuthTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.openapi.fromsmithy.Context;
import software.amazon.smithy.openapi.fromsmithy.SecuritySchemeConverter;
import software.amazon.smithy.openapi.model.SecurityScheme;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Uses the Bearer scheme of the Authentication header.
 */
@SmithyInternalApi
public final class HttpBearerConverter implements SecuritySchemeConverter<HttpBearerAuthTrait> {
    @Override
    public Class<HttpBearerAuthTrait> getAuthSchemeType() {
        return HttpBearerAuthTrait.class;
    }

    @Override
    public SecurityScheme createSecurityScheme(Context<? extends Trait> context, HttpBearerAuthTrait trait) {
        return SecurityScheme.builder()
                .type("http")
                .scheme("Bearer")
                .description("HTTP Bearer authentication")
                .build();
    }
}
