/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
plugins {
    id("smithy.module-conventions")
}

description = "This module contains support for converting Smithy resources to CloudFormation Resource Schemas."

extra["displayName"] = "Smithy :: Cloudformation Conversion"
extra["moduleName"] = "software.amazon.smithy.cloudformation.converter"

// Necessary to load the everit JSON Schema validator.
repositories {
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    api(project(":smithy-build"))
    api(project(":smithy-jsonschema"))
    api(project(":smithy-aws-cloudformation-traits"))
    api(project(":smithy-aws-iam-traits"))
    api(project(":smithy-aws-traits"))

    // For use in validating schemas used in tests against the supplied
    // CloudFormation definition schema.
    testImplementation(libs.json.schema.validator)
}
