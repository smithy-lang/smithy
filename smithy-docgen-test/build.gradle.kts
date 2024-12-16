/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
plugins {
    id("smithy.module-conventions")
    alias(libs.plugins.smithy.jar)
}

extra["displayName"] = "Smithy :: DocGen :: Core"
extra["moduleName"] = "software.amazon.smithy.docgen.core"

dependencies {
    implementation(project(":smithy-aws-traits"))
    implementation(project(":smithy-docgen-core"))
    implementation(project(path = ":smithy-cli", configuration = "shadow"))
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks {
    jar {
        enabled = false
    }
}
