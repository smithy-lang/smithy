/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
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
