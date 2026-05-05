/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
plugins {
    id("smithy.module-conventions")
}

description = "This module provides a smithy-build plugin that serializes a Smithy model " +
    "to JSON AST with CloudFormation Fn::Sub substitution for use as a CFN RestApi Body."

extra["displayName"] = "Smithy :: Amazon API Gateway CloudFormation JSON"
extra["moduleName"] = "software.amazon.smithy.aws.apigateway.cfn"

dependencies {
    api(project(":smithy-build"))
    api(project(":smithy-model"))
    testImplementation(project(":smithy-aws-apigateway-traits"))
}
