/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
plugins {
    id("smithy.module-conventions")
}

description = "Smithy rules engine Language and traits"

extra["displayName"] = "Smithy :: Rules Engine"
extra["moduleName"] = "software.amazon.smithy.rulesengine"

dependencies {
    api(project(":smithy-model"))
    api(project(":smithy-utils"))
    api(project(":smithy-jmespath"))
}
