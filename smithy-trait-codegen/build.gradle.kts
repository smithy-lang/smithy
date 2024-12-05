/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
plugins {
    id("smithy.module-conventions")
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
        val main by getting
        val test by getting

        compileClasspath += main.output +
            configurations["testRuntimeClasspath"] +
            configurations["testCompileClasspath"]

        runtimeClasspath += output +
            compileClasspath +
            test.runtimeClasspath +
            test.output

        java {
            srcDir("${layout.buildDirectory.get()}/integ/")
        }

        resources {
            srcDirs(layout.buildDirectory.dir("generated-resources").get())
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
val generatedMetaInf = File("$buildDir/integ/META-INF")
val destResourceDir = File("$buildDir/generated-resources/META-INF")
tasks.register<Copy>("copyGeneratedSrcs") {
    from(generatedMetaInf)
    into(destResourceDir)
    dependsOn("generateTraits")
}


tasks {
    named("checkstyleIt") {
        enabled = false
    }

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
