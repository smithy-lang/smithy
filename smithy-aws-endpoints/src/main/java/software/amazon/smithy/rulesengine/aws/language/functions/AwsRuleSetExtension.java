/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.rulesengine.aws.language.functions;

import java.util.List;
import software.amazon.smithy.rulesengine.language.EndpointRuleSetExtension;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.FunctionDefinition;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameter;
import software.amazon.smithy.rulesengine.validators.AuthSchemeValidator;
import software.amazon.smithy.utils.ListUtils;

/**
 * AWS-specific extensions to smithy-rules-engine.
 */
public final class AwsRuleSetExtension implements EndpointRuleSetExtension {
    @Override
    public List<Parameter> getBuiltIns() {
        return ListUtils.of(
                AwsBuiltIns.DUALSTACK,
                AwsBuiltIns.FIPS,
                AwsBuiltIns.REGION,
                AwsBuiltIns.ACCOUNT_ID,
                AwsBuiltIns.ACCOUNT_ID_ENDPOINT_MODE,
                AwsBuiltIns.CREDENTIAL_SCOPE,
                AwsBuiltIns.S3_ACCELERATE,
                AwsBuiltIns.S3_DISABLE_MRAP,
                AwsBuiltIns.S3_FORCE_PATH_STYLE,
                AwsBuiltIns.S3_USE_ARN_REGION,
                AwsBuiltIns.S3_USE_GLOBAL_ENDPOINT,
                AwsBuiltIns.S3_CONTROL_USE_ARN_REGION,
                AwsBuiltIns.STS_USE_GLOBAL_ENDPOINT);
    }

    @Override
    public List<FunctionDefinition> getLibraryFunctions() {
        return ListUtils.of(
                AwsPartition.getDefinition(),
                IsVirtualHostableS3Bucket.getDefinition(),
                ParseArn.getDefinition());
    }

    @Override
    public List<AuthSchemeValidator> getAuthSchemeValidators() {
        return ListUtils.of(
                new EndpointAuthUtils.SigV4SchemeValidator(),
                new EndpointAuthUtils.SigV4aSchemeValidator(),
                new EndpointAuthUtils.SigV4SubSchemeValidator(),
                new EndpointAuthUtils.BetaSchemeValidator());
    }
}
