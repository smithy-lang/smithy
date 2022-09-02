/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.rulesengine.language.syntax.parameters;

import java.util.List;
import software.amazon.smithy.rulesengine.language.eval.Value;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Built-in parameters for EndpointRules.
 */
@SmithyUnstableApi
public final class Builtins {
    /**
     * Built-in parameter representing Region eg. `us-east-1`.
     */
    public static final Parameter REGION =
            Parameter.builder()
                    .name("Region")
                    .type(ParameterType.STRING)
                    .builtIn("AWS::Region")
                    .documentation("The AWS region used to dispatch the request.")
                    .build();
    /**
     * Built-in parameter representing the DualStack parameter for SDKs.
     */
    public static final Parameter DUALSTACK =
            Parameter.builder()
                    .name("UseDualStack")
                    .type(ParameterType.BOOLEAN)
                    .builtIn("AWS::UseDualStack")
                    .documentation(
                            "When true, use the dual-stack endpoint. If the configured endpoint does not support "
                                    + "dual-stack, dispatching the request MAY return an error.")
                    .required(true)
                    .defaultValue(Value.bool(false))
                    .build();

    /**
     * Built-in parameter representing whether the endpoint must be FIPS-compliant.
     */
    public static final Parameter FIPS =
            Parameter.builder()
                    .name("UseFIPS")
                    .type(ParameterType.BOOLEAN)
                    .builtIn("AWS::UseFIPS")
                    .documentation("When true, send this request to the FIPS-compliant regional endpoint. If the "
                            + "configured endpoint does not have a FIPS compliant endpoint, dispatching "
                            + "the request will return an error.")
                    .required(true)
                    .defaultValue(Value.bool(false))
                    .build();

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

    /**
     * This MUST only be used by the S3 rules.
     */
    public static final Parameter S3_FORCE_PATH_STYLE =
            Parameter.builder()
                    .type(ParameterType.BOOLEAN)
                    .name("ForcePathStyle")
                    .builtIn("AWS::S3::ForcePathStyle")
                    .documentation(
                            "When true, force a path-style endpoint to be used where the bucket name is part of the "
                                    + "path.")
                    .build();
    /**
     * This MUST only be used by the S3 rules.
     */
    public static final Parameter S3_ACCELERATE =
            Parameter.builder()
                    .type(ParameterType.BOOLEAN)
                    .name("Accelerate")
                    .builtIn("AWS::S3::Accelerate")
                    .required(true)
                    .defaultValue(Value.bool(false))
                    .documentation(
                            "When true, use S3 Accelerate. NOTE: Not all regions support S3 accelerate.")
                    .build();

    /**
     * This MUST only be used by the S3 rules.
     */
    public static final Parameter S3_USE_ARN_REGION =
            Parameter.builder()
                    .type(ParameterType.BOOLEAN)
                    .name("UseArnRegion")
                    .builtIn("AWS::S3::UseArnRegion")
                    .documentation(
                            "When an Access Point ARN is provided and this flag is enabled, the SDK MUST"
                                    + " use the ARN's region when constructing the endpoint instead"
                                    + " of the client's configured region.")
                    .build();

    /**
     * This MUST only be used by the S3Control rules.
     */
    public static final Parameter S3_CONTROL_USE_ARN_REGION =
            Parameter.builder()
                    .type(ParameterType.BOOLEAN)
                    .name("UseArnRegion")
                    .builtIn("AWS::S3Control::UseArnRegion")
                    .documentation(
                            "When an Access Point ARN is provided and this flag is enabled, the SDK MUST"
                                    + " use the ARN's region when constructing the endpoint instead"
                                    + " of the client's configured region.")
                    .build();

    /**
     * This MUST only be used by the S3 rules.
     */
    public static final Parameter S3_USE_GLOBAL_ENDPOINT =
            Parameter.builder()
                    .type(ParameterType.BOOLEAN)
                    .name("UseGlobalEndpoint")
                    .builtIn("AWS::S3::UseGlobalEndpoint")
                    .required(true)
                    .defaultValue(Value.bool(false))
                    .documentation("Whether the global endpoint should be used, rather then "
                            + "the regional endpoint for us-east-1.")
                    .build();

    /**
     * This MUST only be used by the S3 rules.
     */
    public static final Parameter S3_DISABLE_MRAP =
            Parameter.builder()
                    .type(ParameterType.BOOLEAN)
                    .name("DisableMultiRegionAccessPoints")
                    .builtIn("AWS::S3::DisableMultiRegionAccessPoints")
                    .required(true)
                    .defaultValue(Value.bool(false))
                    .documentation("Whether multi-region access points (MRAP) should be disabled.")
                    .build();

    /**
     * This MUST only be used by the STS rules.
     */
    public static final Parameter STS_USE_GLOBAL_ENDPOINT =
            Parameter.builder()
                    .type(ParameterType.BOOLEAN)
                    .name("UseGlobalEndpoint")
                    .builtIn("AWS::STS::UseGlobalEndpoint")
                    .required(true)
                    .defaultValue(Value.bool(false))
                    .documentation("Whether the global endpoint should be used, rather then "
                            + "the regional endpoint for us-east-1.")
                    .build();

    public static final List<Parameter> ALL_BUILTINS = ListUtils.of(
            SDK_ENDPOINT, REGION, FIPS, DUALSTACK, S3_ACCELERATE, S3_FORCE_PATH_STYLE, S3_USE_ARN_REGION,
            S3_USE_GLOBAL_ENDPOINT, S3_CONTROL_USE_ARN_REGION, STS_USE_GLOBAL_ENDPOINT, S3_DISABLE_MRAP);

    private Builtins() {
    }
}
