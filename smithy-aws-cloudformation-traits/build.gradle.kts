/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
plugins {
    id("smithy.module-conventions")
}

description = "This module provides Smithy traits and validators for CloudFormation."

extra["displayName"] = "Smithy :: AWS :: CloudFormation Traits"
extra["moduleName"] = "software.amazon.smithy.aws.cloudformation.traits"

dependencies {
    api(project(":smithy-model"))
}
