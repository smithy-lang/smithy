/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
plugins {
    alias(libs.plugins.smithy.jar)
    id("smithy.module-conventions")
}

description = "Defines models used for benchmarks."

extra["displayName"] = "Smithy :: Benchmark :: Models"
extra["moduleName"] = "software.amazon.smithy.benchmark.models"

dependencies {
    implementation(project(path = ":smithy-cli", configuration = "shadow"))
    api(project(":smithy-aws-traits"))
    api(project(":smithy-protocol-test-traits"))
    api(project(":smithy-protocol-traits"))
    api(project(":smithy-rules-engine"))
    api(project(":smithy-aws-endpoints"))
}

tasks.sourcesJar {
    dependsOn("smithyJarStaging")
}

smithy {
    smithyBuildConfigs.set(project.files())
}
