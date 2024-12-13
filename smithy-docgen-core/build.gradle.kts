/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
plugins {
    id("smithy.module-conventions")
}

description = "This module contains support for generating API documentation " +
    "based on Smithy models."

extra["displayName"] = "Smithy :: DocGen :: Core"
extra["moduleName"] = "software.amazon.smithy.docgen.core"

dependencies {
    implementation(project(":smithy-model"))
    implementation(project(":smithy-build"))
    implementation(project(":smithy-utils"))
    implementation(project(":smithy-codegen-core"))
    implementation(project(":smithy-linters"))
}

// jdk {
//    version = JavaVersion.VERSION_17
// }

jdk {
    version = JavaLanguageVersion.of(17)
}

java {
    toolchain {
//        languageVersion.set(JavaLanguageVersion.of(17))
//        sourceCompatibility = JavaVersion.VERSION_17
//        targetCompatibility = JavaVersion.VERSION_17
        println("currentToolchain: $toolchain")
        println("compats: $sourceCompatibility , $targetCompatibility")
    }
}

// afterEvaluate {
// java {
//    jdk(project, JavaVersion.VERSION_17)

//    toolchain {
//        languageVersion.set(JavaLanguageVersion.of(17))
//    }
// }
