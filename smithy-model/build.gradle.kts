/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
plugins {
    id("smithy.module-conventions")
    id("smithy.profiling-conventions")
}

description = "This module provides the core implementation of loading, validating, " +
    "traversing, mutating, and serializing a Smithy model."

extra["displayName"] = "Smithy :: Model"
extra["moduleName"] = "software.amazon.smithy.model"

dependencies {
    api(project(":smithy-utils"))
    jmh(project(":smithy-utils"))
}
