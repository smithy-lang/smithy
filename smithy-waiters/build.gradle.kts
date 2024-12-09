/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
plugins {
    id("smithy.module-conventions")
}

description = "Defines Smithy waiters."

extra["displayName"] = "Smithy :: Waiters"
extra["moduleName"] = "software.amazon.smithy.waiters"

dependencies {
    api(project(":smithy-model"))
    api(project(":smithy-jmespath"))
}
