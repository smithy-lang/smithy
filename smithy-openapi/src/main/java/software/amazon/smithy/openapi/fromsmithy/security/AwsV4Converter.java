/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.openapi.fromsmithy.security;

import java.util.Set;
import software.amazon.smithy.aws.traits.auth.SigV4Trait;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.openapi.fromsmithy.Context;
import software.amazon.smithy.openapi.fromsmithy.SecuritySchemeConverter;
import software.amazon.smithy.openapi.model.SecurityScheme;
import software.amazon.smithy.utils.SetUtils;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Adds AWS signature version in a way that"s compatible with AWS API Gateway.
 */
@SmithyInternalApi
public final class AwsV4Converter implements SecuritySchemeConverter<SigV4Trait> {
    private static final String AUTH_HEADER = "Authorization";
    private static final Set<String> REQUEST_HEADERS = SetUtils.of(
            AUTH_HEADER,
            "Date",
            "Host",
            "X-Amz-Content-Sha256",
            "X-Amz-Date",
            "X-Amz-Target",
            "X-Amz-Security-Token");

    @Override
    public Class<SigV4Trait> getAuthSchemeType() {
        return SigV4Trait.class;
    }

    @Override
    public SecurityScheme createSecurityScheme(Context<? extends Trait> context, SigV4Trait trait) {
        return SecurityScheme.builder()
                .type("apiKey")
                .description("AWS Signature Version 4 authentication")
                .name(AUTH_HEADER)
                .in("header")
                .putExtension("x-amazon-apigateway-authtype", Node.from("awsSigv4"))
                .build();
    }

    @Override
    public Set<String> getAuthRequestHeaders(Context<? extends Trait> context, SigV4Trait trait) {
        return REQUEST_HEADERS;
    }
}
