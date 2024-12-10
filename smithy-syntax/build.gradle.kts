/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
plugins {
    alias(libs.plugins.shadow)
    id("smithy.module-conventions")
}

description = "Provides a parse tree and formatter for Smithy models."

extra["displayName"] = "Smithy :: Syntax"
extra["moduleName"] = "software.amazon.smithy.syntax"

dependencies {
    api(project(":smithy-utils"))
    api(project(":smithy-model"))
    implementation(libs.prettier4j)

    // This is needed to export these as dependencies since we aren't shading them.
    shadow(project(":smithy-model"))
    shadow(project(":smithy-utils"))
}

tasks {
    shadowJar {
        // Replace the normal JAR with the shaded JAR. We don't want to publish a JAR that isn't shaded.
        archiveClassifier.set("")

        mergeServiceFiles()

        // Shade and relocate prettier4j.
        relocate("com.opencastsoftware.prettier4j", "software.amazon.smithy.syntax.shaded.prettier4j")

        // Despite the "shadow" configuration under dependencies, we unfortunately need to also list here that
        // smithy-model and smithy-utils aren't shaded. These are normal dependencies that we want consumers to resolve.
        dependencies {
            exclude(project(":smithy-utils"))
            exclude(project(":smithy-model"))
        }
    }

    jar {
        finalizedBy(shadowJar)
    }
}
