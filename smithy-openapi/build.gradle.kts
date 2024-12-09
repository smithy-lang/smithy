/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
plugins {
    id("smithy.module-conventions")
}

description = "This module contains support for converting a Smithy model to OpenAPI."

extra["displayName"] = "Smithy :: OpenAPI Conversion"
extra["moduleName"] = "software.amazon.smithy.openapi"

dependencies {
    api(project(":smithy-model"))
    api(project(":smithy-build"))
    api(project(":smithy-jsonschema"))
    api(project(":smithy-aws-traits"))
    api(project(":smithy-openapi-traits"))
}
