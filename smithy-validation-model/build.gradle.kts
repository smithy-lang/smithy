/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
plugins {
    id("smithy.module-conventions")
    alias(libs.plugins.smithy.jar)
}

description = "This module provides support for validation in Smithy server SDKs"

extra["displayName"] = "Smithy :: Validation Support"
extra["moduleName"] = "software.amazon.smithy.validation.model"

dependencies {
    implementation(project(path = ":smithy-cli", configuration = "shadow"))
}

tasks {
    sourcesJar {
        dependsOn("smithyJarStaging")
    }
}

smithy {
    smithyBuildConfigs.set(project.files())
}
