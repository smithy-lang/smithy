/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.rulesengine.aws.language.functions;

import software.amazon.smithy.rulesengine.language.evaluation.value.Value;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameter;
import software.amazon.smithy.rulesengine.language.syntax.parameters.ParameterType;

/**
 * AWS-specific built-in parameters for EndpointRules.
 */
public final class AwsBuiltIns {
    /**
     * Built-in parameter representing the DualStack parameter for SDKs.
     */
    public static final Parameter DUALSTACK =
            Parameter.builder()
                    .name("UseDualStack")
                    .type(ParameterType.BOOLEAN)
                    .builtIn("AWS::UseDualStack")
                    .documentation("When true, use the dual-stack endpoint. If the configured endpoint does not "
                            + "support dual-stack, dispatching the request MAY return an error.")
                    .required(true)
                    .defaultValue(Value.booleanValue(false))
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
                    .defaultValue(Value.booleanValue(false))
                    .build();

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
     * Built-in parameter representing the AccountId.
     */
    public static final Parameter ACCOUNT_ID =
            Parameter.builder()
                    .name("AccountId")
                    .type(ParameterType.STRING)
                    .builtIn("AWS::Auth::AccountId")
                    .documentation("The AWS AccountId used for the request.")
                    .build();

    /**
     * Built-in parameter representing the AccountId Endpoint Mode.
     */
    public static final Parameter ACCOUNT_ID_ENDPOINT_MODE =
            Parameter.builder()
                    .name("AccountIdEndpointMode")
                    .type(ParameterType.STRING)
                    .builtIn("AWS::Auth::AccountIdEndpointMode")
                    .documentation("The AccountId Endpoint Mode.")
                    .build();

    /**
     * Built-in parameter representing the Credential Scope.
     */
    public static final Parameter CREDENTIAL_SCOPE =
            Parameter.builder()
                    .name("CredentialScope")
                    .type(ParameterType.STRING)
                    .builtIn("AWS::Auth::CredentialScope")
                    .documentation("The AWS Credential Scope used for the request.")
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
                    .defaultValue(Value.booleanValue(false))
                    .documentation("When true, use S3 Accelerate. NOTE: Not all regions support S3 accelerate.")
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
                    .defaultValue(Value.booleanValue(false))
                    .documentation("Whether multi-region access points (MRAP) should be disabled.")
                    .build();

    /**
     * This MUST only be used by the S3 rules.
     */
    public static final Parameter S3_FORCE_PATH_STYLE =
            Parameter.builder()
                    .type(ParameterType.BOOLEAN)
                    .name("ForcePathStyle")
                    .builtIn("AWS::S3::ForcePathStyle")
                    .documentation("When true, force a path-style endpoint to be used where the bucket name is part "
                            + "of the path.")
                    .build();

    /**
     * This MUST only be used by the S3 rules.
     */
    public static final Parameter S3_USE_ARN_REGION =
            Parameter.builder()
                    .type(ParameterType.BOOLEAN)
                    .name("UseArnRegion")
                    .builtIn("AWS::S3::UseArnRegion")
                    .documentation("When an Access Point ARN is provided and this flag is enabled, the SDK MUST use "
                            + "the ARN's region when constructing the endpoint instead of the client's "
                            + "configured region.")
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
                    .defaultValue(Value.booleanValue(false))
                    .documentation("Whether the global endpoint should be used, rather then "
                            + "the regional endpoint for us-east-1.")
                    .build();

    /**
     * This MUST only be used by the S3Control rules.
     */
    public static final Parameter S3_CONTROL_USE_ARN_REGION =
            Parameter.builder()
                    .type(ParameterType.BOOLEAN)
                    .name("UseArnRegion")
                    .builtIn("AWS::S3Control::UseArnRegion")
                    .documentation("When an Access Point ARN is provided and this flag is enabled, the SDK MUST use "
                            + "the ARN's region when constructing the endpoint instead of the client's "
                            + "configured region.")
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
                    .defaultValue(Value.booleanValue(false))
                    .documentation("Whether the global endpoint should be used, rather then "
                            + "the regional endpoint for us-east-1.")
                    .build();

    private AwsBuiltIns() {}
}
