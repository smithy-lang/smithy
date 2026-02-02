/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
plugins {
    id("smithy.module-conventions")
}

description = "This module is a library used to validate Smithy models, create filtered " +
    "projections of a model, and generate build artifacts."

extra["displayName"] = "Smithy :: Build"
extra["moduleName"] = "software.amazon.smithy.build"

dependencies {
    api(project(":smithy-utils"))
    api(project(":smithy-model"))

    // Allows testing of annotation processor
    testImplementation(libs.compile.testing)
}
