/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
plugins {
    id("smithy.module-conventions")
    id("smithy.integ-test-conventions")
}

description = "Plugin for Generating Trait Code from Smithy Models"

extra["displayName"] = "Smithy :: Trait Code Generation"
extra["moduleName"] = "software.amazon.smithy.traitcodegen"

dependencies {
    implementation(project(":smithy-codegen-core"))
}

// Set up Integration testing source sets
sourceSets {
    named("it") {
        // Set up the generated integ source
        java {
            srcDir(layout.buildDirectory.dir("integ"))
        }

        // Set up the generated integ resources
        resources {
            srcDir(layout.buildDirectory.dir("generated-resources"))
        }

        spotless {
            java {
                targetExclude("build/**/*.*")
            }
        }
    }
}

// Execute building of trait classes using an executable class
// These traits will then be passed in to the integration test (it)
// source set
tasks.register<JavaExec>("generateTraits") {
    classpath = sourceSets.test.get().runtimeClasspath + sourceSets.test.get().output
    mainClass.set("software.amazon.smithy.traitcodegen.PluginExecutor")
}

// Copy generated META-INF files to a new generated-resources directory to
// make it easy to include as resource srcDir
tasks.register<Copy>("copyGeneratedSrcs") {
    from(layout.buildDirectory.dir("integ/META-INF"))
    into(layout.buildDirectory.dir("generated-resources/META-INF"))
    dependsOn("generateTraits")
}

tasks {
    named("compileItJava") {
        dependsOn("generateTraits", "copyGeneratedSrcs")
    }

    named("processItResources") {
        dependsOn("copyGeneratedSrcs")
    }

    named("integ") {
        mustRunAfter("generateTraits", "copyGeneratedSrcs")
    }

    named("test") {
        finalizedBy("integ")
    }

    named("spotbugsIt") {
        enabled = false
    }
}
