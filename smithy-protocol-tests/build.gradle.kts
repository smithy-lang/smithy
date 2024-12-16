/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
plugins {
    id("smithy.module-conventions")
    alias(libs.plugins.smithy.jar)
}

description = "Defines protocol tests for Smithy HTTP protocols."

extra["displayName"] = "Smithy :: Protocol Tests"
extra["moduleName"] = "software.amazon.smithy.protocoltests"

dependencies {
    implementation(project(path = ":smithy-cli", configuration = "shadow"))
    implementation(project(":smithy-protocol-test-traits"))
    implementation(project(":smithy-protocol-traits"))
    api(project(":smithy-validation-model"))
}

tasks {
    sourcesJar {
        dependsOn("smithyJarStaging")
    }
}

smithy {
    format.set(false)
    smithyBuildConfigs.set(project.files())
}
