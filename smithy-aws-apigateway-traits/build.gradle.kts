/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
plugins {
    id("smithy.module-conventions")
}

description = "This module provides Smithy traits and validators for API Gateway."

extra["displayName"] = "Smithy :: AWS :: API Gateway Traits"
extra["moduleName"] = "software.amazon.smithy.aws.apigateway.traits"

dependencies {
    api(project(":smithy-aws-traits"))
}
