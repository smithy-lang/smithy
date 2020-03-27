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

import java.util.Set;
import software.amazon.smithy.aws.traits.auth.SigV4Trait;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.openapi.fromsmithy.Context;
import software.amazon.smithy.openapi.fromsmithy.SecuritySchemeConverter;
import software.amazon.smithy.openapi.model.SecurityScheme;
import software.amazon.smithy.utils.SetUtils;

/**
 * Adds AWS signature version in a way that"s compatible with AWS API Gateway.
 */
public final class AwsV4Converter implements SecuritySchemeConverter<SigV4Trait> {
    private static final String AUTH_HEADER = "Authorization";
    private static final Set<String> REQUEST_HEADERS = SetUtils.of(
            AUTH_HEADER, "Date", "X-Amz-Date", "X-Amz-Target", "X-Amz-Security-Token");

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
    public Set<String> getAuthRequestHeaders(SigV4Trait trait) {
        return REQUEST_HEADERS;
    }
}
