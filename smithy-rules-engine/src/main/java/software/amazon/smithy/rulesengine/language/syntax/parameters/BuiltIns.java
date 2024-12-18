/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.language.syntax.parameters;

import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Built-in parameters for EndpointRules.
 */
@SmithyUnstableApi
public final class BuiltIns {
    /**
     * Built-in parameter that enables a customer to wholesale override the URL used by the SDK.
     */
    public static final Parameter SDK_ENDPOINT =
            Parameter.builder()
                    .name("Endpoint")
                    .type(ParameterType.STRING)
                    .documentation("Override the endpoint used to send this request")
                    .builtIn("SDK::Endpoint")
                    .build();

    private BuiltIns() {}
}
