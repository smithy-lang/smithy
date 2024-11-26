/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
plugins {
    id("smithy.module-conventions")
}

description = "This module provides support for converting the Amazon API Gateway " +
    "Smithy traits when converting a Smithy model to OpenAPI3."

extra["displayName"] = "Smithy :: Amazon API Gateway OpenAPI Support"
extra["moduleName"] = "software.amazon.smithy.aws.apigateway.openapi"

dependencies {
    api(project(":smithy-aws-apigateway-traits"))
    api(project(":smithy-openapi"))
}
