/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
plugins {
    id("smithy.module-conventions")
}

description = "Defines protocol test traits."

extra["displayName"] = "Smithy :: Protocol Test Traits"
extra["moduleName"] = "software.amazon.smithy.protocoltest.traits"

dependencies {
    api(project(":smithy-model"))
}
