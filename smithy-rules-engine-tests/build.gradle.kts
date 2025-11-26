/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
plugins {
    id("smithy.module-conventions")
}

description = "Tests for the smithy rules engine language and traits"

extra["displayName"] = "Smithy :: Rules Engine :: Tests"
extra["moduleName"] = "software.amazon.smithy.rulesengine"

dependencies {
    api(project(":smithy-rules-engine"))
    api(project(":smithy-model"))
    api(project(":smithy-utils"))
}
