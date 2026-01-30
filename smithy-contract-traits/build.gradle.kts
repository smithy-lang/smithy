/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
plugins {
    id("smithy.module-conventions")
}

description = "This module provides Smithy traits for declaring contracts on models."

extra["displayName"] = "Smithy :: Contract Traits"
extra["moduleName"] = "software.amazon.smithy.contract.traits"

dependencies {
    api(project(":smithy-model-jmespath"))
}
