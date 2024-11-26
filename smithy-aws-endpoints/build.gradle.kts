/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
plugins {
    id("smithy.module-conventions")
}

description = "AWS specific components for managing endpoints in Smithy"

extra["displayName"] = "Smithy :: AWS Endpoints Components"
extra["moduleName"] = "software.amazon.smithy.aws.endpoints"

dependencies {
    api(project(":smithy-aws-traits"))
    api(project(":smithy-diff"))
    api(project(":smithy-rules-engine"))
    api(project(":smithy-model"))
    api(project(":smithy-utils"))
}
