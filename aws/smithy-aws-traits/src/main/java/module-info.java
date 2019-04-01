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

import software.amazon.smithy.aws.traits.ArnReferenceTrait;
import software.amazon.smithy.aws.traits.ArnTemplateValidator;
import software.amazon.smithy.aws.traits.ArnTrait;
import software.amazon.smithy.aws.traits.AwsModelDiscovery;
import software.amazon.smithy.aws.traits.CognitoUserPoolsProviderArnsTrait;
import software.amazon.smithy.aws.traits.DataTrait;
import software.amazon.smithy.aws.traits.SdkServiceIdValidator;
import software.amazon.smithy.aws.traits.ServiceTrait;
import software.amazon.smithy.aws.traits.UnsignedPayload;
import software.amazon.smithy.aws.traits.apigateway.ApiKeySourceTrait;
import software.amazon.smithy.aws.traits.apigateway.AuthorizersTrait;
import software.amazon.smithy.aws.traits.apigateway.AuthorizersTraitValidator;
import software.amazon.smithy.aws.traits.apigateway.RequestValidatorTrait;
import software.amazon.smithy.aws.traits.iam.ActionPermissionDescriptionTrait;
import software.amazon.smithy.aws.traits.iam.ConditionKeysTrait;
import software.amazon.smithy.aws.traits.iam.ConditionKeysValidator;
import software.amazon.smithy.aws.traits.iam.DefineConditionKeysTrait;
import software.amazon.smithy.aws.traits.iam.InferConditionKeysTrait;
import software.amazon.smithy.aws.traits.iam.RequiredActionsTrait;
import software.amazon.smithy.model.loader.ModelDiscovery;
import software.amazon.smithy.model.traits.TraitService;
import software.amazon.smithy.model.validation.Validator;

module software.amazon.smithy.aws.traits {
    requires java.logging;
    requires software.amazon.smithy.model;

    exports software.amazon.smithy.aws.traits;
    exports software.amazon.smithy.aws.traits.iam;
    exports software.amazon.smithy.aws.traits.apigateway;

    uses ModelDiscovery;
    uses TraitService;
    uses Validator;

    // Allow the AWS trait model to be discovered.
    provides ModelDiscovery with AwsModelDiscovery;

    // Adds the typed trait classes.
    provides TraitService with
            // AWS traits.
            ServiceTrait,
            UnsignedPayload,

            // IAM traits.
            ActionPermissionDescriptionTrait,
            ArnReferenceTrait,
            ArnTrait,
            ConditionKeysTrait,
            CognitoUserPoolsProviderArnsTrait,
            DataTrait,
            DefineConditionKeysTrait,
            InferConditionKeysTrait,
            RequiredActionsTrait,

            // API Gateway traits.
            AuthorizersTrait,
            RequestValidatorTrait,
            ApiKeySourceTrait;

    // Add AWS trait validators.
    provides Validator with
            ArnTemplateValidator,
            ConditionKeysValidator,
            SdkServiceIdValidator,
            AuthorizersTraitValidator;
}

