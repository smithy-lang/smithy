/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
plugins {
    id("smithy.module-conventions")
}

description = "Smithy contracts."

extra["displayName"] = "Smithy :: Contracts"
extra["moduleName"] = "software.amazon.smithy.contracts"

dependencies {
    api(project(":smithy-model-jmespath"))
}
