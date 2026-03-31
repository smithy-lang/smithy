/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
plugins {
    alias(libs.plugins.smithy.jar)
    id("smithy.module-conventions")
}

description = "Tests for the smithy rules engine language and traits"

extra["displayName"] = "Smithy :: Rules Engine :: Tests"
extra["moduleName"] = "software.amazon.smithy.rulesenginetests"

dependencies {
    implementation(project(path = ":smithy-cli", configuration = "shadow"))
    api(project(":smithy-rules-engine"))
    api(project(":smithy-model"))
    api(project(":smithy-utils"))
}

tasks.sourcesJar {
    dependsOn("smithyJarStaging")
}

smithy {
    format.set(false)
    sourceProjection.set("transformed")
}
