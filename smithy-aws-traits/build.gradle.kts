/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
plugins {
    id("smithy.module-conventions")
}

description = "This module provides Smithy traits and validators that are used by most AWS services."

extra["displayName"] = "Smithy :: AWS Core Traits"
extra["moduleName"] = "software.amazon.smithy.aws.traits"

dependencies {
    api(project(":smithy-diff"))
    api(project(":smithy-model"))
}
