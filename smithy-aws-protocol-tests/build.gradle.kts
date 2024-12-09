/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
plugins {
    alias(libs.plugins.smithy.jar)
    id("smithy.module-conventions")
}

description = "Defines protocol tests for AWS HTTP protocols."

extra["displayName"] = "Smithy :: AWS :: Protocol Tests"
extra["moduleName"] = "software.amazon.smithy.aws.protocoltests"

dependencies {
    implementation(project(path = ":smithy-cli", configuration = "shadow"))
    implementation(project(":smithy-protocol-test-traits"))
    implementation(project(":smithy-protocol-traits"))
    implementation(project(":smithy-aws-traits"))
    api(project(":smithy-validation-model"))
}

tasks.sourcesJar {
    dependsOn("smithyJarStaging")
}

smithy {
    format.set(false)
    smithyBuildConfigs.set(project.files())
}
