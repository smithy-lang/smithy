/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

description = "Smithy rules engine Language and traits"

ext {
    set("displayName", "Smithy :: Rules Engine")
    set("moduleName", "software.amazon.smithy.rulesengine")
}

dependencies {
    api(project(":smithy-model"))
    api(project(":smithy-utils"))
    api(project(":smithy-jmespath"))
}
