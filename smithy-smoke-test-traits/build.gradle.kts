/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
plugins {
    id("smithy.module-conventions")
}

description = "Defines smoke test traits."

extra["displayName"] = "Smithy :: Smoke Test Traits"
extra["moduleName"] = "software.amazon.smithy.smoketest.traits"

dependencies {
    api(project(":smithy-model"))
}
